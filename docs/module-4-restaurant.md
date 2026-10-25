# Module 4 : Restaurants

Cette branche contient **uniquement le code du module 4**. Il utilise le module 1
(`User`, connexion JWT, préférences `Interest` / `BudgetLevel`) et le socle commun
(`ApiException`, `OllamaClient`, `authGuard`, `API_URL`) : pour lancer l'application,
utilisez la branche **`main`**, qui contient tous les modules intégrés.

## Fonctionnalités

- Catalogue des restaurants : recherche texte, filtre par cuisine, ville, budget maximum,
  note minimale et options alimentaires (halal, végétarien, vegan, sans gluten)
- Tri par note, prix croissant/décroissant ou nom
- Fiche restaurant : description, adresse, budget, options, note moyenne
- Avis : publication (note 1 à 5 + commentaire) **authentifiée par le JWT** ; l'auteur
  est l'utilisateur connecté, il n'est jamais saisi dans le formulaire
- **Tâche IA 1 — analyse de sentiment** : chaque avis est classé POSITIF / NÉGATIF / NEUTRE
  par llama3 via `OllamaClient`, avec un moteur local à base de lexique français en secours
- **Tâche IA 2 — recommandation** : classement des restaurants sur 100 selon la note
  (45 %), la satisfaction des avis (35 %) et le respect du budget (20 %) ; le budget
  initial vient des préférences du profil
- Favoris : ajout / retrait et liste des restaurants favoris de l'utilisateur connecté
- Suppression automatique des avis et favoris quand un utilisateur supprime son compte

## Fichiers

| Dossier | Contenu |
|---|---|
| `backend/src/main/java/com/travelmate/restaurants/entity` | `Restaurant`, `Avis`, `Favori`, `Sentiment` |
| `backend/src/main/java/com/travelmate/restaurants/repository` | `RestaurantRepository`, `AvisRepository`, `FavoriRepository`, `RestaurantSpecifications` |
| `backend/src/main/java/com/travelmate/restaurants/dto` | `RestaurantDTO`, `AvisDTO`, `AvisRequest`, `AnalyseAvisDTO`, `MetaFiltresDTO`, `RecommandationDTO`, `ReanalyseDTO`, `StatutIaDTO` |
| `backend/src/main/java/com/travelmate/restaurants/mapper` | `RestaurantMapper` |
| `backend/src/main/java/com/travelmate/restaurants/service` | `RestaurantService`, `AvisService`, `FavoriService`, `RecommandationService`, `SentimentService`, `OllamaStatutService` |
| `backend/src/main/java/com/travelmate/restaurants/controller` | `RestaurantController`, `FavoriController`, `IaController` |
| `backend/src/main/java/com/travelmate/restaurants/config` | `RestaurantDataSeeder` (12 restaurants, 48 avis de démonstration, 3 favoris) |
| `backend/src/test/java/com/travelmate/restaurants/service` | `SentimentServiceTest` (6 tests, sans Ollama) |
| `frontend/src/app/restaurants` | Catalogue, fiche + avis, recommandations IA, favoris, carte restaurant, modèles, services, routes |

Les tables `restaurants`, `restaurant_options`, `restaurant_reviews` et
`restaurant_favorites` sont créées automatiquement par Hibernate (`ddl-auto=update`).

## Routes de l'API

Toutes les routes sont préfixées par `/api/restaurants`.

| Méthode | URL | Description |
|---|---|---|
| GET | `/api/restaurants` | Recherche : `q`, `cuisine`, `ville`, `prixMin`, `prixMax`, `noteMin`, `options` (répétable) |
| GET | `/api/restaurants/filtres` | Cuisines, villes, options et bornes de prix disponibles |
| GET | `/api/restaurants/{id}` | Fiche d'un restaurant |
| GET | `/api/restaurants/{id}/avis` | Avis du restaurant, du plus récent au plus ancien |
| POST | `/api/restaurants/{id}/avis` | **JWT** — publier un avis `{ "note": 5, "commentaire": "..." }` |
| GET | `/api/restaurants/{id}/analyse-avis` | Synthèse IA des avis (pourcentages, points forts/faibles) |
| GET | `/api/restaurants/recommandations` | **JWT** — classement IA : `budgetMax`, `cuisine`, `ville`, `options`, `limite` |
| GET | `/api/restaurants/favoris` | **JWT** — restaurants favoris de l'utilisateur |
| GET | `/api/restaurants/favoris/ids` | **JWT** — identifiants des favoris |
| POST / DELETE | `/api/restaurants/favoris/{restaurantId}` | **JWT** — ajouter / retirer un favori |
| GET | `/api/restaurants/ia/statut` | Disponibilité d'Ollama et nom du modèle |
| POST | `/api/restaurants/ia/reanalyser-avis` | Réanalyser tous les avis avec Ollama |

