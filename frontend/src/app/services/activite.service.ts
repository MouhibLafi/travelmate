import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Activite } from '../models/activite.model';
import { Categorie } from '../models/categorie.model';
import { RecommandationDTO } from '../models/recommandation.model';
import { environment } from '../../environments/environment';

export interface ActiviteFiltres {
  destinationId?: number;
  categorie?: string;
  prixMin?: number;
  prixMax?: number;
  dureeMax?: number;
  q?: string;
  tri?: 'prix' | 'duree' | 'nom';
}

export interface RecommandationParams {
  interets: string[];
  destinationId?: number;
  budgetMax?: number;
  limite?: number;
}

@Injectable({
  providedIn: 'root'
})
export class ActiviteService {
  private readonly apiUrl = `${environment.apiUrl}/api/activites`;
  private readonly categoriesUrl = `${environment.apiUrl}/api/categories`;

  constructor(private http: HttpClient) {}

  getAll(filtres?: ActiviteFiltres): Observable<Activite[]> {
    let params = new HttpParams();
    if (filtres) {
      if (filtres.destinationId != null) {
        params = params.set('destinationId', String(filtres.destinationId));
      }
      if (filtres.categorie) {
        params = params.set('categorie', filtres.categorie);
      }
      if (filtres.prixMin != null) {
        params = params.set('prixMin', String(filtres.prixMin));
      }
      if (filtres.prixMax != null) {
        params = params.set('prixMax', String(filtres.prixMax));
      }
      if (filtres.dureeMax != null) {
        params = params.set('dureeMax', String(filtres.dureeMax));
      }
      if (filtres.q && filtres.q.trim()) {
        params = params.set('q', filtres.q.trim());
      }
      if (filtres.tri) {
        params = params.set('tri', filtres.tri);
      }
    }
    return this.http.get<Activite[]>(this.apiUrl, { params });
  }

  getById(id: number): Observable<Activite> {
    return this.http.get<Activite>(`${this.apiUrl}/${id}`);
  }

  getCategories(): Observable<Categorie[]> {
    return this.http.get<Categorie[]>(this.categoriesUrl);
  }

  getRecommandations(params: RecommandationParams): Observable<RecommandationDTO[]> {
    let httpParams = new HttpParams().set('interets', params.interets.join(','));
    if (params.destinationId != null) {
      httpParams = httpParams.set('destinationId', String(params.destinationId));
    }
    if (params.budgetMax != null) {
      httpParams = httpParams.set('budgetMax', String(params.budgetMax));
    }
    if (params.limite != null) {
      httpParams = httpParams.set('limite', String(params.limite));
    }
    return this.http.get<RecommandationDTO[]>(`${this.apiUrl}/recommandations`, { params: httpParams });
  }
}
