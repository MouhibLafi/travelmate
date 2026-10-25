import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../services/api';
import { Reanalyse, StatutIa } from './restaurant.model';

// Statut d'Ollama et reanalyse des avis (routes /api/restaurants/ia).
@Injectable({ providedIn: 'root' })
export class IaService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API_URL}/restaurants/ia`;

  readonly statut = signal<StatutIa | null>(null);

  constructor() {
    this.chargerStatut();
  }

  chargerStatut(): void {
    this.http.get<StatutIa>(`${this.base}/statut`).subscribe({
      next: (statut) => this.statut.set(statut),
      error: () =>
        this.statut.set({
          disponible: false,
          modele: 'llama3',
          url: 'http://localhost:11434',
          message: "Le backend ne repond pas : impossible de verifier Ollama."
        })
    });
  }

  reanalyser(): Observable<Reanalyse> {
    return this.http.post<Reanalyse>(`${this.base}/reanalyser-avis`, null);
  }
}
