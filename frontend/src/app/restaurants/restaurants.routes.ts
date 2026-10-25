import { Routes } from '@angular/router';
import { authGuard } from '../guards/auth.guard';
import { CatalogueComponent } from './catalogue.component';
import { DetailComponent } from './detail.component';
import { FavorisComponent } from './favoris.component';
import { RecommandationsComponent } from './recommandations.component';

/*
 * Routes du module 4.
 * A brancher dans app.routes.ts (les routes litterales doivent passer avant :id) :
 *   import { RESTAURANTS_ROUTES } from './restaurants/restaurants.routes';
 *   ...RESTAURANTS_ROUTES,
 */
export const RESTAURANTS_ROUTES: Routes = [
  { path: 'restaurants', component: CatalogueComponent, canActivate: [authGuard] },
  { path: 'restaurants/recommandations', component: RecommandationsComponent, canActivate: [authGuard] },
  { path: 'restaurants/favoris', component: FavorisComponent, canActivate: [authGuard] },
  { path: 'restaurants/:id', component: DetailComponent, canActivate: [authGuard] }
];
