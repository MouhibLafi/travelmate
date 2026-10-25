import { Component, computed, input } from '@angular/core';
import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

import { AccommodationSummary } from '../../models/accommodation.models';
import { AMENITY_META, TYPE_META } from '../../utils/amenity.utils';
import { StarRatingComponent } from '../star-rating/star-rating.component';
import { ImageFallbackDirective } from '../image-fallback.directive';

@Component({
  selector: 'tm-accommodation-card',
  standalone: true,
  imports: [RouterLink, CurrencyPipe, DecimalPipe, MatIconModule, MatTooltipModule, StarRatingComponent, ImageFallbackDirective],
  templateUrl: './accommodation-card.component.html',
  styleUrl: './accommodation-card.component.scss',
})
export class AccommodationCardComponent {
  accommodation = input.required<AccommodationSummary>();
  /** Query params forwarded to the detail page (dates, guests). */
  linkParams = input<Record<string, string | number | null>>({});

  readonly amenityMeta = AMENITY_META;
  type = computed(() => TYPE_META[this.accommodation().type]);
  topAmenities = computed(() => this.accommodation().amenities.slice(0, 4));
  extraAmenities = computed(() => Math.max(0, this.accommodation().amenities.length - 4));
}
