import { Routes } from '@angular/router';
import { adminGuard, authGuard } from './guards/auth.guard';
import { AdminComponent } from './pages/admin/admin.component';
import { LoginComponent } from './pages/login/login.component';
import { PreferencesComponent } from './pages/preferences/preferences.component';
import { ProfileComponent } from './pages/profile/profile.component';
import { RegisterComponent } from './pages/register/register.component';
import { TripsComponent } from './trips/trips.component';
import { RESTAURANTS_ROUTES } from './restaurants/restaurants.routes';
import { AdminRestaurantsComponent } from './restaurants/admin-restaurants.component';

export const routes: Routes = [
  // ----- Module 1 : utilisateurs -----
  { path: 'login', component: LoginComponent },
  { path: 'inscription', component: RegisterComponent },
  { path: 'preferences', component: PreferencesComponent, canActivate: [authGuard] },
  { path: 'profil', component: ProfileComponent, canActivate: [authGuard] },
  { path: 'admin', component: AdminComponent, canActivate: [authGuard, adminGuard] },

  // ----- Module 2 : planification des voyages -----
  { path: 'voyages', component: TripsComponent, canActivate: [authGuard] },

  // ----- Module 5 : hebergements (pages chargees a la demande) -----
  {
    path: 'accommodations',
    canActivate: [authGuard],
    loadChildren: () => import('./features/accommodation/accommodation.routes').then(m => m.ACCOMMODATION_ROUTES),
  },

  // ----- Module 4 : restaurants (catalogue, recommandations, favoris, fiche) -----
  ...RESTAURANTS_ROUTES,
  // Page admin du module 4 : gestion du catalogue des restaurants
  { path: 'admin/restaurants', component: AdminRestaurantsComponent, canActivate: [authGuard, adminGuard] },

  // ----- Modules 3 et 6 : ajoutez vos routes ici, avec canActivate: [authGuard] -----

  { path: '', redirectTo: 'preferences', pathMatch: 'full' },
  { path: '**', redirectTo: 'preferences' },
];
