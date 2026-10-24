import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { AuthResponse, User } from '../models/user.model';
import { API_URL } from './api';

// Gere la connexion : on garde le jeton JWT et l'utilisateur dans le localStorage.
@Injectable({ providedIn: 'root' })
export class AuthService {

  constructor(private http: HttpClient, private router: Router) {}

  register(data: { firstName: string; lastName: string; email: string; password: string; phone: string }): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API_URL}/auth/register`, data)
      .pipe(tap(response => this.saveSession(response)));
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API_URL}/auth/login`, { email, password })
      .pipe(tap(response => this.saveSession(response)));
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  getUser(): User | null {
    const json = localStorage.getItem('user');
    return json ? JSON.parse(json) : null;
  }

  // Utilise apres la modification du profil
  setUser(user: User): void {
    localStorage.setItem('user', JSON.stringify(user));
  }

  isLoggedIn(): boolean {
    return this.getToken() !== null;
  }

  isAdmin(): boolean {
    return this.getUser()?.role === 'ADMIN';
  }

  private saveSession(response: AuthResponse): void {
    localStorage.setItem('token', response.token);
    this.setUser(response.user);
  }
}
