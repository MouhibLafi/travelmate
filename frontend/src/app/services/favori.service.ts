import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Activite } from '../models/activite.model';
import { API_URL } from './api';

// Activites favorites de l'utilisateur CONNECTE.
// Integration : plus de userId fixe ; le backend lit l'utilisateur dans le jeton JWT (module 1),
// ajoute automatiquement par l'intercepteur.
@Injectable({
  providedIn: 'root'
})
export class FavoriService {
  private readonly apiUrl = `${API_URL}/favoris`;

  constructor(private http: HttpClient) {}

  getFavoris(): Observable<Activite[]> {
    return this.http.get<Activite[]>(this.apiUrl);
  }

  ajouter(activiteId: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${activiteId}`, null);
  }

  retirer(activiteId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${activiteId}`);
  }

  estFavori(activiteId: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.apiUrl}/${activiteId}/existe`);
  }
}
