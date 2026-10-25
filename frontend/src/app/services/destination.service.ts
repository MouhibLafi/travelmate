import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Destination } from '../models/destination.model';
import { API_URL } from './api';   // integration : adresse du backend commun

@Injectable({
  providedIn: 'root'
})
export class DestinationService {
  private readonly apiUrl = `${API_URL}/destinations`;

  constructor(private http: HttpClient) {}

  getAll(q?: string): Observable<Destination[]> {
    let params = new HttpParams();
    if (q && q.trim()) {
      params = params.set('q', q.trim());
    }
    return this.http.get<Destination[]>(this.apiUrl, { params });
  }

  getById(id: number): Observable<Destination> {
    return this.http.get<Destination>(`${this.apiUrl}/${id}`);
  }
}
