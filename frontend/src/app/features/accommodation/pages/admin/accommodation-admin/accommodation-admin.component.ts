import { AfterViewInit, Component, DestroyRef, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subject, catchError, debounceTime, distinctUntilChanged, filter, merge, of, startWith, switchMap, tap } from 'rxjs';
import { MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { AccommodationService } from '../../../services/accommodation.service';
import { NotificationService } from '../../../services/notification.service';
import { AccommodationSummary } from '../../../models/accommodation.models';
import { TYPE_META } from '../../../utils/amenity.utils';
import { ImageFallbackDirective } from '../../../components/image-fallback.directive';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../components/confirm-dialog/confirm-dialog.component';
import { AvailabilityDialogComponent } from '../availability-dialog/availability-dialog.component';

@Component({
  selector: 'tm-accommodation-admin',
  standalone: true,
  imports: [CurrencyPipe, DecimalPipe, ReactiveFormsModule, RouterLink, MatTableModule, MatPaginatorModule, MatSortModule,
    MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule, MatTooltipModule, MatDialogModule,
    MatProgressBarModule, ImageFallbackDirective],
  templateUrl: './accommodation-admin.component.html',
  styleUrl: './accommodation-admin.component.scss',
})
export class AccommodationAdminComponent implements AfterViewInit {
  private readonly api = inject(AccommodationService);
  private readonly notify = inject(NotificationService);
  private readonly dialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);

  readonly columns = ['image', 'name', 'type', 'city', 'stars', 'rating', 'pricePerNight', 'maxGuests', 'actions'];
  readonly typeMeta: Record<string, { label: string; icon: string }> = TYPE_META;
  readonly searchControl = new FormControl('', { nonNullable: true });

  readonly rows = signal<AccommodationSummary[]>([]);
  readonly total = signal(0);
  readonly loading = signal(true);

  private readonly paginator = viewChild.required(MatPaginator);
  private readonly sort = viewChild.required(MatSort);
  private readonly refresh$ = new Subject<void>();

  ngAfterViewInit(): void {
    const search$ = this.searchControl.valueChanges.pipe(debounceTime(300), distinctUntilChanged(),
      tap(() => this.paginator().firstPage()));
    this.sort().sortChange.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.paginator().firstPage());

    merge(search$, this.sort().sortChange, this.paginator().page, this.refresh$).pipe(
      startWith(null),
      tap(() => this.loading.set(true)),
      switchMap(() => {
        const s = this.sort();
        const sort = s.active && s.direction ? `${s.active},${s.direction}` : 'createdAt,desc';
        return this.api.search({ q: this.searchControl.value.trim() || null },
          { page: this.paginator().pageIndex, size: this.paginator().pageSize, sort }).pipe(
          catchError(err => { this.notify.error(err, 'Could not load accommodations'); return of(null); }),
        );
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(page => {
      this.loading.set(false);
      this.rows.set(page?.content ?? []);
      this.total.set(page?.totalElements ?? 0);
    });
  }

  confirmDelete(row: AccommodationSummary): void {
    this.dialog.open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, {
      width: '420px',
      data: {
        title: 'Delete accommodation?',
        message: `"${row.name}" and all its booked periods will be permanently deleted.`,
        confirmLabel: 'Delete',
        danger: true,
      },
    }).afterClosed().pipe(
      filter(Boolean),
      switchMap(() => this.api.delete(row.id)),
    ).subscribe({
      next: () => { this.notify.success(`"${row.name}" deleted`); this.refresh$.next(); },
      error: err => this.notify.error(err, 'Delete failed'),
    });
  }

  openAvailability(row: AccommodationSummary): void {
    this.dialog.open(AvailabilityDialogComponent, { width: '640px', maxWidth: '95vw', data: { id: row.id, name: row.name } });
  }
}
