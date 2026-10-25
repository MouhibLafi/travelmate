import { Categorie } from './categorie.model';
import { Destination } from './destination.model';

export interface Activite {
  id: number;
  nom: string;
  description: string;
  prix: number;
  dureeMinutes: number;
  imageUrl: string;
  tags: string;
  categorie: Categorie;
  destination: Destination;
}
