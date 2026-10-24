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
  selector: 'tm-login',
  imports: [FormsModule, RouterLink, MatFormFieldModule, MatInputModule, MatButtonModule, IconComponent],
  templateUrl: './login.component.html',
})
export class LoginComponent {
  email = '';
  password = '';
  errorMessage = '';
  loading = false;

  constructor(private authService: AuthService, private router: Router) {}

  login(): void {
    this.errorMessage = '';
    this.loading = true;

    this.authService.login(this.email, this.password).subscribe({
      next: () => this.router.navigate(['/preferences']),
      error: (error) => {
        this.errorMessage = getErrorMessage(error);
        this.loading = false;
      },
    });
  }
}
