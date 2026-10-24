import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

// Page accessible seulement si on est connecte
export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  if (authService.isLoggedIn()) {
    return true;
  }
  return inject(Router).createUrlTree(['/login']);
};

// Page accessible seulement aux administrateurs
export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  if (authService.isAdmin()) {
    return true;
  }
  return inject(Router).createUrlTree(['/preferences']);
};