## Les tâches IA

### Analyse de sentiment des avis

1. `SentimentService` envoie le commentaire à llama3 via le `OllamaClient` commun et
   demande une réponse JSON `{ "sentiment": "POSITIF|NEGATIF|NEUTRE", "score": ..., "justification": "..." }`.
2. Si Ollama est indisponible ou renvoie une réponse invalide, un moteur local à lexique
   français prend le relais (négations « pas », « jamais », accords, accents).
3. La source (`OLLAMA` ou `LOCAL`) et le score sont stockés avec l'avis et affichés sur la fiche.

### Recommandation selon le budget et les goûts

- Score = 45 % note moyenne + 35 % part d'avis positifs + 20 % respect du budget.
- Si `budgetMax` n'est pas fourni, le budget est déduit des préférences du profil :
  ECONOMIQUE 20 €, MOYEN 35 €, LUXE 60 €.
- Chaque recommandation renvoie une explication lisible (`raison`) et un indicateur
  `dansBudget`.

## Intégration dans `main`

1. Ajouter l'import et les routes dans `frontend/src/app/app.routes.ts`, **avant** la
   route `'**'` :

   ```ts
   import { RESTAURANTS_ROUTES } from './restaurants/restaurants.routes';

   export const routes: Routes = [
     // ... modules 1 et 2 ...
     ...RESTAURANTS_ROUTES,
     { path: '', redirectTo: 'preferences', pathMatch: 'full' },
     { path: '**', redirectTo: 'preferences' },
   ];
   ```

   `RESTAURANTS_ROUTES` déclare `restaurants`, `restaurants/recommandations`,
   `restaurants/favoris` et `restaurants/:id`, toutes protégées par `authGuard`.

2. Ajouter les liens dans `frontend/src/app/components/navbar/navbar.component.html` :

   ```html
   <a routerLink="/restaurants" routerLinkActive="active" ariaCurrentWhenActive="page"
      title="Restaurants" aria-label="Restaurants">
     <tm-icon name="utensils"></tm-icon> <span>Restaurants</span>
   </a>
   <a routerLink="/restaurants/recommandations" routerLinkActive="active" ariaCurrentWhenActive="page"
      title="Recommandations" aria-label="Recommandations">
     <tm-icon name="sparkles"></tm-icon> <span>Recommandations</span>
   </a>
   <a routerLink="/restaurants/favoris" routerLinkActive="active" ariaCurrentWhenActive="page"
      title="Favoris" aria-label="Favoris">
     <tm-icon name="heart"></tm-icon> <span>Favoris</span>
   </a>
   ```

3. Aucune nouvelle dépendance backend ou frontend, aucun changement de configuration :
   le module réutilise `OllamaClient`, `UserService`, `ApiException`, `API_URL`,
   `authGuard` et l'intercepteur JWT du socle.

## Données de démonstration

`RestaurantDataSeeder` s'exécute après le seeder du module 1 et ne fait rien si la table
`restaurants` contient déjà des données. Il crée 12 restaurants tunisiens, 48 avis répartis
sur les comptes de démonstration actifs, et 3 favoris pour `yasmine@travelmate.tn` et
`mouhib@travelmate.tn` (si ces comptes existent).

## Tests

`SentimentServiceTest` vérifie le moteur local (positif, négatif, négation, neutre,
accents, source `LOCAL`) sans dépendre d'Ollama. Les 6 tests passent.

Le chemin llama3 réel n'est validé que lorsque Ollama est lancé avec le modèle `llama3`
(`ollama serve`, puis `ollama pull llama3`) ; sans Ollama, l'application fonctionne avec
le moteur local et l'affiche dans l'interface.

## Branche

Branche `module-4-restaurant` : uniquement les dossiers du module et cette fiche.
