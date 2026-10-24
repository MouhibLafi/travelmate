// Les memes types que les DTO Java du backend

export type Role = 'VOYAGEUR' | 'ADMIN';

export interface User {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
  role: Role;
  active: boolean;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface Preferences {
  description: string | null;
  interests: string[];        // ex : ["PLAGE", "GASTRONOMIE"]
  budgetLevel: string | null; // "ECONOMIQUE" | "MOYEN" | "LUXE"
  aiGenerated: boolean;
}

export interface AiResponse {
  interests: string[];
  budgetLevel: string;
  confidence: number;  // entre 0 et 1
  explanation: string;
}

// Liste des centres d'interet (meme ordre que l'enum Java Interest)
export const INTERESTS = [
  { value: 'CULTURE', label: 'Culture', icon: 'landmark' },
  { value: 'HISTOIRE', label: 'Histoire', icon: 'castle' },
  { value: 'PLAGE', label: 'Plage', icon: 'sun' },
  { value: 'NATURE', label: 'Nature', icon: 'mountain' },
  { value: 'GASTRONOMIE', label: 'Gastronomie', icon: 'utensils' },
  { value: 'AVENTURE', label: 'Aventure', icon: 'compass' },
  { value: 'SHOPPING', label: 'Shopping', icon: 'shopping-bag' },
  { value: 'VIE_NOCTURNE', label: 'Vie nocturne', icon: 'moon' },
  { value: 'SPORT', label: 'Sport', icon: 'bike' },
  { value: 'DETENTE', label: 'Détente', icon: 'heart' },
];

export const BUDGETS = [
  { value: 'ECONOMIQUE', label: 'Économique', hint: 'Auberges, bons plans' },
  { value: 'MOYEN', label: 'Moyen', hint: 'Confort standard' },
  { value: 'LUXE', label: 'Luxe', hint: 'Haut de gamme' },
];
