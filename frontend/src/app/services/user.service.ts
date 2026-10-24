import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Preferences, User } from '../models/user.model';
import { API_URL } from './api';

// Profil et preferences de l'utilisateur connecte (/api/users/me)
@Injectable({ providedIn: 'root' })
export class UserService {

  constructor(private http: HttpClient) {}

  getMe(): Observable<User> {
    return this.http.get<User>(`${API_URL}/users/me`);
  }

  updateMe(data: { firstName: string; lastName: string; phone: string | null }): Observable<User> {
    return this.http.put<User>(`${API_URL}/users/me`, data);
  }

  changePassword(currentPassword: string, newPassword: string): Observable<void> {
    return this.http.put<void>(`${API_URL}/users/me/password`, { currentPassword, newPassword });
  }

  deleteMe(): Observable<void> {
    return this.http.delete<void>(`${API_URL}/users/me`);
  }

  getPreferences(): Observable<Preferences> {
    return this.http.get<Preferences>(`${API_URL}/users/me/preferences`);
  }

  updatePreferences(preferences: Preferences): Observable<Preferences> {
    return this.http.put<Preferences>(`${API_URL}/users/me/preferences`, preferences);
  }
}
