import { Component, computed, effect, input, model, signal, viewChild } from '@angular/core';
import { DateRange, MatCalendar, MatCalendarCellClassFunction, MatDatepickerModule } from '@angular/material/datepicker';
import { MatIconModule } from '@angular/material/icon';

import { addDays, toIsoDate, today } from '../../utils/date.utils';

/**
 * Calendar showing booked nights (struck through in coral). Clicking two days selects a stay;
 * a stay cannot include a booked night (but can check out on the first booked day).
 */
@Component({
  selector: 'tm-availability-calendar',
  standalone: true,
  imports: [MatDatepickerModule, MatIconModule],
  template: `
    <div class="calendar-wrap">
      <mat-calendar
        [selected]="range()"
        [startAt]="checkIn()"
        [minDate]="minDate"
        [dateClass]="dateClass"
        (selectedChange)="onSelect($event)" />
    </div>
    <div class="legend">
      <span><i class="dot free"></i>Available</span>
      <span><i class="dot booked"></i>Booked</span>
      <span><i class="dot selected"></i>Your stay</span>
    </div>
    @if (hint()) {
      <p class="hint"><mat-icon>info</mat-icon>{{ hint() }}</p>
    }
  `,
  styles: [`
    .calendar-wrap { max-width: 380px; }
    .legend { display: flex; gap: 16px; font-size: 13px; color: #64748B; margin-top: 4px; flex-wrap: wrap; }
    .legend span { display: inline-flex; align-items: center; gap: 6px; }
    .dot { width: 12px; height: 12px; border-radius: 50%; display: inline-block; }
    .dot.free { background: #E2F2F3; border: 1px solid #0E7C86; }
    .dot.booked { background: rgba(255,107,74,.25); border: 1px solid #FF6B4A; }
    .dot.selected { background: #0E7C86; }
    .hint { display: flex; align-items: center; gap: 6px; color: #C93C1D; font-size: 13px; margin: 8px 0 0; }
    .hint mat-icon { font-size: 18px; width: 18px; height: 18px; }
  `],
})
export class AvailabilityCalendarComponent {
  bookedDates = input<string[]>([]);
  checkIn = model<Date | null>(null);
  checkOut = model<Date | null>(null);

  readonly minDate = today();
  readonly hint = signal<string | null>(null);
  private readonly calendar = viewChild(MatCalendar);
  private readonly booked = computed(() => new Set(this.bookedDates()));

  readonly range = computed(() => new DateRange<Date>(this.checkIn(), this.checkOut()));

  readonly dateClass: MatCalendarCellClassFunction<Date> = (date, view) =>
    view === 'month' && this.booked().has(toIsoDate(date)!) ? 'tm-booked' : '';

  constructor() {
    // re-render cells when the booked dates arrive
    effect(() => {
      this.booked();
      this.calendar()?.updateTodaysDate();
    });
  }

  onSelect(date: Date | null): void {
    if (!date) return;
    this.hint.set(null);
    const start = this.checkIn();
    const end = this.checkOut();

    if (start && !end && date > start) {
      if (this.hasBookedNight(start, date)) {
        this.hint.set('Your stay would include booked nights. Please choose other dates.');
        return;
      }
      this.checkOut.set(date);
      return;
    }
    if (this.booked().has(toIsoDate(date)!)) {
      this.hint.set('This night is already booked.');
      return;
    }
    this.checkIn.set(date);
    this.checkOut.set(null);
  }

  private hasBookedNight(start: Date, end: Date): boolean {
    for (let d = new Date(start); d < end; d = addDays(d, 1)) {
      if (this.booked().has(toIsoDate(d)!)) return true;
    }
    return false;
  }
}
