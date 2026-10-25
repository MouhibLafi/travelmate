# TravelMate – Module 3 : Destinations et activités (backend)

Spring Boot 3 / Java 17 / MySQL (XAMPP) / Ollama (Llama 3).

## Lancer le projet

1. Démarrer **MySQL** dans XAMPP (la base `travelmate_activites` est créée automatiquement).
2. Démarrer Ollama et télécharger le modèle :
   ```
   ollama pull llama3
   ollama serve
   ```
3. Lancer l'API (port **8083**) :
   ```
   mvn spring-boot:run
   ```
   Au premier démarrage, des destinations et activités de démonstration sont insérées.

Réglages dans `src/main/resources/application.properties` (mot de passe MySQL, port, modèle Ollama, origine CORS).

## API

| Méthode | URL | Rôle |
|---|---|---|
| GET | `/api/destinations?q=` | Catalogue des destinations (recherche nom/pays/tags) |
| GET | `/api/destinations/{id}` | Détail d'une destination |
| GET | `/api/activites` | Liste + filtres : `destinationId`, `categorie`, `prixMin`, `prixMax`, `dureeMax` (minutes), `q`, `tri` (`nom`, `prix`, `duree`) |
| GET | `/api/activites/{id}` | Détail d'une activité |
| GET | `/api/categories` | Catégories (pour les filtres) |
| GET | `/api/favoris?userId=` | Activités favorites d'un utilisateur |
| POST | `/api/favoris/{activiteId}?userId=` | Ajouter aux favoris |
| DELETE | `/api/favoris/{activiteId}?userId=` | Retirer des favoris |
| GET | `/api/favoris/{activiteId}/existe?userId=` | Est-ce un favori ? |
| GET | `/api/activites/recommandations?interets=Nature,Culture&destinationId=&budgetMax=&limite=5` | **Recommandations IA** |

Exemple : `http://localhost:8083/api/activites/recommandations?interets=Histoire,Gastronomie&destinationId=1&budgetMax=70`

## Système de recommandation

1. **Filtrage par contenu** (`RecommandationService.calculerScore`) : score de 0 à 100 =
   60 pts si la catégorie correspond à un centre d'intérêt + 25 pts selon la proportion d'intérêts retrouvés dans les tags
   + 15 pts selon le budget.
2. **Llama 3 via Ollama** (`OllamaClient`) : reçoit les 10 meilleures candidates, les reclasse et écrit une raison courte
   par activité (réponse JSON, `temperature` 0.3).
3. **Repli** : si Ollama est arrêté ou répond mal, le classement de l'étape 1 est renvoyé avec une raison générique.

## Intégration avec le module 1

- `userId` est passé en paramètre. Quand l'authentification (JWT) sera disponible, remplacer ce paramètre par l'utilisateur connecté.
- Les `interets` doivent reprendre les noms de catégories (Culture, Nature, Gastronomie, Aventure, Détente, Histoire, Sport, Shopping),
  idéalement ceux déduits par l'IA du module 1. Le front Angular peut lire les centres d'intérêt du profil et les passer à l'endpoint.
