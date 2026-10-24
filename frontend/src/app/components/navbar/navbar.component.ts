import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { IconComponent } from '../icon/icon.component';

@Component({
  selector: 'tm-navbar',
  imports: [RouterLink, RouterLinkActive, IconComponent],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css',
})
export class NavbarComponent {

  constructor(public authService: AuthService) {}

  // Initiales pour l'avatar, ex : "Mouhib Lafi" -> "ML"
  getInitials(): string {
    const user = this.authService.getUser();
    return user ? (user.firstName[0] + user.lastName[0]).toUpperCase() : '';
  }
}
