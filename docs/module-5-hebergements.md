# TravelMate

Team mini-project for the course **"IA for Software Engineering"**.
Stack: **Spring Boot 3 (Java 17, layered MVC)** · **Angular 18 (standalone, Angular Material)** · **local AI with Ollama (llama3)**.

```
backend/    Spring Boot API   → http://localhost:8080   (Swagger: /swagger-ui.html)
frontend/   Angular app       → http://localhost:4200
```

---

## Module 5 – Accommodations (Hébergements)

Accommodation catalog with search & filters, availability calendar, stay cost calculator and an
**AI fair-price prediction with cheaper alternatives** running 100 % locally on llama3.

The module is self-contained:

| Layer | Location |
|---|---|
| Backend | `backend/src/main/java/com/travelmate/accommodation/**` |
| Backend tests | `backend/src/test/java/com/travelmate/accommodation/**` |
| Frontend | `frontend/src/app/features/accommodation/**` (lazy-loaded at `/accommodations`) |

Other modules are referenced only through plain fields (`city`, `country`, optional `destinationId`); no
teammate code is touched. The error handler and CORS mapping are scoped to `com.travelmate.accommodation` and
`/api/accommodations/**`.

### 1. Prerequisites

| Tool | Version | Check |
|---|---|---|
| Java | 17+ | `java -version` |
| Maven | 3.9+ (or your IDE) | `mvn -v` |
| Node.js | 18.19+ / 20 / 22 | `node -v` |
| MySQL | 8.x running on `localhost:3306` | `mysql -u root -p` |
| Ollama | latest | `ollama --version` — https://ollama.com/download |

### 2. Run it

```bash
# 1) Local AI (once), then keep the server running
ollama pull llama3
ollama serve                 # listens on http://localhost:11434 (already running if you use the desktop app)

# 2) Backend (creates the `travelmate` database and seeds 42 accommodations on first start)
cd backend
mvn spring-boot:run          # DB credentials: DB_USERNAME / DB_PASSWORD env vars (default root / root)

# 3) Frontend
cd frontend
npm install
npm start                    # = ng serve → http://localhost:4200/accommodations
```

Useful URLs:

* App: http://localhost:4200/accommodations — admin: http://localhost:4200/accommodations/admin
* Swagger UI: http://localhost:8080/swagger-ui.html
* AI health: http://localhost:8080/api/accommodations/ai/status (503 if Ollama is not reachable)

**No MySQL at hand?** Run the backend on an in-memory H2 database (test classpath only, data lost on stop):

```bash
cd backend
mvn test-compile spring-boot:run -Dspring-boot.run.useTestClasspath=true -Dspring-boot.run.profiles=h2 \
  -Dspring-boot.run.arguments=--spring.config.additional-location=file:src/test/resources/
```

**Tests:** `cd backend && mvn test` → 33 tests (unit + MockMvc/H2 integration). Ollama is mocked, so tests
never need a running model.

### 3. Configuration (`backend/src/main/resources/application.properties`)

```properties
travelmate.ai.ollama.base-url=http://localhost:11434
travelmate.ai.ollama.model=llama3
travelmate.ai.ollama.temperature=0.2
travelmate.ai.ollama.connect-timeout=3s
travelmate.ai.ollama.read-timeout=60s
travelmate.ai.ollama.enabled=true            # false = always use the statistical fallback

spring.cache.caffeine.spec=maximumSize=500,expireAfterWrite=10m   # price-analysis cache

travelmate.accommodation.pricing.service-fee-rate=0.08
travelmate.accommodation.pricing.default-cleaning-fee=15          # APARTMENT/GUESTHOUSE 25, HOSTEL 5
travelmate.accommodation.pricing.city-taxes.rome=6.00             # per person per night (default 3.00)
travelmate.accommodation.seed.enabled=true
```

### 4. Data model

```
Accommodation                                  BookedPeriod
─────────────                                  ────────────
id, name, description                          id
type: HOTEL|APARTMENT|HOSTEL|GUESTHOUSE  1───* accommodation (FK)
city, country, address, destinationId?        startDate  (first night, inclusive)
stars (1-5), rating (0-5)                      endDate    (check-out day, exclusive)
pricePerNight, maxGuests                       status: AVAILABLE|BOOKED  (only BOOKED blocks)
amenities: Set<Amenity>  (WIFI, POOL, …14)     note
imageUrls: List<String>
createdAt, updatedAt
```

