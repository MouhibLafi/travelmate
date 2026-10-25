import { Activite } from './activite.model';

export interface RecommandationDTO {
  activite: Activite;
  score: number;
  raison: string;
}
