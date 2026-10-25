import { Routes } from '@angular/router';
import { DateAdapter, MAT_DATE_LOCALE, provideNativeDateAdapter } from '@angular/material/core';
import { adminGuard } from '../../guards/auth.guard';
import { DayFirstDateAdapter } from './utils/day-first-date-adapter';

export const ACCOMMODATION_ROUTES: Routes = [
  {
    path: '',
    // Calendriers du module : dates saisies au format jour/mois/annee
    providers: [
      provideNativeDateAdapter(),                               // formats de date du calendrier
      { provide: MAT_DATE_LOCALE, useValue: 'fr-FR' },
      { provide: DateAdapter, useClass: DayFirstDateAdapter },  // remplace l'adaptateur natif
    ],
    children: [
      {
        path: '',
        title: 'Stays · TravelMate',
        loadComponent: () => import('./pages/accommodation-list/accommodation-list.component').then(m => m.AccommodationListComponent),
      },
      // Gestion du catalogue : reservee aux administrateurs (module 1)
      {
        path: 'admin',
        title: 'Manage accommodations · TravelMate',
        canActivate: [adminGuard],
        loadComponent: () => import('./pages/admin/accommodation-admin/accommodation-admin.component').then(m => m.AccommodationAdminComponent),
      },
      {
        path: 'admin/new',
        title: 'New accommodation · TravelMate',
        canActivate: [adminGuard],
        loadComponent: () => import('./pages/admin/accommodation-form/accommodation-form.component').then(m => m.AccommodationFormComponent),
      },
      {
        path: 'admin/:id/edit',
        title: 'Edit accommodation · TravelMate',
        canActivate: [adminGuard],
        loadComponent: () => import('./pages/admin/accommodation-form/accommodation-form.component').then(m => m.AccommodationFormComponent),
      },
      {
        path: ':id',
        title: 'Stay details · TravelMate',
        loadComponent: () => import('./pages/accommodation-detail/accommodation-detail.component').then(m => m.AccommodationDetailComponent),
      },
    ],
  },
];