Backend layers: `controller → service (interface + impl) → repository`, DTOs (Java records) + manual
`AccommodationMapper`; entities never leave the service layer. Bean Validation on every request DTO, and a
`@RestControllerAdvice` returns uniform error JSON:

```json
{ "timestamp": "2026-10-24T13:41:32Z", "status": 409, "error": "Conflict",
  "message": "Not available: already booked from 2026-10-27 to 2026-11-01",
  "path": "/api/accommodations/1/stay-cost" }
```

`400` validation (with `fieldErrors`) · `404` unknown id · `409` booked dates / overlapping period · `503` AI status when Ollama is down.

### 5. REST endpoints

| Method | URL | Description |
|---|---|---|
| GET | `/api/accommodations` | Search (paginated, sorted, filtered) |
| GET | `/api/accommodations/{id}` | Details |
| POST | `/api/accommodations` | Create → `201` + `Location` |
| PUT | `/api/accommodations/{id}` | Update |
| DELETE | `/api/accommodations/{id}` | Delete (with its periods) → `204` |
| GET | `/api/accommodations/cities` | Distinct cities (autocomplete) |
| GET | `/api/accommodations/{id}/availability?checkIn&checkOut` | Free for this range? + conflicts |
| GET | `/api/accommodations/{id}/booked-dates?from&to` | Booked nights for a calendar (default: today → +6 months) |
| GET | `/api/accommodations/{id}/periods` | All periods (admin) |
| POST | `/api/accommodations/{id}/periods` | Add a period (`409` if a BOOKED one overlaps) |
| DELETE | `/api/accommodations/{id}/periods/{periodId}` | Remove a period |
| POST | `/api/accommodations/{id}/stay-cost` | Stay cost calculator |
| GET | `/api/accommodations/{id}/price-analysis?checkIn&checkOut&guests` | **AI fair price + cheaper alternatives** |
| GET | `/api/accommodations/ai/status` | Is Ollama reachable? (`503` if not) |

#### Search

Filters (all optional): `q` (name/city/country), `city`, `type`, `minPrice`, `maxPrice`, `minStars`,
`minRating`, `amenities` (comma-separated, **all** required), `guests`, `checkIn` + `checkOut` (excludes
accommodations with an overlapping BOOKED period). Paging: `page`, `size` (≤ 100),
`sort=pricePerNight|rating|stars|name|city|maxGuests|createdAt,(asc|desc)`. Built with composable
**JPA Specifications** (`AccommodationSpecifications`), including a `NOT EXISTS` sub-query for availability.

```http
GET /api/accommodations?city=rome&checkIn=2026-12-03&checkOut=2026-12-06&guests=2&amenities=WIFI,BREAKFAST&sort=rating,desc&size=2
```
```json
{
  "content": [
    { "id": 3, "name": "Palazzo Navona Luxury Suites", "type": "HOTEL", "city": "Rome", "country": "Italy",
      "stars": 5, "rating": 4.8, "pricePerNight": 390.00, "maxGuests": 2,
      "amenities": ["WIFI","BREAKFAST","AIR_CONDITIONING","GYM","SPA","RESTAURANT","AIRPORT_SHUTTLE"],
      "mainImageUrl": "https://images.unsplash.com/photo-…" }
  ],
  "page": 0, "size": 2, "totalElements": 4, "totalPages": 2, "first": true, "last": false
}
```

#### Stay cost calculator

```
subtotal   = nights × pricePerNight
cleaning   = flat fee by type (hotel 15 €, apartment/guesthouse 25 €, hostel 5 €)
service    = subtotal × 8 %
city tax   = guests × nights × city rate (Rome 6 €, Paris 4 €, Barcelona 3.50 €, Istanbul 2 €, Tunisia 1.50 €)
total      = subtotal + cleaning + service + city tax
```
Validated: `checkOut > checkIn`, `checkIn` not in the past, ≤ 60 nights, `guests ≤ maxGuests` (400), not booked (409).

