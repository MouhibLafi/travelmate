// Interfaces mirroring the Spring Boot DTOs of Module 5.

export type AccommodationType = 'HOTEL' | 'APARTMENT' | 'HOSTEL' | 'GUESTHOUSE';
export const ACCOMMODATION_TYPES: AccommodationType[] = ['HOTEL', 'APARTMENT', 'HOSTEL', 'GUESTHOUSE'];

export type Amenity =
  | 'WIFI' | 'PARKING' | 'POOL' | 'BREAKFAST' | 'AIR_CONDITIONING' | 'KITCHEN' | 'GYM'
  | 'SPA' | 'RESTAURANT' | 'BEACH_ACCESS' | 'SEA_VIEW' | 'PET_FRIENDLY' | 'WASHING_MACHINE' | 'AIRPORT_SHUTTLE';
export const AMENITIES: Amenity[] = [
  'WIFI', 'PARKING', 'POOL', 'BREAKFAST', 'AIR_CONDITIONING', 'KITCHEN', 'GYM',
  'SPA', 'RESTAURANT', 'BEACH_ACCESS', 'SEA_VIEW', 'PET_FRIENDLY', 'WASHING_MACHINE', 'AIRPORT_SHUTTLE',
];

export type PeriodStatus = 'AVAILABLE' | 'BOOKED';
export type PriceVerdict = 'UNDERPRICED' | 'FAIR' | 'OVERPRICED';

export interface AccommodationSummary {
  id: number;
  name: string;
  type: AccommodationType;
  city: string;
  country: string;
  stars: number;
  rating: number;
  pricePerNight: number;
  maxGuests: number;
  amenities: Amenity[];
  mainImageUrl: string | null;
}

export interface Accommodation extends Omit<AccommodationSummary, 'mainImageUrl'> {
  description: string | null;
  address: string | null;
  destinationId: number | null;
  imageUrls: string[];
  createdAt: string;
  updatedAt: string;
}

export interface AccommodationRequest {
  name: string;
  description?: string | null;
  type: AccommodationType;
  city: string;
  country: string;
  address?: string | null;
  stars: number;
  rating: number;
  pricePerNight: number;
  maxGuests: number;
  destinationId?: number | null;
  amenities: Amenity[];
  imageUrls: string[];
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface AccommodationSearchCriteria {
  q?: string | null;
  city?: string | null;
  type?: AccommodationType | null;
  minPrice?: number | null;
  maxPrice?: number | null;
  minStars?: number | null;
  minRating?: number | null;
  amenities?: Amenity[] | null;
  guests?: number | null;
  checkIn?: string | null;   // yyyy-MM-dd
  checkOut?: string | null;
}

export interface PageRequest {
  page: number;
  size: number;
  sort?: string; // e.g. "pricePerNight,asc"
}

export interface BookedPeriod {
  id: number;
  accommodationId: number;
  startDate: string;
  endDate: string;   // exclusive (check-out day)
  status: PeriodStatus;
  note: string | null;
}

export interface BookedPeriodRequest {
  startDate: string;
  endDate: string;
  status: PeriodStatus;
  note?: string | null;
}

export interface AvailabilityResponse {
  accommodationId: number;
  checkIn: string;
  checkOut: string;
  available: boolean;
  conflicts: BookedPeriod[];
}

export interface BookedDatesResponse {
  accommodationId: number;
  from: string;
  to: string;
  periods: BookedPeriod[];
  bookedDates: string[];
}

export interface StayCostRequest {
  checkIn: string;
  checkOut: string;
  guests: number;
}

export interface StayCostResponse {
  accommodationId: number;
  accommodationName: string;
  checkIn: string;
  checkOut: string;
  guests: number;
  nights: number;
  pricePerNight: number;
  subtotal: number;
  cleaningFee: number;
  serviceFeeRate: number;
  serviceFee: number;
  cityTaxPerPersonPerNight: number;
  cityTax: number;
  total: number;
  currency: string;
}

export interface ComparableListing {
  id: number;
  name: string;
  city: string;
  type: AccommodationType;
  stars: number;
  rating: number;
  amenitiesCount: number;
  pricePerNight: number;
  similarityScore: number;
}

export interface Baseline {
  baselinePrice: number;
  medianComparablePrice: number | null;
  minComparablePrice: number | null;
  maxComparablePrice: number | null;
  comparablesCount: number;
  scope: 'CITY' | 'COUNTRY' | 'COUNTRY_ANY_TYPE';
  comparables: ComparableListing[];
}

export interface AiResult {
  estimatedFairPrice: number;
  priceVerdict: PriceVerdict;
  differencePercent: number;
  explanation: string;
  keyFactors: string[];
  source: 'AI' | 'STATISTICAL';
  clamped: boolean;
  model: string | null;
  durationMs: number | null;
}

export interface CheaperAlternative {
  id: number;
  name: string;
  type: AccommodationType;
  city: string;
  stars: number;
  rating: number;
  pricePerNight: number;
  amenities: Amenity[];
  mainImageUrl: string | null;
  savingsPerNight: number;
  savingsPercent: number;
  similarityScore: number;
  reason: string;
}

export interface PriceAnalysis {
  target: AccommodationSummary;
  baseline: Baseline;
  aiResult: AiResult;
  alternatives: CheaperAlternative[];
  aiAvailable: boolean;
  aiMessage: string | null;
  checkIn: string | null;
  checkOut: string | null;
  generatedAt: string;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: Record<string, string>;
}
