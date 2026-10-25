import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../services/api';
import { Restaurant } from './restaurant.model';

// Favoris de l'utilisateur connecte (le backend lit son email dans le JWT).
@Injectable({ providedIn: 'root' })
export class FavoriService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API_URL}/restaurants/favoris`;

  /** Identifiants des restaurants favoris, partages entre toutes les pages. */
  readonly ids = signal<number[]>([]);
  readonly nombre = computed(() => this.ids().length);

  constructor() {
    this.chargerIds();
  }

  private chargerIds(): void {
    this.http.get<number[]>(`${this.base}/ids`).subscribe({
      next: (ids) => this.ids.set(ids),
      error: () => this.ids.set([])
    });
  }

  lister(): Observable<Restaurant[]> {
    return this.http.get<Restaurant[]>(this.base);
  }

  estFavori(restaurantId: number): boolean {
    return this.ids().includes(restaurantId);
  }

  basculer(restaurantId: number): void {
    if (this.estFavori(restaurantId)) {
      this.http.delete<void>(`${this.base}/${restaurantId}`).subscribe({
        next: () => this.ids.update((ids) => ids.filter((id) => id !== restaurantId))
      });
      return;
    }

    this.http.post<Restaurant>(`${this.base}/${restaurantId}`, null).subscribe({
      next: () => this.ids.update((ids) => [...ids, restaurantId])
    });
  }
}
