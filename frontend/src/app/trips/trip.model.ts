// MODULE 2 - Planification des voyages
// Memes champs que TripResponse.java

export interface Trip {
  id: number;
  destination: string;
  startDate: string;          // format "2026-12-24"
  endDate: string;
  budget: number | null;      // en euros
  interests: string[];        // memes valeurs que le module 1 (INTERESTS)
  budgetLevel: string | null; // memes valeurs que le module 1 (BUDGETS)
}

// Donnees envoyees pour creer / modifier un voyage (sans l'id)
export type TripRequest = Omit<Trip, 'id'>;

// Resultat de l'IA (TripSuggestion.java) : le formulaire pre-rempli
export interface TripSuggestion {
  destination: string | null;
  startDate: string;
  endDate: string;
  budget: number | null;
  interests: string[];
  budgetLevel: string | null;
  confidence: number;  // entre 0 et 1
  explanation: string;
}
