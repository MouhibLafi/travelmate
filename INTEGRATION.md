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
├── restaurants/   Module 4  Restaurants          (intégré)
├── accommodation/ Module 5  Hébergements        (intégré)
└── itinerary/     Module 6  Itinéraire et budget

frontend/src/app/
├── (dossiers du module 1 : pages, services, guards, interceptors, components)
├── trips/         Module 2
├── activities/    Module 3
├── restaurants/   Module 4
├── features/accommodation/  Module 5
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

Pour lancer l'application, utilisez toujours la branche **`main`** : c'est la seule qui contient tout.

---

## 6. Git : une branche par module, `main` pour l'intégration

Dépôt : https://github.com/MouhibLafi/travelmate

| Branche | Contenu |
|---|---|
| `main` | **Intégration** : le socle + tous les modules + leur branchement (routes, menu). C'est la version qu'on lance et qu'on présente. |
| `module-1-users` | Module 1 seul, qui est aussi le socle (security, exception, ai, styles) |
| `module-2-trips` | **Uniquement** le code du module 2 (`trips/`) + `docs/module-2-trips.md` |
| `module-3-activities` | **Uniquement** le code du module 3 (à créer) |
| `module-4-restaurant` | **Uniquement** le code du module 4 (Oumayma) — intégré dans `main` |
| `module-hebergements` | **Uniquement** le code du module 5 (Anas) — intégré dans `main` |
| `module-6-itinerary` | **Uniquement** le code du module 6 (à créer) |

Une branche de module ne contient **que les dossiers de ce module** : on voit clairement le travail
de chacun. Elle ne se lance pas seule (elle a besoin du module 1) ; c'est `main` qui assemble tout.
Exemple à suivre : la branche `module-2-trips`.

### a) Développer son module (sur sa machine)

On développe sur une branche locale qui part de `main`, pour pouvoir lancer et tester l'application :

```
git clone https://github.com/MouhibLafi/travelmate.git
cd travelmate
git checkout -b dev-module-3 main
# ... travail dans backend/.../activities/ et frontend/src/app/activities/
#     (ajoutez aussi votre route et votre lien de menu pour tester)
git add .
git commit -m "Module 3 : catalogue des activités"
```

### b) Publier son module seul sur sa branche

**La première fois** : on crée la branche `module-3-activities` avec uniquement ses dossiers.

```
git checkout --orphan module-3-activities
git rm -r -q --cached .
git add backend/src/main/java/com/travelmate/activities backend/src/test/java/com/travelmate/activities frontend/src/app/activities docs/module-3-activities.md
git commit -m "Module 3 : catalogue des activités"
git push -u origin module-3-activities
git checkout -f dev-module-3
```

**Les fois suivantes** (après avoir commité dans `dev-module-3`) :

```
git checkout module-3-activities
git checkout dev-module-3 -- backend/src/main/java/com/travelmate/activities backend/src/test/java/com/travelmate/activities frontend/src/app/activities docs/module-3-activities.md
git commit -m "Module 3 : recherche et filtres"
git push
git checkout dev-module-3
```

Dans `docs/module-3-activities.md`, décrivez votre module et **les lignes à ajouter dans `main`**
pour le brancher : votre route (`app.routes.ts`), votre lien de menu (`navbar.component.html`),
et tout changement demandé au socle (ex. une nouvelle valeur d'`Interest`).
Modèle : `docs/module-2-trips.md`.

### c) Intégrer un module dans `main` (responsable de l'intégration)

```
git checkout main
git pull
git fetch origin
git merge --allow-unrelated-histories --no-commit origin/module-3-activities   # la 1re fois
# git merge --no-commit origin/module-3-activities                             # les fois suivantes
# ajouter la route et le lien de menu indiqués dans docs/module-3-activities.md, puis :
#   mvn test (backend)  ·  ng build (frontend)  ·  tester le module et la suppression d'un utilisateur
git add .
git commit -m "Intégration du module 3"
git push
```

### d) Récupérer le travail des autres

Sur votre branche de développement, à faire souvent :

```
git checkout dev-module-3
git fetch origin
git merge origin/main
```

**On ne pousse jamais directement sur `main`** sans avoir testé l'ensemble.
En cas de conflit dans `app.routes.ts` ou `navbar.component.html`, gardez les lignes des **deux** modules.

---

## 7. Checklist avant de fusionner son module

- [ ] Mon code est dans `com.travelmate.<module>` et `src/app/<module>`
- [ ] Mes routes commencent par `/api/<module>`
- [ ] J'utilise `User`, `Interest` et `BudgetLevel` du module 1 (pas de copie)
- [ ] Si mes données sont liées à un utilisateur : `@EventListener onUserDeleted` ajouté
- [ ] Ma page est protégée par `authGuard` ; ma route et mon lien de menu sont indiqués dans `docs/module-X-….md`
- [ ] Ma branche `module-X-…` ne contient que mes dossiers et ma fiche `docs/`
- [ ] Couleurs et composants de la charte, violet seulement pour l'IA
- [ ] `mvn test` et `ng build` passent
