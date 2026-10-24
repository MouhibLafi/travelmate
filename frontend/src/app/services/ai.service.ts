import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AiResponse } from '../models/user.model';
import { API_URL } from './api';

// Tache IA : le backend envoie la phrase a llama3 (Ollama) et renvoie la suggestion
@Injectable({ providedIn: 'root' })
export class AiService {

  constructor(private http: HttpClient) {}

  analyzeProfile(description: string): Observable<AiResponse> {
    return this.http.post<AiResponse>(`${API_URL}/ai/analyze-profile`, { description });
  }
}
