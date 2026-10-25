import { Component, effect, input, output, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatSliderModule } from '@angular/material/slider';
import { MatChipsModule } from '@angular/material/chips';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';

import { ACCOMMODATION_TYPES, AMENITIES, AccommodationType, Amenity } from '../../models/accommodation.models';
import { AMENITY_META, TYPE_META } from '../../utils/amenity.utils';

export interface FiltersValue {
  minPrice: number | null;
  maxPrice: number | null;
  type: AccommodationType | null;
  minStars: number | null;
  minRating: number | null;
  amenities: Amenity[];
}

export const EMPTY_FILTERS: FiltersValue = { minPrice: null, maxPrice: null, type: null, minStars: null, minRating: null, amenities: [] };
export const PRICE_SLIDER_MAX = 700;

@Component({
  selector: 'tm-filters-panel',
  standalone: true,
  imports: [FormsModule, CurrencyPipe, MatSliderModule, MatChipsModule, MatCheckboxModule, MatButtonModule, MatIconModule,
    MatSelectModule, MatFormFieldModule],
  templateUrl: './filters-panel.component.html',
  styleUrl: './filters-panel.component.scss',
})
export class FiltersPanelComponent {
  value = input<FiltersValue>(EMPTY_FILTERS);
  filtersChange = output<FiltersValue>();

  readonly types = ACCOMMODATION_TYPES;
  readonly typeMeta = TYPE_META;
  readonly amenities = AMENITIES;
  readonly amenityMeta = AMENITY_META;
  readonly priceMax = PRICE_SLIDER_MAX;
  readonly starOptions = [2, 3, 4, 5];
  readonly ratingOptions = [3.5, 4, 4.5];

  // Slider thumbs are bound locally and only emitted on release.
  readonly priceLow = signal(0);
  readonly priceHigh = signal(PRICE_SLIDER_MAX);

  constructor() {
    effect(() => {
      const v = this.value();
      this.priceLow.set(v.minPrice ?? 0);
      this.priceHigh.set(v.maxPrice ?? PRICE_SLIDER_MAX);
    });   // Angular 20 : l'option allowSignalWrites n'est plus necessaire
  }

  activeCount(): number {
    const v = this.value();
    return [v.minPrice, v.maxPrice, v.type, v.minStars, v.minRating].filter(x => x !== null).length + v.amenities.length;
  }

  commitPrice(): void {
    const low = this.priceLow();
    const high = this.priceHigh();
    this.patch({ minPrice: low > 0 ? low : null, maxPrice: high < PRICE_SLIDER_MAX ? high : null });
  }

  setType(type: AccommodationType | null): void {
    this.patch({ type });
  }

  setStars(stars: number | null): void {
    this.patch({ minStars: stars });
  }

  setRating(rating: number | null): void {
    this.patch({ minRating: rating });
  }

  toggleAmenity(amenity: Amenity, checked: boolean): void {
    const current = this.value().amenities.filter(a => a !== amenity);
    this.patch({ amenities: checked ? [...current, amenity] : current });
  }

  reset(): void {
    this.filtersChange.emit({ ...EMPTY_FILTERS });
  }

  private patch(change: Partial<FiltersValue>): void {
    this.filtersChange.emit({ ...this.value(), ...change });
  }
}
