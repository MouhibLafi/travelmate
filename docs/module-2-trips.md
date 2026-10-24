# Module 2 : Planification des voyages

Cette branche contient **uniquement le code du module 2**. Il utilise le module 1
(`User`, connexion JWT, `Interest`, `BudgetLevel`, préférences) : pour lancer l'application,
utilisez la branche **`main`**, qui contient tous les modules intégrés.

## Fonctionnalités

- Créer un voyage : destination, dates, budget
- Choisir les centres d'intérêt du voyage
- Modifier ou supprimer un voyage
- Liste des voyages de l'utilisateur
- **Tâche IA** : l'utilisateur écrit sa demande en texte libre (« 5 jours à Rome, 800 € »)
  et l'IA (llama3 via Ollama) remplit automatiquement le formulaire

## Fichiers

| Dossier | Contenu |
|---|---|
| `backend/src/main/java/com/travelmate/trips/entity` | `Trip` (lié à `User` du module 1) |
| `backend/src/main/java/com/travelmate/trips/repository` | `TripRepository` |
| `backend/src/main/java/com/travelmate/trips/dto` | `TripRequest`, `TripResponse`, `TripAiRequest`, `TripSuggestion` |
| `backend/src/main/java/com/travelmate/trips/service` | `TripService` (voyages), `TripAiService` (tâche IA) |
| `backend/src/main/java/com/travelmate/trips/controller` | `TripController` : routes `/api/trips` |
| `backend/src/test/java/com/travelmate/trips/service` | `TripAiServiceTest` (6 tests, sans Ollama) |
| `frontend/src/app/trips` | Page « Mes voyages » : bloc IA, formulaire, liste |

## Routes de l'API

| Méthode | URL | Description |
|---|---|---|
| GET / POST | `/api/trips` | Mes voyages / créer un voyage |
| PUT / DELETE | `/api/trips/{id}` | Modifier / supprimer un de mes voyages |
| POST | `/api/trips/ai/fill-form` | **Tâche IA** : `{ "text": "5 jours à Rome, 800 €" }` |

## La tâche IA : le modèle extrait, Java calcule

1. **Prompt** : la date du jour + la demande ; le modèle extrait destination, date de début,
   durée, budget et centres d'intérêt (liste fermée), sans rien inventer.
2. **Appel** à llama3 via `OllamaClient` (commun, dans le socle du module 1).
3. **Vérification et calculs en Java** : date de fin = début + durée, date passée ou absente
   → dans 7 jours, niveau de budget selon le budget par jour (< 80 € ECONOMIQUE, ≤ 200 € MOYEN,
   au-delà LUXE), valeurs inventées ignorées.

## Intégration dans `main` (déjà faite)

- route `voyages` dans `frontend/src/app/app.routes.ts`, protégée par `authGuard`
- lien « Mes voyages » dans la barre de navigation
- `TripService.onUserDeleted` : les voyages sont supprimés avec l'utilisateur
