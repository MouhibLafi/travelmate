import { Routes } from '@angular/router';
import { adminGuard, authGuard } from './guards/auth.guard';
import { AdminComponent } from './pages/admin/admin.component';
import { LoginComponent } from './pages/login/login.component';
import { PreferencesComponent } from './pages/preferences/preferences.component';
import { ProfileComponent } from './pages/profile/profile.component';
import { RegisterComponent } from './pages/register/register.component';
import { TripsComponent } from './trips/trips.component';

export const routes: Routes = [
  // ----- Module 1 : utilisateurs -----
  { path: 'login', component: LoginComponent },
  { path: 'inscription', component: RegisterComponent },
  { path: 'preferences', component: PreferencesComponent, canActivate: [authGuard] },
  { path: 'profil', component: ProfileComponent, canActivate: [authGuard] },
  { path: 'admin', component: AdminComponent, canActivate: [authGuard, adminGuard] },

  // ----- Module 2 : planification des voyages -----
  { path: 'voyages', component: TripsComponent, canActivate: [authGuard] },

  // ----- Modules 3 a 6 : ajoutez vos routes ici, avec canActivate: [authGuard] -----

  { path: '', redirectTo: 'preferences', pathMatch: 'full' },
  { path: '**', redirectTo: 'preferences' },
];
