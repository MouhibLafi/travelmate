import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../services/api';
import { Restaurant } from './restaurant.model';

// Donnees du formulaire admin (memes champs que RestaurantRequest.java)
export interface RestaurantRequest {
  nom: string;
  cuisine: string;
  description: string;
  adresse: string;
  ville: string;
  prixMoyen: number | null;
  imageUrl: string;
  optionsAlimentaires: string[];
}

// Gestion du catalogue par l'admin : /api/admin/restaurants (reserve au role ADMIN par le module 1)
@Injectable({ providedIn: 'root' })
export class AdminRestaurantService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API_URL}/admin/restaurants`;

  lister(): Observable<Restaurant[]> {
    return this.http.get<Restaurant[]>(this.base);
  }

  creer(restaurant: RestaurantRequest): Observable<Restaurant> {
    return this.http.post<Restaurant>(this.base, restaurant);
  }

  modifier(id: number, restaurant: RestaurantRequest): Observable<Restaurant> {
    return this.http.put<Restaurant>(`${this.base}/${id}`, restaurant);
  }

  supprimer(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
