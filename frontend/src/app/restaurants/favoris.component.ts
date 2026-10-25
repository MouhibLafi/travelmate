import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';

import { getErrorMessage } from '../services/api';
import { FavoriService } from './favori.service';
import { Restaurant } from './restaurant.model';
import { RestaurantCardComponent } from './restaurant-card.component';

@Component({
  selector: 'tm-favoris',
  imports: [RouterLink, RestaurantCardComponent],
  templateUrl: './favoris.component.html',
  styleUrls: ['./favoris.component.css', './restaurants.css']
})
export class FavorisComponent {
  private readonly favoriService = inject(FavoriService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly favoris = this.favoriService;
  protected readonly restaurants = signal<Restaurant[]>([]);
  protected readonly chargement = signal(true);
  protected readonly erreur = signal('');

  protected readonly visibles = computed(() => {
    const ids = this.favoris.ids();
    return this.restaurants().filter((restaurant) => ids.includes(restaurant.id));
  });

  constructor() {
    this.charger();
  }

  protected charger(): void {
    this.chargement.set(true);
    this.erreur.set('');

    this.favoriService
      .lister()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (restaurants) => {
          this.restaurants.set(restaurants);
          this.chargement.set(false);
        },
        error: (erreur: HttpErrorResponse) => {
          this.restaurants.set([]);
          this.erreur.set(getErrorMessage(erreur));
          this.chargement.set(false);
        }
      });
  }
}
