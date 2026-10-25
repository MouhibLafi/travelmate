import { Component, computed, inject, input, model, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { CurrencyPipe, PercentPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { catchError, debounceTime, distinctUntilChanged, map, of, switchMap, tap } from 'rxjs';
import { MatDatepickerModule, MatCalendarCellClassFunction } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { AccommodationService } from '../../services/accommodation.service';
import { errorMessage } from '../../services/notification.service';
import { StayCostResponse } from '../../models/accommodation.models';
import { nightsBetween, toIsoDate, today } from '../../utils/date.utils';

@Component({
  selector: 'tm-stay-cost-calculator',
  standalone: true,
  imports: [CurrencyPipe, PercentPipe, FormsModule, MatDatepickerModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatProgressBarModule],
  templateUrl: './stay-cost-calculator.component.html',
  styleUrl: './stay-cost-calculator.component.scss',
})
export class StayCostCalculatorComponent {
  private readonly api = inject(AccommodationService);

  accommodationId = input.required<number>();
  pricePerNight = input.required<number>();
  maxGuests = input.required<number>();
  bookedDates = input<string[]>([]);
  checkIn = model<Date | null>(null);
  checkOut = model<Date | null>(null);
  guests = model<number>(2);

  readonly minDate = today();
  readonly loading = signal(false);
  readonly cost = signal<StayCostResponse | null>(null);
  readonly error = signal<string | null>(null);

  readonly nights = computed(() => nightsBetween(this.checkIn(), this.checkOut()));
  private readonly booked = computed(() => new Set(this.bookedDates()));
  readonly dateClass: MatCalendarCellClassFunction<Date> = (date, view) =>
    view === 'month' && this.booked().has(toIsoDate(date)!) ? 'tm-booked' : '';

  private readonly request = computed(() => {
    const checkIn = this.checkIn();
    const checkOut = this.checkOut();
    if (!checkIn || !checkOut || checkOut <= checkIn) return null;
    return { id: this.accommodationId(), checkIn: toIsoDate(checkIn)!, checkOut: toIsoDate(checkOut)!, guests: this.guests() };
  });

  constructor() {
    toObservable(this.request).pipe(
      map(r => (r ? JSON.stringify(r) : null)),
      distinctUntilChanged(),
      tap(key => { if (!key) { this.cost.set(null); this.error.set(null); } }),
      debounceTime(250),
      switchMap(key => {
        if (!key) return of(null);
        const r = JSON.parse(key);
        this.loading.set(true);
        return this.api.stayCost(r.id, { checkIn: r.checkIn, checkOut: r.checkOut, guests: r.guests }).pipe(
          tap(() => this.error.set(null)),
          catchError(err => {
            this.error.set(errorMessage(err, 'Could not compute the stay cost'));
            return of(null);
          }),
        );
      }),
      takeUntilDestroyed(),
    ).subscribe(cost => {
      this.cost.set(cost);
      this.loading.set(false);
    });
  }

  changeGuests(delta: number): void {
    this.guests.set(Math.min(this.maxGuests(), Math.max(1, this.guests() + delta)));
  }
}
