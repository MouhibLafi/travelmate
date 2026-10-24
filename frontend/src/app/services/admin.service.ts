import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Role, User } from '../models/user.model';
import { API_URL } from './api';

// Administration des utilisateurs (/api/admin/users), reserve au role ADMIN
@Injectable({ providedIn: 'root' })
export class AdminService {

  constructor(private http: HttpClient) {}

  getUsers(search: string): Observable<User[]> {
    return this.http.get<User[]>(`${API_URL}/admin/users`, { params: { search } });
  }

  changeRole(id: number, role: Role): Observable<User> {
    return this.http.put<User>(`${API_URL}/admin/users/${id}/role`, { role });
  }

  changeStatus(id: number, active: boolean): Observable<User> {
    return this.http.put<User>(`${API_URL}/admin/users/${id}/status`, { active });
  }

  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${API_URL}/admin/users/${id}`);
  }
}
