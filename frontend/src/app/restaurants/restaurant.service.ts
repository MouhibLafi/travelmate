import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../services/api';
import {
  AnalyseAvis,
  Avis,
  AvisRequest,
  FiltresRecommandation,
  FiltresRestaurants,
  MetaFiltres,
  Recommandation,
  Restaurant
} from './restaurant.model';

// Catalogue, avis et recommandations du module 4 (routes /api/restaurants).
@Injectable({ providedIn: 'root' })
export class RestaurantService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API_URL}/restaurants`;

  rechercher(filtres: FiltresRestaurants): Observable<Restaurant[]> {
    let params = new HttpParams();
    if (filtres.q) {
      params = params.set('q', filtres.q);
    }
    if (filtres.cuisine) {
      params = params.set('cuisine', filtres.cuisine);
    }
    if (filtres.ville) {
      params = params.set('ville', filtres.ville);
    }
    if (filtres.prixMin != null) {
      params = params.set('prixMin', filtres.prixMin);
    }
    if (filtres.prixMax != null) {
      params = params.set('prixMax', filtres.prixMax);
    }
    if (filtres.noteMin != null) {
      params = params.set('noteMin', filtres.noteMin);
    }
    (filtres.options ?? []).forEach((option) => {
      params = params.append('options', option);
    });
    return this.http.get<Restaurant[]>(this.base, { params });
  }

  detail(id: number): Observable<Restaurant> {
    return this.http.get<Restaurant>(`${this.base}/${id}`);
  }

  avis(id: number): Observable<Avis[]> {
    return this.http.get<Avis[]>(`${this.base}/${id}/avis`);
  }

  ajouterAvis(id: number, avis: AvisRequest): Observable<Avis> {
    return this.http.post<Avis>(`${this.base}/${id}/avis`, avis);
  }

  analyse(id: number): Observable<AnalyseAvis> {
    return this.http.get<AnalyseAvis>(`${this.base}/${id}/analyse-avis`);
  }

  filtres(): Observable<MetaFiltres> {
    return this.http.get<MetaFiltres>(`${this.base}/filtres`);
  }

  recommandations(filtres: FiltresRecommandation): Observable<Recommandation[]> {
    let params = new HttpParams();
    if (filtres.budgetMax != null) {
      params = params.set('budgetMax', filtres.budgetMax);
    }
    if (filtres.cuisine) {
      params = params.set('cuisine', filtres.cuisine);
    }
    if (filtres.ville) {
      params = params.set('ville', filtres.ville);
    }
    if (filtres.limite != null) {
      params = params.set('limite', filtres.limite);
    }
    (filtres.options ?? []).forEach((option) => {
      params = params.append('options', option);
    });
    return this.http.get<Recommandation[]>(`${this.base}/recommandations`, { params });
  }
}