```http
POST /api/accommodations/1/stay-cost
{ "checkIn": "2026-12-03", "checkOut": "2026-12-06", "guests": 2 }
```
```json
{ "accommodationId": 1, "accommodationName": "Hotel Colosseo Vista",
  "checkIn": "2026-12-03", "checkOut": "2026-12-06", "guests": 2, "nights": 3,
  "pricePerNight": 185.00, "subtotal": 555.00, "cleaningFee": 15.00,
  "serviceFeeRate": 0.08, "serviceFee": 44.40,
  "cityTaxPerPersonPerNight": 6.00, "cityTax": 36.00, "total": 650.40, "currency": "EUR" }
```

#### Availability

```http
GET /api/accommodations/1/booked-dates
```
```json
{ "accommodationId": 1, "from": "2026-10-24", "to": "2027-04-24",
  "periods": [ { "id": 1, "accommodationId": 1, "startDate": "2026-10-27", "endDate": "2026-11-01", "status": "BOOKED", "note": "Demo booking" } ],
  "bookedDates": ["2026-10-27","2026-10-28","2026-10-29","2026-10-30","2026-10-31"] }
```
(The check-out day 2026-11-01 stays free: a new guest can arrive that day.)

#### AI price analysis

```http
GET /api/accommodations/1/price-analysis?checkIn=2026-11-08&checkOut=2026-11-10&guests=2
```
```json
{
  "target": { "id": 1, "name": "Hotel Colosseo Vista", "type": "HOTEL", "city": "Rome", "stars": 4, "rating": 4.5, "pricePerNight": 185.00, "…": "…" },
  "baseline": {
    "baselinePrice": 170.32, "medianComparablePrice": 165.00,
    "minComparablePrice": 110.00, "maxComparablePrice": 390.00,
    "comparablesCount": 3, "scope": "CITY",
    "comparables": [ { "id": 6, "name": "Hotel Vaticano Garden", "stars": 4, "rating": 4.3, "amenitiesCount": 4, "pricePerNight": 165.00, "similarityScore": 0.896 }, "…" ]
  },
  "aiResult": {
    "estimatedFairPrice": 176.00, "priceVerdict": "FAIR", "differencePercent": 5.1,
    "explanation": "Comparable 4-star hotels in Rome cost 110-390 EUR, with a median of 165 EUR. The rooftop terrace, gym and breakfast justify a small premium over that median.",
    "keyFactors": ["Close to the Colosseum", "Gym and restaurant", "Above-average rating"],
    "source": "AI", "clamped": false, "model": "llama3", "durationMs": 7421
  },
  "alternatives": [
    { "id": 6, "name": "Hotel Vaticano Garden", "type": "HOTEL", "city": "Rome", "stars": 4, "rating": 4.3,
      "pricePerNight": 165.00, "savingsPerNight": 20.00, "savingsPercent": 10.8, "similarityScore": 0.896,
      "reason": "Same city, 4★ hotel with breakfast and free parking", "…": "…" }
  ],
  "aiAvailable": true, "aiMessage": null,
  "checkIn": "2026-11-08", "checkOut": "2026-11-10", "generatedAt": "2026-10-24T14:02:11Z"
}
```
The `aiResult` text above is illustrative: llama3's wording changes between runs. When Ollama is down the
response is still `200`, with `"aiAvailable": false`, `"source": "STATISTICAL"` and an explanation generated
from the baseline.

### 6. The AI pipeline (for the presentation)

```
           ┌─────────────────────────┐
 request → │ a) Comparables          │  same city + same type + stars ±1, ranked by similarity (max 10)
           │    (JPA Specifications) │  < 3 found → same country + same type → same country, any type
           └───────────┬─────────────┘
                       ▼
           ┌─────────────────────────┐
           │ b) Statistical baseline │  each comparable price normalised to the target:
           │    (BaselineCalculator) │  price × (1+12%·Δstars) × (1+6%·Δrating) × (1+2%·Δamenities)
           └───────────┬─────────────┘  baseline = MEDIAN of normalised prices (robust to luxury outliers)
                       ▼
           ┌─────────────────────────┐
           │ c) llama3 via Ollama    │  PromptBuilder → POST /api/chat {model: llama3, stream: false,
           │    (OllamaClient)       │  format: "json", temperature: 0.2}, 60 s timeout, duration logged
           └───────────┬─────────────┘
                       ▼
           ┌─────────────────────────┐
           │ d) Validation           │  parse JSON defensively (code fences, "155 EUR" strings…)
           │    (LlmResponseParser)  │  clamp estimatedFairPrice to baseline ±40 %
           └───────────┬─────────────┘  RECOMPUTE differencePercent and verdict ourselves (±10 % = FAIR)
                       ▼
           ┌─────────────────────────┐
           │ e) Fallback             │  Ollama down / timeout / invalid JSON → baseline as fair price,
           │                         │  generated explanation, aiAvailable=false. Never a 500.
           └───────────┬─────────────┘
                       ▼
           ┌─────────────────────────┐
           │ f) Cheaper alternatives │  same country, price < target, fits guests, free on the dates,
           │                         │  top 5 by similarity score; 2nd llama3 call writes one-line reasons
           └─────────────────────────┘  (rule-based reasons if AI unavailable). Result cached 10 min.
```

