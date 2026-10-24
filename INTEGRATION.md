# Intégrer son module dans TravelMate

Guide pour les membres du groupe 6. Le module 1 (utilisateurs) est le **socle** : il gère la connexion,
les rôles et les préférences. Chaque module s'ajoute **dans ce même projet**, dans son propre dossier.

Les modules 1 (`users/`) et 2 (`trips/`) sont **terminés** et fusionnés dans `main`.
Le module 2 est le meilleur exemple à copier, car il utilise le module 1 et contient une tâche IA complète.

---

## 1. Une seule application, un dossier par module

```
backend/src/main/java/com/travelmate/
├── security/      commun (JWT)        ← ne pas modifier sans prévenir le groupe
├── exception/     commun (ApiException)
├── ai/            commun (OllamaClient : appel a llama3)
├── users/         Module 1  Gestion des utilisateurs     (termine)
├── trips/         Module 2  Planification des voyages    (termine)
├── activities/    Module 3  Destinations et activités
├── restaurants/   Module 4  Restaurants
├── stays/         Module 5  Hébergements
└── itinerary/     Module 6  Itinéraire et budget

frontend/src/app/
├── (dossiers du module 1 : pages, services, guards, interceptors, components)
├── trips/         Module 2
├── activities/    Module 3
├── restaurants/   Module 4
├── stays/         Module 5
└── itinerary/     Module 6
```

Un seul backend (port **8080**), un seul frontend (port **4200**), une seule base MySQL **`travelmate`**.

---

## 2. Ce que le module 1 vous donne (ne le recodez pas)

| Besoin | Ce qu'il faut utiliser |
|---|---|
| Savoir qui est connecté | `Principal principal` dans le contrôleur → `principal.getName()` = email |
| Récupérer l'utilisateur | `userService.findByEmail(email)` → entité `User` |
| Lier vos données à un utilisateur | `@ManyToOne private User user;` dans votre entité |
| Centres d'intérêt | enum `Interest` (Java) et `INTERESTS` (Angular, `models/user.model.ts`) |
| Niveau de budget | enum `BudgetLevel` (Java) et `BUDGETS` (Angular) |
| Préférences de l'utilisateur | `GET /api/users/me/preferences` ou `userService.getPreferences(email)` |
| Réservé à l'admin | mettre votre route sous `/api/admin/...` : seul le rôle ADMIN y a accès |
| Erreurs | `throw new ApiException(HttpStatus.NOT_FOUND, "Message en français");` |
| Appeler l'IA (Ollama, llama3) | injecter `OllamaClient` et appeler `ollamaClient.generateJson(prompt)` ; modèle complet : `trips/service/TripAiService.java` (prompt → appel → vérification) |
| Afficher une suggestion de l'IA | classes communes `.ai-card`, `.suggestion`, `.meter`, `.btn-ai` de `styles.scss` (voir `trips/trips.component.html`) |

**Sécurité : rien à faire.** Toutes les routes `/api/**` exigent déjà un jeton JWT (sauf `/api/auth/**`).
**Frontend : rien à faire pour le jeton.** L'intercepteur l'ajoute à toutes les requêtes.

### Qui utilise quoi

