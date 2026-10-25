import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { IconComponent } from '../../components/icon/icon.component';
import { Role, User } from '../../models/user.model';
import { AdminService } from '../../services/admin.service';
import { getErrorMessage } from '../../services/api';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'tm-admin',
  imports: [FormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, IconComponent, RouterLink],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.css',
})
export class AdminComponent implements OnInit {
  users: User[] = [];
  search = '';
  errorMessage = '';

  constructor(private adminService: AdminService, private authService: AuthService) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.adminService.getUsers(this.search).subscribe({
      next: (users) => (this.users = users),
      error: (error) => (this.errorMessage = getErrorMessage(error)),
    });
  }

  // L'admin ne peut pas se modifier lui-meme (le backend le verifie aussi)
  isMe(user: User): boolean {
    return user.email === this.authService.getUser()?.email;
  }

  changeRole(user: User, role: Role): void {
    this.adminService.changeRole(user.id, role).subscribe({
      next: () => this.loadUsers(),
      error: (error) => (this.errorMessage = getErrorMessage(error)),
    });
  }

  toggleStatus(user: User): void {
    this.adminService.changeStatus(user.id, !user.active).subscribe({
      next: () => this.loadUsers(),
      error: (error) => (this.errorMessage = getErrorMessage(error)),
    });
  }

  deleteUser(user: User): void {
    if (!confirm(`Supprimer ${user.firstName} ${user.lastName} ?`)) {
      return;
    }
    this.adminService.deleteUser(user.id).subscribe({
      next: () => this.loadUsers(),
      error: (error) => (this.errorMessage = getErrorMessage(error)),
    });
  }

  // Petites statistiques en haut de la page
  countByRole(role: Role): number {
    return this.users.filter((u) => u.role === role).length;
  }

  countInactive(): number {
    return this.users.filter((u) => !u.active).length;
  }
}
