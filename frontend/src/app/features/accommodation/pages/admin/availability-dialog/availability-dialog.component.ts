import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

import { AccommodationService } from '../../../services/accommodation.service';
import { NotificationService } from '../../../services/notification.service';
import { BookedPeriod, PeriodStatus } from '../../../models/accommodation.models';
import { toIsoDate, today } from '../../../utils/date.utils';

@Component({
  selector: 'tm-availability-dialog',
  standalone: true,
  imports: [DatePipe, ReactiveFormsModule, MatDialogModule, MatDatepickerModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatButtonModule, MatIconModule, MatTooltipModule],
  template: `
    <h2 mat-dialog-title>Availability · {{ data.name }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" (ngSubmit)="add()" class="add-form">
        <mat-form-field appearance="outline" subscriptSizing="dynamic" class="dates">
          <mat-label>Start – end (check-out day)</mat-label>
          <mat-date-range-input [rangePicker]="picker" [min]="minDate">
            <input matStartDate formControlName="startDate" placeholder="Start">
            <input matEndDate formControlName="endDate" placeholder="End">
          </mat-date-range-input>
          <mat-datepicker-toggle matIconSuffix [for]="picker" />
          <mat-date-range-picker #picker />
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic" class="status">
          <mat-label>Status</mat-label>
          <mat-select formControlName="status">
            <mat-option value="BOOKED">Booked</mat-option>
            <mat-option value="AVAILABLE">Available</mat-option>
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic" class="note">
          <mat-label>Note</mat-label>
          <input matInput formControlName="note" maxlength="255">
        </mat-form-field>
        <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || saving()">
          <mat-icon>add</mat-icon> Add
        </button>
      </form>

      @if (periods().length) {
        <table class="periods">
          <thead><tr><th>From</th><th>To (check-out)</th><th>Status</th><th>Note</th><th></th></tr></thead>
          <tbody>
            @for (p of periods(); track p.id) {
              <tr>
                <td>{{ p.startDate | date: 'mediumDate' }}</td>
                <td>{{ p.endDate | date: 'mediumDate' }}</td>
                <td><span class="tm-badge" [class.coral]="p.status === 'BOOKED'">{{ p.status }}</span></td>
                <td class="tm-muted">{{ p.note }}</td>
                <td><button mat-icon-button matTooltip="Remove" aria-label="Remove period" (click)="remove(p)"><mat-icon>delete</mat-icon></button></td>
              </tr>
            }
          </tbody>
        </table>
      } @else {
        <p class="tm-muted empty">{{ loading() ? 'Loading…' : 'No period yet: the accommodation is available every day.' }}</p>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Close</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .add-form { display: grid; grid-template-columns: 1.6fr 1fr; gap: 12px; align-items: center; padding-top: 8px; }
    .add-form .note { grid-column: 1 / 2; }
    .add-form button { height: 52px; }
    mat-form-field { width: 100%; }
    .periods { width: 100%; border-collapse: collapse; margin-top: 20px; font-size: 14px; }
    .periods th { text-align: left; color: #64748B; font-weight: 600; padding: 6px; border-bottom: 1px solid #E3E8EE; }
    .periods td { padding: 4px 6px; border-bottom: 1px solid #F0F3F6; }
    .periods mat-icon { color: #E53935; }
    .empty { margin-top: 20px; }
    @media (max-width: 600px) { .add-form { grid-template-columns: 1fr; } .add-form .note { grid-column: auto; } }
  `],
})
export class AvailabilityDialogComponent implements OnInit {
  readonly data = inject<{ id: number; name: string }>(MAT_DIALOG_DATA);
  private readonly api = inject(AccommodationService);
  private readonly notify = inject(NotificationService);
  private readonly fb = inject(FormBuilder);

  readonly minDate = today();
  readonly periods = signal<BookedPeriod[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);

  readonly form = this.fb.group({
    startDate: this.fb.control<Date | null>(null, Validators.required),
    endDate: this.fb.control<Date | null>(null, Validators.required),
    status: this.fb.nonNullable.control<PeriodStatus>('BOOKED'),
    note: this.fb.nonNullable.control('', Validators.maxLength(255)),
  });

  ngOnInit(): void {
    this.load();
  }

  add(): void {
    const v = this.form.getRawValue();
    if (!v.startDate || !v.endDate) return;
    this.saving.set(true);
    this.api.addPeriod(this.data.id, {
      startDate: toIsoDate(v.startDate)!, endDate: toIsoDate(v.endDate)!, status: v.status, note: v.note || null,
    }).subscribe({
      next: () => {
        this.saving.set(false);
        this.notify.success('Period added');
        this.form.reset({ status: 'BOOKED', note: '' });
        this.load();
      },
      error: err => { this.saving.set(false); this.notify.error(err, 'Could not add the period'); },
    });
  }

  remove(period: BookedPeriod): void {
    this.api.deletePeriod(this.data.id, period.id).subscribe({
      next: () => { this.notify.success('Period removed'); this.load(); },
      error: err => this.notify.error(err, 'Could not remove the period'),
    });
  }

  private load(): void {
    this.loading.set(true);
    this.api.periods(this.data.id).subscribe({
      next: p => { this.periods.set(p); this.loading.set(false); },
      error: err => { this.loading.set(false); this.notify.error(err); },
    });
  }
}
