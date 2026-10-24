# TravelMate

Mini-projet *IA for Software Engineering* · Groupe 6

Ce dépôt contient **le socle du projet** et deux modules terminés :
le module 1 (gestion des utilisateurs) et le module 2 (planification des voyages).
**Membres du groupe : lisez [INTEGRATION.md](INTEGRATION.md) pour ajouter votre module.**

Branches : `main` = intégration de tous les modules (**c'est elle qu'on lance**) ·
`module-1-users` = module 1 seul (socle) · `module-2-trips` = module 2 seul · une branche par module.

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

Comptes créés automatiquement au démarrage :

| Email | Mot de passe | Rôle |
|---|---|---|
| admin@travelmate.tn | admin123 | ADMIN |
| yasmine@example.com | password123 | VOYAGEUR |
| karim@example.com | password123 | VOYAGEUR |

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

Module 2 (`trips/`) : même organisation `entity / repository / dto / service / controller`.
La logique est dans `TripService` (voyages) et `TripAiService` (tâche IA).

### Frontend (`frontend/src/app`)
| Dossier | Rôle |
|---|---|
| `trips` | Module 2 : page « Mes voyages » (bloc IA, créer / modifier, liste, supprimer) |
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
| GET / POST | `/api/trips` | connecté | Module 2 : mes voyages / créer un voyage |
| PUT / DELETE | `/api/trips/{id}` | connecté | Module 2 : modifier / supprimer un de mes voyages |
| POST | `/api/trips/ai/fill-form` | connecté | **Tâche IA du module 2** |

Les requêtes protégées doivent contenir l'en-tête `Authorization: Bearer <jeton>`.

---

## 4. Les tâches IA expliquées

Les deux tâches suivent les mêmes 3 étapes. L'étape 2 est commune : `ai/OllamaClient.java`
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

### Module 2 : le voyage en texte libre

> L'utilisateur écrit sa demande en texte libre (« 5 jours à Rome, 800 € ») et l'IA remplit le formulaire.

Tout se passe dans `backend/.../trips/service/TripAiService.java`. Idée clé : **le modèle extrait,
Java calcule**. Un modèle de langage se trompe facilement en calcul de dates ou en divisions ;
on lui demande seulement de lire la phrase, et le code fait le reste.

1. **Le prompt** : on donne la date du jour et on demande la destination, la date de début,
   la durée, le budget, les centres d'intérêt (liste fermée), sans rien inventer.
2. **L'appel à Ollama** via `OllamaClient`.
3. **La vérification et les calculs** :
   - destination écrite proprement (« ROME » → « Rome ») ;
   - date de début absente ou passée → dans 7 jours ;
   - date de fin = début + durée (« 5 jours » du 31/10 au 04/11) ; durée absente → 7 jours, confiance baissée ;
   - niveau de budget calculé à partir du **budget par jour** : moins de 80 € → ECONOMIQUE,
     jusqu'à 200 € → MOYEN, au-delà → LUXE. Sans montant, on garde le niveau déduit des mots (« pas cher »).

Exemple :
```
POST /api/trips/ai/fill-form
{ "text": "5 jours à Rome, 800 €" }

→ { "destination": "Rome", "startDate": "2026-10-31", "endDate": "2026-11-04",
    "budget": 800, "budgetLevel": "MOYEN", "interests": [], "confidence": 0.75,
    "explanation": "5 jours à Rome ; budget de 800 €." }
```

Là aussi, rien n'est enregistré : l'utilisateur clique « Remplir le formulaire », vérifie, puis crée le voyage.
Si l'IA ne trouve aucun centre d'intérêt, le formulaire garde ceux des préférences du module 1.

### Les tests

Les étapes 3 des deux tâches sont testées **sans lancer Ollama** (8 tests) :
`AiServiceTest.java` (module 1) et `TripAiServiceTest.java` (module 2).
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
