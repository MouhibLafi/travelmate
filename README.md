# TravelMate

Mini-projet *IA for Software Engineering* · Groupe 6

Branche `module-1-users` : **le socle du projet** et le module 1 (gestion des utilisateurs), terminé.
**Membres du groupe : lisez [INTEGRATION.md](INTEGRATION.md) pour ajouter votre module.**

- **Backend** : Spring Boot 3.5 (Java 21), Spring Security + JWT, JPA + MySQL (XAMPP)
- **Frontend** : Angular 20 + Angular Material, thème de la charte TravelMate
- **IA** : llama3 en local avec Ollama

---

## 1. Lancer le projet

### a) L'IA (Ollama)
1. Installer Ollama : https://ollama.com/download
2. Télécharger le modèle (une seule fois, environ 4,7 Go) :
   ```
   ollama pull llama3
   ```
3. Ollama tourne ensuite tout seul sur `http://localhost:11434`.
   Pour tester : `ollama run llama3 "bonjour"`

### b) La base de données (MySQL avec XAMPP)
Ouvrir le panneau XAMPP et cliquer **Start** sur la ligne **MySQL**.
La base `travelmate` et ses tables sont créées automatiquement au premier lancement du backend.

Pas de XAMPP ? Lancez le backend avec la base H2 en mémoire (données perdues à chaque redémarrage) :
```
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

### c) Le backend
```
cd backend
mvn spring-boot:run
```
L'API démarre sur `http://localhost:8080`.

### Base de démonstration

Au démarrage, le backend crée une base de test complète (seulement ce qui manque) :
**20 utilisateurs**, leurs préférences et **22 voyages** (dates calculées à partir d'aujourd'hui).

| Email | Mot de passe | Rôle | Pour tester |
|---|---|---|---|
| admin@travelmate.tn | admin123 | ADMIN | Administration |
| mouhib@travelmate.tn | password123 | ADMIN | 2e administrateur |
| yasmine@travelmate.tn | password123 | VOYAGEUR | Profil complet, 1 voyage à venir + 1 terminé |
| karim@travelmate.tn | password123 | VOYAGEUR | Montagne, 2 voyages |
| oussama@travelmate.tn | password123 | VOYAGEUR | Nouvel inscrit : préférences vides, aucun voyage |
| bilel@travelmate.tn | password123 | VOYAGEUR | **Compte désactivé** : la connexion est refusée |

Les 14 autres voyageurs (amira, mehdi, ines, youssef, nour, hamza, rania, mariem, aymen, sarra,
fares, lina, walid, emna `@travelmate.tn`) ont tous le mot de passe `password123`.
Données : `backend/.../users/config/DataSeeder.java` (module 1) et `trips/config/TripDataSeeder.java` (module 2).

La même base existe en SQL dans `docs/travelmate-demo.sql`, à importer dans phpMyAdmin (onglet **Importer**).
Ses dates sont figées au jour de l'export ; pour des dates à jour, laissez plutôt le backend créer la base.

### d) Le frontend
```
cd frontend
npm install
ng serve
```
Ouvrir `http://localhost:4200`.

---

## 2. Organisation du code

### Backend (`backend/src/main/java/com/travelmate`)

Communs à tous les modules :

| Dossier | Rôle |
|---|---|
| `security` | JWT : `JwtService` (crée/lit le jeton), `JwtFilter` (lit le jeton à chaque requête), `SecurityConfig` (qui a accès à quoi) |
| `exception` | Une seule exception `ApiException` + un gestionnaire qui renvoie `{ "message": "..." }` |
| `ai` | `OllamaClient` : l'appel à llama3, partagé par toutes les tâches IA |

Module 1 (`users/`) :

| Dossier | Rôle |
|---|---|
| `users/entity` | Les tables : `User`, `TravelPreferences` et les enums `Role`, `Interest`, `BudgetLevel` |
| `users/repository` | Accès à la base (Spring Data JPA) |
| `users/dto` | Les objets échangés avec le front (on ne renvoie jamais le mot de passe) |
| `users/service` | La logique métier : `AuthService`, `UserService`, `AiService` |
| `users/controller` | Les routes de l'API REST |
| `users/event` | `UserDeletedEvent` : prévient les autres modules qu'un utilisateur est supprimé |
| `users/config` | `DataSeeder` : comptes de test |

