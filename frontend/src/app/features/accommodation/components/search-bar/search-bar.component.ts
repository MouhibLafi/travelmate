import { Component, computed, effect, inject, input, output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { fromIsoDate, toIsoDate, today } from '../../utils/date.utils';

export interface SearchBarValue {
  city: string | null;
  checkIn: string | null;
  checkOut: string | null;
  guests: number | null;
}

@Component({
  selector: 'tm-search-bar',
  standalone: true,
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatAutocompleteModule, MatDatepickerModule,
    MatButtonModule, MatIconModule],
  templateUrl: './search-bar.component.html',
  styleUrl: './search-bar.component.scss',
})
export class SearchBarComponent {
  value = input<SearchBarValue>({ city: null, checkIn: null, checkOut: null, guests: null });
  cities = input<string[]>([]);
  search = output<SearchBarValue>();

  readonly minDate = today();
  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.group({
    city: this.fb.control<string | null>(null),
    checkIn: this.fb.control<Date | null>(null),
    checkOut: this.fb.control<Date | null>(null),
    guests: this.fb.control<number | null>(null),
  });

  private readonly cityText = toSignal(this.form.controls.city.valueChanges, { initialValue: null });
  readonly filteredCities = computed(() => {
    const text = (this.cityText() ?? '').toLowerCase();
    return this.cities().filter(c => c.toLowerCase().includes(text));
  });

  constructor() {
    // keep the form in sync with the URL state
    effect(() => {
      const v = this.value();
      this.form.setValue({
        city: v.city, checkIn: fromIsoDate(v.checkIn), checkOut: fromIsoDate(v.checkOut), guests: v.guests,
      }, { emitEvent: false });
    });
  }

  submit(): void {
    const v = this.form.getRawValue();
    const hasRange = !!v.checkIn && !!v.checkOut;
    this.search.emit({
      city: v.city?.trim() || null,
      checkIn: hasRange ? toIsoDate(v.checkIn) : null,
      checkOut: hasRange ? toIsoDate(v.checkOut) : null,
      guests: v.guests && v.guests > 0 ? Math.min(v.guests, 30) : null,
    });
  }

  clearCity(): void {
    this.form.controls.city.setValue(null);
    this.submit();
  }
}