**Why this design?**

* **The LLM is grounded, not trusted.** llama3 gets real market data (the comparables and the baseline) in the
  prompt, so it reasons about this market instead of inventing prices. Everything it returns is validated:
  an 8B model can hallucinate a 900 € estimate or a verdict that contradicts its own number, so we clamp the
  price and recompute the difference % and the verdict deterministically.
* **Statistics first, AI second.** The baseline is explainable (median, adjustment factors shown in the UI under
  "How was this computed?") and always available. The AI adds a nuanced estimate and a human-readable explanation.
* **Graceful degradation.** Ollama being stopped, slow (> 60 s) or returning garbage never breaks the page; the
  user sees a statistical estimate with a discreet notice. If the first AI call fails, the reasons call is
  skipped so the user doesn't wait for a second timeout.
* **Performance.** Local inference takes seconds, so results are cached per (accommodation, dates, guests) for
  10 minutes with Caffeine. Fallback results are *not* cached, so the AI is retried as soon as Ollama is back.
  The cache is cleared when an accommodation or its periods change. DB reads happen in a read-only transaction
  *before* the LLM call, so no connection is held while the model thinks.
* **Similarity score** (for comparables and alternatives) = 30 % same city + 20 % same type + 20 % star
  proximity + 20 % amenity overlap (Jaccard) + 10 % rating proximity.

Main classes: `ai/OllamaClient`, `ai/PromptBuilder`, `ai/LlmResponseParser`,
`service/analysis/ComparablesFinder`, `service/analysis/BaselineCalculator`, `service/analysis/SimilarityScorer`,
`service/impl/PriceAnalysisServiceImpl`.

### 7. Frontend

* **Design system:** teal `#0E7C86`, navy `#12263A`, coral `#FF6B4A`; AI features in violet `#7C5CFF` with a
  violet→teal gradient and the ✨ `auto_awesome` icon. Poppins (headings) + Inter (body). Radius 8 px for inputs
  and buttons, 16 px for cards. Tokens are in `src/styles/_variables.scss`, and the Material custom theme is in
  `src/styles/_theme.scss`.
* **Stays list** (`/accommodations`): search bar (city autocomplete, date range, guests), filters panel (price
  range slider, type, stars, rating, amenities), card grid, sort, pagination, loading skeletons and an empty
  state. The search state lives in the URL, so it can be shared and the back button works.
* **Detail page** (`/accommodations/:id`): gallery, description, amenity icons, an availability calendar
  (booked nights struck through; click to select a stay), and a sticky stay cost calculator with a live
  breakdown. The calendar and calculator share the same dates.
* **AI price panel:** "Analyze price with AI" → animated loading steps → estimated fair price, verdict gauge
  with marker, difference %, explanation, key-factor chips, a "How was this computed?" panel with the
  comparables, and a "Similar but cheaper" carousel with *Save X €/night* badges. Shows a discreet notice when
  `aiAvailable` is false.
* **Admin** (`/accommodations/admin`): mat-table with server-side search/sort/pagination, a create/edit reactive
  form (client and server validation messages, amenity chips, image URL list with preview), a delete
  confirmation dialog and an availability management dialog.
* A typed `AccommodationService` (HttpClient) with interfaces that mirror the DTOs; the API URL is in
  `environment.ts`; routes are lazy-loaded; snackbars report errors and successes.
