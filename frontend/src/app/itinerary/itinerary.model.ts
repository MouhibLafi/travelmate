// MODULE 6 - Itineraire et budget
// Memes champs que les DTO Java (itinerary/dto)

export type ItemType = 'ACTIVITE' | 'RESTAURANT' | 'HEBERGEMENT' | 'TRANSPORT' | 'AUTRE';

export const ITEM_TYPES: { value: ItemType; label: string }[] = [
  { value: 'ACTIVITE', label: 'Activité' },
  { value: 'RESTAURANT', label: 'Restaurant' },
  { value: 'HEBERGEMENT', label: 'Hébergement' },
  { value: 'TRANSPORT', label: 'Transport' },
  { value: 'AUTRE', label: 'Autre' },
];

export function getTypeLabel(type: ItemType): string {
  return ITEM_TYPES.find((t) => t.value === type)?.label ?? type;
}

export interface ItineraryItem {
  id: number;
  type: ItemType;
  refId: number | null;
  name: string;
  category: string | null;
  startTime: string | null;   // "09:00:00"
  endTime: string | null;
  unitPrice: number;
  quantity: number;
  estimatedCost: number;
  notes: string | null;
}

export interface ItineraryDay {
  id: number;
  dayNumber: number;
  date: string;
  title: string;
  dayCost: number;
  items: ItineraryItem[];
}

export interface Itinerary {
  id: number;
  tripId: number;
  destination: string;
  startDate: string;
  endDate: string;
  budget: number;
  totalEstimatedCost: number;
  overBudget: boolean;
  generatedByAi: boolean;   // false = plan construit par les regles (IA indisponible)
  createdAt: string;
  days: ItineraryDay[];
}

// Un remplacement fait par l'optimiseur de budget
export interface Replacement {
  itemId: number;
  dayNumber: number;
  type: ItemType;
  oldName: string;
  newName: string;
  oldCost: number;
  newCost: number;
  reason: string;
}

export interface OptimizationResult {
  itinerary: Itinerary;
  budget: number;
  totalBefore: number;
  totalAfter: number;
  withinBudget: boolean;
  aiUsed: boolean;
  replacements: Replacement[];
  message: string;
}

// optimization vaut null si l'itineraire genere tenait deja dans le budget
export interface GenerationResult {
  itinerary: Itinerary;
  optimization: OptimizationResult | null;
}

// Element du catalogue (modules 3, 4, 5 ; catalogue de demonstration pour l'instant)
export interface CatalogItem {
  id: number;
  type: ItemType;
  name: string;
  category: string;
  price: number;
  durationMinutes: number | null;
  rating: number | null;
}

export interface ItemRequest {
  type: ItemType;
  refId: number | null;
  name: string | null;
  category: string | null;
  startTime: string | null;
  endTime: string | null;
  unitPrice: number | null;
  quantity: number | null;
  notes: string | null;
}

export interface CategoryBudget {
  category: ItemType;
  estimated: number;
  spent: number;
}

export interface BudgetSummary {
  budget: number;
  estimatedTotal: number;
  spentTotal: number;
  remainingEstimated: number;
  remainingActual: number;
  overBudget: boolean;
  categories: CategoryBudget[];
}

export interface Expense {
  id: number;
  category: ItemType;
  amount: number;
  date: string;
  description: string | null;
}

export type ExpenseRequest = Omit<Expense, 'id'>;

// 1234.5 -> "1 234,50 €"
export function formatEuros(amount: number): string {
  return amount.toLocaleString('fr-FR', { style: 'currency', currency: 'EUR' });
}

// "2026-12-04" -> "4 déc. 2026"
export function formatDate(date: string): string {
  return new Date(date).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' });
}
