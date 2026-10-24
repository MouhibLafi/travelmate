import { HttpErrorResponse } from '@angular/common/http';

// Adresse du backend Spring Boot
export const API_URL = 'http://localhost:8080/api';

// Transforme une erreur HTTP en message lisible.
// Le backend renvoie toujours { "message": "..." } en cas d'erreur.
export function getErrorMessage(error: HttpErrorResponse): string {
  if (error.status === 0) {
    return 'Serveur injoignable : le backend Spring Boot est-il lancé ?';
  }
  return error.error?.message ?? 'Une erreur est survenue.';
}