### Frontend (`frontend/src/app`)
| Dossier | Rôle |
|---|---|
| `pages/login`, `pages/register` | Connexion et inscription |
| `pages/preferences` | Préférences de voyage + **bloc IA** |
| `pages/profile` | Modifier ses infos, son mot de passe, supprimer son compte |
| `pages/admin` | Liste des utilisateurs, changer le rôle, activer/désactiver, supprimer |
| `services` | Appels HTTP vers le backend |
| `interceptors/auth.interceptor.ts` | Ajoute le jeton JWT à chaque requête |
| `guards/auth.guard.ts` | Bloque les pages si on n'est pas connecté / pas admin |
| `components` | Barre de navigation et icônes Lucide |

---

## 3. Les routes de l'API

| Méthode | URL | Accès | Description |
|---|---|---|---|
| POST | `/api/auth/register` | public | Inscription (rôle VOYAGEUR) |
| POST | `/api/auth/login` | public | Connexion, renvoie le jeton JWT |
| GET / PUT / DELETE | `/api/users/me` | connecté | Voir, modifier, supprimer son profil |
| PUT | `/api/users/me/password` | connecté | Changer de mot de passe |
| GET / PUT | `/api/users/me/preferences` | connecté | Voir / enregistrer ses préférences |
| POST | `/api/ai/analyze-profile` | connecté | **Tâche IA du module 1** |
| GET | `/api/admin/users?search=` | ADMIN | Liste et recherche |
| PUT | `/api/admin/users/{id}/role` | ADMIN | `{ "role": "ADMIN" }` |
| PUT | `/api/admin/users/{id}/status` | ADMIN | `{ "active": false }` |
| DELETE | `/api/admin/users/{id}` | ADMIN | Supprimer un utilisateur |

Les requêtes protégées doivent contenir l'en-tête `Authorization: Bearer <jeton>`.

---

## 4. La tâche IA expliquée

Une tâche IA suit 3 étapes. L'étape 2 est commune à tous les modules : `ai/OllamaClient.java`
appelle `POST http://localhost:11434/api/generate` avec `"format": "json"` (le modèle doit
répondre en JSON) et `"temperature": 0` (réponses stables).

### Module 1 : le profil en une phrase

> L'utilisateur se décrit en une phrase et l'IA en déduit ses centres d'intérêt et son niveau de budget.

Tout se passe dans `backend/.../users/service/AiService.java` :

1. **Le prompt** : on donne au modèle son rôle, la liste fermée des valeurs autorisées
   (les 10 centres d'intérêt et les 3 budgets), les règles, un exemple et le format JSON attendu, puis la phrase.
2. **L'appel à Ollama** via `OllamaClient`.
3. **La vérification** : un modèle peut se tromper ou inventer une valeur. On garde seulement
   les valeurs qui existent dans nos enums (4 intérêts maximum) ; si le budget est invalide,
   on met `MOYEN` avec une confiance basse.

Exemple :
```
POST /api/ai/analyze-profile
{ "description": "J'adore la plage et la bonne cuisine, avec un petit budget" }

→ { "interests": ["PLAGE", "GASTRONOMIE"], "budgetLevel": "ECONOMIQUE",
    "confidence": 0.9, "explanation": "La plage et la cuisine sont citées ; petit budget." }
```

**Rien n'est enregistré par l'IA** : le front affiche la suggestion, l'utilisateur clique
« Appliquer », peut encore modifier, puis enregistre. C'est la règle de la charte
« Review every suggestion before adding it ».

### Les tests

L'étape 3 est testée **sans lancer Ollama** dans `AiServiceTest.java` :
```
cd backend
mvn test
```

---

## 5. Charte graphique appliquée

- Teal `#0E7C86` : boutons principaux, liens, éléments actifs
- Navy `#12263A` : titres, panneau de connexion, avatar
- Coral `#FF6B4A` : bouton de conversion « Créer mon compte » (texte navy)
- Violet `#7C5CFF` et dégradé violet → teal : **uniquement** le bloc IA
- Poppins (titres) / Inter (texte), grille de 8 px, rayons 8 px (boutons, champs) et 16 px (cartes)
- Icônes Lucide, trait 1,5 px

---

## 6. Pour le groupe

Voir **[INTEGRATION.md](INTEGRATION.md)** : comment ajouter votre module, les règles communes et Git.