| Module | Utilise du module 1 |
|---|---|
| 2 Voyages | `User` (propriétaire du voyage), `Interest`, `BudgetLevel`, préférences pour préremplir |
| 3 Activités | `Interest` comme **catégorie** des activités, préférences pour la recommandation, `User` pour les favoris |
| 4 Restaurants | `User` (auteur d'un avis), `BudgetLevel`, préférences (goûts) |
| 5 Hébergements | `BudgetLevel`, rôle ADMIN pour gérer le catalogue |
| 6 Itinéraire | `User`, préférences, voyages du module 2, `OllamaClient` pour l'IA générative |

---

## 3. Ajouter son module, étape par étape

### Backend
1. Créez votre package, ex. `com.travelmate.activities`, avec les sous-dossiers
   `entity`, `repository`, `dto`, `service`, `controller` (comme `trips/`).
2. Préfixez vos routes : `/api/activities/...` (et `/api/admin/activities/...` pour l'admin).
3. Si votre entité est liée à un utilisateur, **supprimez ses données quand il est supprimé**
   (sinon la suppression d'un compte plante). Ajoutez dans votre service :
   ```java
   @EventListener
   public void onUserDeleted(UserDeletedEvent event) {
       favoriteRepository.deleteAll(favoriteRepository.findByUserId(event.userId()));
   }
   ```
   Exemple complet : `trips/service/TripService.java`.

### Frontend
1. Créez votre dossier, ex. `src/app/activities/`, avec `*.model.ts`, `*.service.ts`, `*.component.ts/html/css`.
2. Utilisez `API_URL` et `getErrorMessage` de `services/api.ts`.
3. Ajoutez votre route dans `app.routes.ts`, protégée par `authGuard` :
   ```ts
   { path: 'activites', component: ActivitiesComponent, canActivate: [authGuard] },
   ```
4. Ajoutez un lien dans `components/navbar/navbar.component.html`.
5. Respectez la charte : utilisez les classes et variables de `styles.scss`
   (`.card`, `.page`, `.chip`, `.alert-*`, `var(--tm-teal)`…). **Pas de nouvelles couleurs en dur.**
   Le violet `--tm-violet` est réservé aux fonctions IA.

---

## 4. Règles communes

1. **Un seul vocabulaire.** Les catégories d'activités, les goûts et les intérêts d'un voyage utilisent
   l'enum `Interest`. Besoin d'une nouvelle valeur ? Demandez-la au module 1, ne créez pas votre propre liste.
2. **On ne modifie pas le code d'un autre module** sans le prévenir (surtout `security/` et `users/`).
3. **Noms de tables au pluriel** et préfixés si besoin (`activities`, `restaurant_reviews`…).
4. **Messages d'erreur en français**, via `ApiException`.
5. **Commentaires en français**, code simple : organisation `entity / repository / dto / service / controller`.

---

## 5. Lancer le projet chez soi

1. XAMPP → **Start** sur MySQL (la base `travelmate` se crée toute seule).
   Sans XAMPP : `mvn spring-boot:run -Dspring-boot.run.profiles=h2`.
2. `ollama serve` (et une seule fois : `ollama pull llama3`).
3. `cd backend` puis `mvn spring-boot:run`.
4. `cd frontend` puis `npm install` (une seule fois) et `ng serve`.
5. http://localhost:4200, connexion `admin@travelmate.tn` / `admin123`.

Le module 2 (`trips/`) n'existe que sur les branches `module-2-trips` et `main`.

---

## 6. Git : une branche par module, `main` pour l'intégration

Dépôt : https://github.com/MouhibLafi/travelmate

| Branche | Contenu |
|---|---|
| `main` | **Intégration** : le socle + tous les modules fusionnés. C'est la version qu'on présente. |
| `module-1-users` | Module 1 seul (le socle) |
| `module-2-trips` | Module 2 (part du module 1) |
| `module-3-activities` | Module 3 (à créer) |
| `module-4-restaurants` | Module 4 (à créer) |
| `module-5-stays` | Module 5 (à créer) |
| `module-6-itinerary` | Module 6 (à créer) |

**On ne travaille jamais directement sur `main`.** Chacun travaille sur la branche de son module :

```
git clone https://github.com/MouhibLafi/travelmate.git
cd travelmate
git checkout -b module-3-activities main      # votre branche part de main (le module 1 y est deja)
# ... travail, puis :
git add .
git commit -m "Module 3 : catalogue des activités"
git push -u origin module-3-activities
```

**Intégrer son module** : sur GitHub, ouvrez une **Pull Request** de votre branche vers `main`.
Avant de la fusionner :
- `mvn test` passe dans `backend`
- `ng build` passe dans `frontend`
- vous avez testé votre module **et** la connexion / suppression d'un utilisateur

**Récupérer le travail des autres** (à faire souvent, pour éviter les conflits) :
```
git checkout main
git pull
git checkout module-3-activities
git merge main
```

Fichiers partagés où les conflits sont fréquents : `app.routes.ts`, `navbar.component.html`, `README.md`.
En cas de conflit, gardez les lignes des **deux** modules.

---

## 7. Checklist avant de fusionner son module

- [ ] Mon code est dans `com.travelmate.<module>` et `src/app/<module>`
- [ ] Mes routes commencent par `/api/<module>`
- [ ] J'utilise `User`, `Interest` et `BudgetLevel` du module 1 (pas de copie)
- [ ] Si mes données sont liées à un utilisateur : `@EventListener onUserDeleted` ajouté
- [ ] Ma page est protégée par `authGuard` et a un lien dans la navbar
- [ ] Couleurs et composants de la charte, violet seulement pour l'IA
- [ ] `mvn test` et `ng build` passent
