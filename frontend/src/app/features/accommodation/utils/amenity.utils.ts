import { AccommodationType, Amenity } from '../models/accommodation.models';

export const AMENITY_META: Record<Amenity, { label: string; icon: string }> = {
  WIFI: { label: 'Wi-Fi', icon: 'wifi' },
  PARKING: { label: 'Parking', icon: 'local_parking' },
  POOL: { label: 'Pool', icon: 'pool' },
  BREAKFAST: { label: 'Breakfast', icon: 'free_breakfast' },
  AIR_CONDITIONING: { label: 'Air conditioning', icon: 'ac_unit' },
  KITCHEN: { label: 'Kitchen', icon: 'kitchen' },
  GYM: { label: 'Gym', icon: 'fitness_center' },
  SPA: { label: 'Spa', icon: 'spa' },
  RESTAURANT: { label: 'Restaurant', icon: 'restaurant' },
  BEACH_ACCESS: { label: 'Beach access', icon: 'beach_access' },
  SEA_VIEW: { label: 'Sea view', icon: 'water' },
  PET_FRIENDLY: { label: 'Pet friendly', icon: 'pets' },
  WASHING_MACHINE: { label: 'Washing machine', icon: 'local_laundry_service' },
  AIRPORT_SHUTTLE: { label: 'Airport shuttle', icon: 'airport_shuttle' },
};

export const TYPE_META: Record<AccommodationType, { label: string; icon: string }> = {
  HOTEL: { label: 'Hotel', icon: 'hotel' },
  APARTMENT: { label: 'Apartment', icon: 'apartment' },
  HOSTEL: { label: 'Hostel', icon: 'night_shelter' },
  GUESTHOUSE: { label: 'Guesthouse', icon: 'cottage' },
};

/** Inline SVG shown when a listing image is missing or fails to load. */
export const IMAGE_PLACEHOLDER =
  'data:image/svg+xml;utf8,' +
  encodeURIComponent(`<svg xmlns="http://www.w3.org/2000/svg" width="800" height="500" viewBox="0 0 800 500">
  <defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#0E7C86"/><stop offset="1" stop-color="#12263A"/></linearGradient></defs>
  <rect width="800" height="500" fill="url(#g)"/>
  <g fill="none" stroke="#ffffff" stroke-opacity=".55" stroke-width="14" stroke-linecap="round" stroke-linejoin="round">
    <path d="M300 300v-90h200a40 40 0 0 1 40 40v50M260 330h280M260 230v120M540 300v50"/><circle cx="340" cy="250" r="22"/>
  </g></svg>`);
