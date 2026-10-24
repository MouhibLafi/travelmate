import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../services/api';
import { Trip, TripRequest, TripSuggestion } from './trip.model';

// Appels a /api/trips.
// Pas besoin d'ajouter le jeton : l'intercepteur du module 1 le fait pour toutes les requetes.
@Injectable({ providedIn: 'root' })
export class TripService {

  constructor(private http: HttpClient) {}

  getMyTrips(): Observable<Trip[]> {
    return this.http.get<Trip[]>(`${API_URL}/trips`);
  }

  create(trip: TripRequest): Observable<Trip> {
    return this.http.post<Trip>(`${API_URL}/trips`, trip);
  }

  update(id: number, trip: TripRequest): Observable<Trip> {
    return this.http.put<Trip>(`${API_URL}/trips/${id}`, trip);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${API_URL}/trips/${id}`);
  }

  // Tache IA : "5 jours à Rome, 800 €" -> formulaire pre-rempli (rien n'est enregistre)
  fillForm(text: string): Observable<TripSuggestion> {
    return this.http.post<TripSuggestion>(`${API_URL}/trips/ai/fill-form`, { text });
  }
}
