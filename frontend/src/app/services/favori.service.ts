import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Activite } from '../models/activite.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class FavoriService {
  private readonly apiUrl = `${environment.apiUrl}/api/favoris`;

  /** Identifiant utilisateur fixe en attendant l'intégration JWT du Module 1 */
  readonly userId = 1;

  constructor(private http: HttpClient) {}

  getFavoris(userId: number = this.userId): Observable<Activite[]> {
    const params = new HttpParams().set('userId', String(userId));
    return this.http.get<Activite[]>(this.apiUrl, { params });
  }

  ajouter(activiteId: number, userId: number = this.userId): Observable<void> {
    const params = new HttpParams().set('userId', String(userId));
    return this.http.post<void>(`${this.apiUrl}/${activiteId}`, null, { params });
  }

  retirer(activiteId: number, userId: number = this.userId): Observable<void> {
    const params = new HttpParams().set('userId', String(userId));
    return this.http.delete<void>(`${this.apiUrl}/${activiteId}`, { params });
  }

  estFavori(activiteId: number, userId: number = this.userId): Observable<boolean> {
    const params = new HttpParams().set('userId', String(userId));
    return this.http.get<boolean>(`${this.apiUrl}/${activiteId}/existe`, { params });
  }
}
