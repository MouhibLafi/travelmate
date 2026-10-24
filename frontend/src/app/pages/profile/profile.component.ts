import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { IconComponent } from '../../components/icon/icon.component';
import { User } from '../../models/user.model';
import { getErrorMessage } from '../../services/api';
import { AuthService } from '../../services/auth.service';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'tm-profile',
  imports: [FormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, IconComponent],
  templateUrl: './profile.component.html',
})
export class ProfileComponent implements OnInit {
  user: User | null = null;

  // Formulaire "informations"
  firstName = '';
  lastName = '';
  phone = '';
  profileMessage = '';
  profileError = '';

  // Formulaire "mot de passe"
  currentPassword = '';
  newPassword = '';
  passwordMessage = '';
  passwordError = '';

  constructor(private userService: UserService, private authService: AuthService) {}

  ngOnInit(): void {
    this.userService.getMe().subscribe({
      next: (user) => {
        this.user = user;
        this.firstName = user.firstName;
        this.lastName = user.lastName;
        this.phone = user.phone ?? '';
      },
      error: (error) => (this.profileError = getErrorMessage(error)),
    });
  }

  saveProfile(): void {
    this.profileMessage = '';
    this.profileError = '';

    this.userService.updateMe({ firstName: this.firstName, lastName: this.lastName, phone: this.phone || null })
      .subscribe({
        next: (user) => {
          this.user = user;
          this.authService.setUser(user); // met a jour le nom dans la barre de navigation
          this.profileMessage = 'Profil mis à jour.';
        },
        error: (error) => (this.profileError = getErrorMessage(error)),
      });
  }

  changePassword(): void {
    this.passwordMessage = '';
    this.passwordError = '';

    this.userService.changePassword(this.currentPassword, this.newPassword).subscribe({
      next: () => {
        this.passwordMessage = 'Mot de passe modifié.';
        this.currentPassword = '';
        this.newPassword = '';
      },
      error: (error) => (this.passwordError = getErrorMessage(error)),
    });
  }

  deleteAccount(): void {
    if (!confirm('Supprimer définitivement votre compte ? Cette action est irréversible.')) {
      return;
    }
    this.userService.deleteMe().subscribe({
      next: () => this.authService.logout(),
      error: (error) => (this.profileError = getErrorMessage(error)),
    });
  }
}
