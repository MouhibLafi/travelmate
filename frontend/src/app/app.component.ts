import { Component } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { NavbarComponent } from './components/navbar/navbar.component';

@Component({
  selector: 'tm-root',
  imports: [RouterOutlet, NavbarComponent],
  template: `
    <!-- Pas de barre de navigation sur les pages de connexion et d'inscription -->
    @if (showNavbar()) {
      <tm-navbar></tm-navbar>
    }
    <router-outlet></router-outlet>
  `,
})
export class AppComponent {

  constructor(private router: Router) {}

  showNavbar(): boolean {
    return !this.router.url.startsWith('/login') && !this.router.url.startsWith('/inscription');
  }
}
