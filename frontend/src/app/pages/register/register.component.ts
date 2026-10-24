import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';
import { IconComponent } from '../../components/icon/icon.component';
import { getErrorMessage } from '../../services/api';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'tm-register',
  imports: [FormsModule, RouterLink, MatFormFieldModule, MatInputModule, MatButtonModule, IconComponent],
  templateUrl: './register.component.html',
})
export class RegisterComponent {
  // Les champs du formulaire
  user = { firstName: '', lastName: '', email: '', phone: '', password: '' };
  confirmPassword = '';

  errorMessage = '';
  loading = false;

  constructor(private authService: AuthService, private router: Router) {}

  register(): void {
    this.errorMessage = '';

    if (this.user.password !== this.confirmPassword) {
      this.errorMessage = 'Les mots de passe ne correspondent pas.';
      return;
    }

    this.loading = true;
    this.authService.register(this.user).subscribe({
      // Apres l'inscription, on envoie l'utilisateur sur ses preferences pour essayer l'IA
      next: () => this.router.navigate(['/preferences']),
      error: (error) => {
        this.errorMessage = getErrorMessage(error);
        this.loading = false;
      },
    });
  }
}
