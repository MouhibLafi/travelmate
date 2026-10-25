// Types du module 4 (memes champs que les DTO Java)

export interface Restaurant {
  id: number;
  nom: string;
  cuisine: string;
  description: string;
  adresse: string;
  ville: string;
  prixMoyen: number;
  fourchettePrix: string;
  optionsAlimentaires: string[];
  noteMoyenne: number;
  nombreAvis: number;
  imageUrl: string;
}

export type Sentiment = 'POSITIF' | 'NEGATIF' | 'NEUTRE';

export interface Avis {
  id: number;
  restaurantId: number;
  auteur: string;          // prenom + nom de l'utilisateur connecte
  note: number;
  commentaire: string;
  sentiment: Sentiment;
  scoreSentiment: number;
  sourceSentiment: 'OLLAMA' | 'LOCAL';
  dateCreation: string;
}

// L'auteur n'est plus envoye : le backend le deduit du jeton JWT.
export interface AvisRequest {
  note: number;
  commentaire: string;
}

export interface Recommandation {
  restaurant: Restaurant;
  score: number;
  dansBudget: boolean;
  raison: string;
}

export interface AnalyseAvis {
  restaurantId: number;
  nombreAvis: number;
  pourcentagePositifs: number;
  pourcentageNegatifs: number;
  pourcentageNeutres: number;
  noteMoyenne: number;
  pointsForts: string[];
  pointsFaibles: string[];
}

export interface MetaFiltres {
  cuisines: string[];
  villes: string[];
  optionsAlimentaires: string[];
  prixMinimum: number;
  prixMaximum: number;
}

export interface StatutIa {
  disponible: boolean;
  modele: string;
  url: string;
  message: string;
}

export interface Reanalyse {
  avisAnalyses: number;
  avisModifies: number;
  message: string;
}

export interface FiltresRestaurants {
  q?: string;
  cuisine?: string;
  ville?: string;
  prixMin?: number;
  prixMax?: number;
  noteMin?: number;
  options?: string[];
}

export interface FiltresRecommandation {
  budgetMax?: number;
  cuisine?: string;
  ville?: string;
  options?: string[];
  limite?: number;
}
