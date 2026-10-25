import { Component, Input, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';

import { FavoriService } from './favori.service';
import { Restaurant } from './restaurant.model';

@Component({
  selector: 'tm-restaurant-card',
  imports: [RouterLink, DecimalPipe],
  templateUrl: './restaurant-card.component.html',
  styleUrls: ['./restaurant-card.component.css', './restaurants.css']
})
export class RestaurantCardComponent {
  @Input({ required: true }) restaurant!: Restaurant;

  protected readonly favoris = inject(FavoriService);

  basculerFavori(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.favoris.basculer(this.restaurant.id);
  }
}
