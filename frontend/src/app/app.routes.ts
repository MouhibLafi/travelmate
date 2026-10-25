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
import { DecouvrirLayoutComponent } from './decouvrir/decouvrir-layout.component';
import { ItinerariesComponent } from './itinerary/itineraries.component';
import { ItineraryDetailComponent } from './itinerary/itinerary-detail.component';

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

  // ----- Module 3 : destinations et activites (Fatma) -----
  // Composant parent sans adresse : garde les adresses du module (/destinations, /activites...)
  // et ajoute un sous-menu commun a ses pages.
  {
    path: '',
    component: DecouvrirLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: 'destinations', loadComponent: () => import('./destinations/destinations.component').then(m => m.DestinationsComponent) },
      { path: 'destinations/:id', loadComponent: () => import('./destinations/destination-detail/destination-detail.component').then(m => m.DestinationDetailComponent) },
      { path: 'activites', loadComponent: () => import('./activites/activites.component').then(m => m.ActivitesComponent) },
      { path: 'activites/:id', loadComponent: () => import('./activites/activite-detail/activite-detail.component').then(m => m.ActiviteDetailComponent) },
      { path: 'recommandations', loadComponent: () => import('./recommandations/recommandations.component').then(m => m.RecommandationsComponent) },
      { path: 'favoris', loadComponent: () => import('./favoris/favoris.component').then(m => m.FavorisComponent) },
    ],
  },

  // ----- Module 6 : itineraire et budget (Badiss) -----
  { path: 'itineraires', component: ItinerariesComponent, canActivate: [authGuard] },
  { path: 'itineraires/:id', component: ItineraryDetailComponent, canActivate: [authGuard] },

  { path: '', redirectTo: 'preferences', pathMatch: 'full' },
  { path: '**', redirectTo: 'preferences' },
];
