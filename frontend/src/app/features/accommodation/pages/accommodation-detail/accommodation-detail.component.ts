import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, of, switchMap, tap } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

import { AccommodationService } from '../../services/accommodation.service';
import { NotificationService } from '../../services/notification.service';
import { Accommodation } from '../../models/accommodation.models';
import { AMENITY_META, TYPE_META } from '../../utils/amenity.utils';
import { fromIsoDate, toIsoDate } from '../../utils/date.utils';
import { StarRatingComponent } from '../../components/star-rating/star-rating.component';
import { ImageFallbackDirective } from '../../components/image-fallback.directive';
import { AvailabilityCalendarComponent } from '../../components/availability-calendar/availability-calendar.component';
import { StayCostCalculatorComponent } from '../../components/stay-cost-calculator/stay-cost-calculator.component';
import { AiPricePanelComponent } from '../../components/ai-price-panel/ai-price-panel.component';

@Component({
  selector: 'tm-accommodation-detail',
  standalone: true,
  imports: [DecimalPipe, RouterLink, MatButtonModule, MatIconModule, MatTooltipModule, StarRatingComponent,
    ImageFallbackDirective, AvailabilityCalendarComponent, StayCostCalculatorComponent, AiPricePanelComponent],
  templateUrl: './accommodation-detail.component.html',
  styleUrl: './accommodation-detail.component.scss',
})
export class AccommodationDetailComponent {
  private readonly api = inject(AccommodationService);
  private readonly notify = inject(NotificationService);

  // Route param + optional query params (bound by withComponentInputBinding)
  id = input.required<string>();
  checkInParam = input<string | undefined>(undefined, { alias: 'checkIn' });
  checkOutParam = input<string | undefined>(undefined, { alias: 'checkOut' });
  guestsParam = input<string | undefined>(undefined, { alias: 'guests' });

  readonly amenityMeta = AMENITY_META;
  readonly typeMeta = TYPE_META;

  readonly accommodation = signal<Accommodation | null>(null);
  readonly bookedDates = signal<string[]>([]);
  readonly loading = signal(true);
  readonly notFound = signal(false);
  readonly activeImage = signal(0);

  // Stay shared by the calendar, the calculator and the AI panel
  readonly checkIn = signal<Date | null>(null);
  readonly checkOut = signal<Date | null>(null);
  readonly guests = signal(2);

  readonly checkInIso = computed(() => (this.checkIn() && this.checkOut() ? toIsoDate(this.checkIn()) : null));
  readonly checkOutIso = computed(() => (this.checkIn() && this.checkOut() ? toIsoDate(this.checkOut()) : null));
  readonly images = computed(() => this.accommodation()?.imageUrls ?? []);
  readonly mainImage = computed(() => this.images()[this.activeImage()] ?? null);

  constructor() {
    toObservable(this.id).pipe(
      tap(() => { this.loading.set(true); this.notFound.set(false); this.activeImage.set(0); }),
      switchMap(id => forkJoin({
        accommodation: this.api.getById(+id),
        booked: this.api.bookedDates(+id).pipe(catchError(() => of(null))),
      }).pipe(catchError(err => {
        if (err?.status === 404) this.notFound.set(true);
        else this.notify.error(err, 'Could not load this stay');
        return of(null);
      }))),
      takeUntilDestroyed(),
    ).subscribe(result => {
      this.loading.set(false);
      if (!result) return;
      this.accommodation.set(result.accommodation);
      this.bookedDates.set(result.booked?.bookedDates ?? []);
      this.guests.update(g => Math.min(g, result.accommodation.maxGuests));
    });

    // Pre-fill the stay from the search page
    effect(() => {
      const checkIn = fromIsoDate(this.checkInParam());
      const checkOut = fromIsoDate(this.checkOutParam());
      const guests = Number(this.guestsParam());
      untracked(() => {
        if (checkIn && checkOut && checkOut > checkIn) { this.checkIn.set(checkIn); this.checkOut.set(checkOut); }
        if (guests > 0) this.guests.set(guests);
      });
    });
  }

  selectImage(index: number): void {
    this.activeImage.set(index);
  }

  nextImage(step: number): void {
    const count = this.images().length;
    if (count) this.activeImage.set((this.activeImage() + step + count) % count);
  }
}
