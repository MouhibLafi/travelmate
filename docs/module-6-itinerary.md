# Module 6 : Itinéraire et budget

Cette branche contient **uniquement le code du module 6**. Il utilise le module 1
(`User`, connexion JWT, `Interest`, préférences, `OllamaClient`) et le module 2 (`Trip`) :
pour lancer l'application, utilisez la branche **`main`**, qui contient tous les modules intégrés.

## Fonctionnalités

- Itinéraire jour par jour pour un voyage du module 2 : hébergement, activités, déjeuner et dîner, avec les horaires
- Estimation du coût total (prix × quantité, ex. nuits d'hôtel) et du coût de chaque journée
- Ajouter, modifier ou retirer un élément, depuis le catalogue ou à la main
- Suivi du budget : coût prévu comparé aux dépenses réelles, par catégorie
- Export PDF de l'itinéraire
- **Tâche IA 1** : l'IA (llama3 via Ollama) génère l'itinéraire personnalisé selon les centres d'intérêt et le budget
- **Tâche IA 2** : si le budget est dépassé, l'IA remplace automatiquement des éléments par des alternatives moins chères
  et explique chaque remplacement (aussi disponible avec le bouton « Ajuster au budget »)

## Fichiers

| Dossier | Contenu |
|---|---|
| `backend/src/main/java/com/travelmate/itinerary/entity` | `Itinerary` (lié à `User`), `ItineraryDay`, `ItineraryItem`, `Expense`, `ItemType` |
| `backend/src/main/java/com/travelmate/itinerary/repository` | 4 repositories |
| `backend/src/main/java/com/travelmate/itinerary/dto` | `TripInfo` (voyage du module 2), `ItineraryDto`, `OptimizationResultDto`, `BudgetSummaryDto`… |
| `backend/src/main/java/com/travelmate/itinerary/catalog` | `CatalogProvider` (activités, restaurants, hébergements) et `DemoCatalogProvider` |
| `backend/src/main/java/com/travelmate/itinerary/service` | `AiItineraryService` (IA 1), `BudgetOptimizerService` (IA 2), `ItineraryService`, `BudgetService`, `PdfService`, `TripPlanningService` |
| `backend/src/main/java/com/travelmate/itinerary/controller` | `ItineraryController`, `BudgetController` : routes `/api/itinerary` |
| `backend/src/main/resources/itinerary/catalogue-demo.json` | Catalogue de démonstration : Rome, Paris, Tunis, Djerba, Marrakech |
| `backend/src/test/java/com/travelmate/itinerary/service` | 32 tests, sans Ollama (faux client IA) |
| `frontend/src/app/itinerary` | Pages « Mes itinéraires » et détail d'un itinéraire, formulaire d'élément, suivi du budget |

Tables : `itineraries`, `itinerary_days`, `itinerary_items`, `itinerary_expenses`.

## Routes de l'API

| Méthode | URL | Description |
|---|---|---|
| GET | `/api/itinerary` | Mes itinéraires |
| POST | `/api/itinerary/generate` | **Tâche IA** : `{ "tripId": 3 }`, génère (ou régénère) l'itinéraire et ajuste le budget |
| GET / DELETE | `/api/itinerary/{id}` | Lire / supprimer un de mes itinéraires |
| POST | `/api/itinerary/{id}/optimize` | **Tâche IA** : remplacer des éléments par moins cher |
| GET | `/api/itinerary/{id}/catalog?type=ACTIVITE` | Catalogue de la destination (ACTIVITE, RESTAURANT, HEBERGEMENT) |
| POST | `/api/itinerary/{id}/days/{dayId}/items` | Ajouter un élément à une journée |
| PUT / DELETE | `/api/itinerary/items/{itemId}` | Modifier / retirer un élément |
| GET | `/api/itinerary/{id}/budget` | Résumé du budget par catégorie |
| GET / POST | `/api/itinerary/{id}/expenses` | Mes dépenses / ajouter une dépense |
| DELETE | `/api/itinerary/expenses/{expenseId}` | Supprimer une dépense |
| GET | `/api/itinerary/{id}/pdf` | Export PDF |

Les voyages à choisir viennent du module 2 : `GET /api/trips`.

## Les tâches IA : l'IA choisit, Java vérifie et calcule

1. **Le budget est filtré en Java avant le prompt** : au plus 45 % pour l'hébergement, le reste réparti par jour.
   Le modèle ne voit que les hébergements, activités et restaurants que le budget permet (un modèle de 8 milliards
   de paramètres additionne mal les prix).
2. **Prompt** en français : le voyage, une limite par nuit et par jour, les centres d'intérêt et le catalogue avec ses ids.
   Le modèle répond en JSON : un hébergement, puis pour chaque jour des éléments (id, heures, une note qui explique le choix).
3. **Vérification en Java** : ids inventés ou répétés ignorés, heures illisibles ignorées, déjeuner ou dîner manquant ajouté,
   chevauchements décalés, jours oubliés complétés par des règles. Les prix viennent toujours du catalogue.
4. **Budget dépassé** : Java liste les alternatives moins chères, l'IA choisit les remplacements et les explique,
   Java les applique un par un et **s'arrête dès que le budget est tenu**. Si ce n'est pas assez, un algorithme glouton
   termine, et en dernier recours une activité payante est retirée.
5. **Sans Ollama**, le module fonctionne quand même : le plan est construit par des règles (« Plan construit sans IA »).

Autres règles : un voyage sans montant reçoit un budget estimé selon son niveau (ÉCONOMIQUE 60 €/jour, MOYEN 140 €, LUXE 300 €).
Un voyage sans centres d'intérêt utilise ceux des préférences de l'utilisateur (module 1).

## Intégration dans `main`

À ajouter dans `main` :

1. **Route** dans `frontend/src/app/app.routes.ts` :
   ```ts
   import { ItinerariesComponent } from './itinerary/itineraries.component';
   import { ItineraryDetailComponent } from './itinerary/itinerary-detail.component';

   // ----- Module 6 : itineraire et budget -----
   { path: 'itineraires', component: ItinerariesComponent, canActivate: [authGuard] },
   { path: 'itineraires/:id', component: ItineraryDetailComponent, canActivate: [authGuard] },
   ```
2. **Lien de menu** dans `frontend/src/app/components/navbar/navbar.component.html`, après « Mes voyages » :
   ```html
   <a routerLink="/itineraires" routerLinkActive="active" ariaCurrentWhenActive="page"
      title="Mes itinéraires" aria-label="Mes itinéraires">
     <tm-icon name="calendar"></tm-icon> <span>Itinéraires</span>
   </a>
   ```
3. **Changement demandé au socle** : une dépendance dans `backend/pom.xml`, pour l'export PDF :
   ```xml
   <!-- Module 6 : export PDF de l'itineraire -->
   <dependency>
       <groupId>com.github.librepdf</groupId>
       <artifactId>openpdf</artifactId>
       <version>3.0.5</version>
   </dependency>
   ```

Déjà fait dans le module : `ItineraryService.onUserDeleted` supprime les itinéraires (et leurs dépenses) avec l'utilisateur.
Le voyage est gardé par son id sans clé étrangère : supprimer un voyage dans le module 2 ne plante pas,
l'itinéraire devenu orphelin est supprimé à la lecture suivante.

## Quand les modules 3, 4 et 5 seront prêts

Le module 6 lit les activités, restaurants et hébergements uniquement via l'interface `CatalogProvider`
(`getActivities`, `getRestaurants`, `getHotels` pour une destination). Il suffira d'écrire une implémentation qui appelle
leurs services, de convertir leurs entités en `CatalogItem` (catégorie d'une activité = un `Interest`), puis de supprimer
`DemoCatalogProvider` et `catalogue-demo.json`.

## Contributeur

Module développé par **Badiss Benzarti**.
