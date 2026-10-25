import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../services/api';
import {
  BudgetSummary, CatalogItem, Expense, ExpenseRequest, GenerationResult, ItemRequest, ItemType, Itinerary,
  OptimizationResult,
} from './itinerary.model';

// Appels a /api/itinerary. Les voyages viennent du module 2 (TripService).
// Pas besoin d'ajouter le jeton : l'intercepteur du module 1 le fait pour toutes les requetes.
@Injectable({ providedIn: 'root' })
export class ItineraryService {

  private api = `${API_URL}/itinerary`;

  constructor(private http: HttpClient) {}

  getMyItineraries(): Observable<Itinerary[]> {
    return this.http.get<Itinerary[]>(this.api);
  }

  getById(id: number): Observable<Itinerary> {
    return this.http.get<Itinerary>(`${this.api}/${id}`);
  }

  // Tache IA : genere (ou regenere) l'itineraire, et ajuste le budget automatiquement s'il est depasse
  generate(tripId: number): Observable<GenerationResult> {
    return this.http.post<GenerationResult>(`${this.api}/generate`, { tripId });
  }

  // Tache IA : remplace des elements par des alternatives moins cheres
  optimize(id: number): Observable<OptimizationResult> {
    return this.http.post<OptimizationResult>(`${this.api}/${id}/optimize`, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }

  getCatalog(id: number, type: ItemType): Observable<CatalogItem[]> {
    return this.http.get<CatalogItem[]>(`${this.api}/${id}/catalog`, { params: { type } });
  }

  addItem(id: number, dayId: number, item: ItemRequest): Observable<Itinerary> {
    return this.http.post<Itinerary>(`${this.api}/${id}/days/${dayId}/items`, item);
  }

  updateItem(itemId: number, item: ItemRequest): Observable<Itinerary> {
    return this.http.put<Itinerary>(`${this.api}/items/${itemId}`, item);
  }

  deleteItem(itemId: number): Observable<Itinerary> {
    return this.http.delete<Itinerary>(`${this.api}/items/${itemId}`);
  }

  getBudget(id: number): Observable<BudgetSummary> {
    return this.http.get<BudgetSummary>(`${this.api}/${id}/budget`);
  }

  getExpenses(id: number): Observable<Expense[]> {
    return this.http.get<Expense[]>(`${this.api}/${id}/expenses`);
  }

  addExpense(id: number, expense: ExpenseRequest): Observable<Expense> {
    return this.http.post<Expense>(`${this.api}/${id}/expenses`, expense);
  }

  deleteExpense(expenseId: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/expenses/${expenseId}`);
  }

  downloadPdf(id: number): Observable<Blob> {
    return this.http.get(`${this.api}/${id}/pdf`, { responseType: 'blob' });
  }
}
