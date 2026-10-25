import { Routes } from '@angular/router';

export const ACCOMMODATION_ROUTES: Routes = [
  {
    path: '',
    title: 'Stays · TravelMate',
    loadComponent: () => import('./pages/accommodation-list/accommodation-list.component').then(m => m.AccommodationListComponent),
  },
  {
    path: 'admin',
    title: 'Manage accommodations · TravelMate',
    loadComponent: () => import('./pages/admin/accommodation-admin/accommodation-admin.component').then(m => m.AccommodationAdminComponent),
  },
  {
    path: 'admin/new',
    title: 'New accommodation · TravelMate',
    loadComponent: () => import('./pages/admin/accommodation-form/accommodation-form.component').then(m => m.AccommodationFormComponent),
  },
  {
    path: 'admin/:id/edit',
    title: 'Edit accommodation · TravelMate',
    loadComponent: () => import('./pages/admin/accommodation-form/accommodation-form.component').then(m => m.AccommodationFormComponent),
  },
  {
    path: ':id',
    title: 'Stay details · TravelMate',
    loadComponent: () => import('./pages/accommodation-detail/accommodation-detail.component').then(m => m.AccommodationDetailComponent),
  },
];
