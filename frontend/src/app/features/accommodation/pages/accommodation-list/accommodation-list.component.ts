import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, ParamMap, Router } from '@angular/router';
import { catchError, map, of, switchMap, tap } from 'rxjs';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { AccommodationService } from '../../services/accommodation.service';
import { NotificationService } from '../../services/notification.service';
import { AccommodationSearchCriteria, AccommodationSummary, AccommodationType, Amenity, PageResponse } from '../../models/accommodation.models';
import { SearchBarComponent, SearchBarValue } from '../../components/search-bar/search-bar.component';
import { EMPTY_FILTERS, FiltersPanelComponent, FiltersValue } from '../../components/filters-panel/filters-panel.component';
import { AccommodationCardComponent } from '../../components/accommodation-card/accommodation-card.component';
import { SkeletonCardComponent } from '../../components/skeleton-card/skeleton-card.component';

interface ListState {
  search: SearchBarValue;
  filters: FiltersValue;
  sort: string;
  page: number;
  size: number;
}

const DEFAULT_SORT = 'pricePerNight,asc';
const DEFAULT_SIZE = 12;

@Component({
  selector: 'tm-accommodation-list',
  standalone: true,
  imports: [MatPaginatorModule, MatSelectModule, MatFormFieldModule, MatButtonModule, MatIconModule,
    SearchBarComponent, FiltersPanelComponent, AccommodationCardComponent, SkeletonCardComponent],
  templateUrl: './accommodation-list.component.html',
  styleUrl: './accommodation-list.component.scss',
})
export class AccommodationListComponent implements OnInit {
  private readonly api = inject(AccommodationService);
  private readonly notify = inject(NotificationService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly sortOptions = [
    { value: 'pricePerNight,asc', label: 'Price: low to high' },
    { value: 'pricePerNight,desc', label: 'Price: high to low' },
    { value: 'rating,desc', label: 'Top rated' },
    { value: 'stars,desc', label: 'Most stars' },
    { value: 'name,asc', label: 'Name (A-Z)' },
  ];
  readonly skeletons = Array.from({ length: 8 });

  readonly state = signal<ListState>(this.parseState(this.route.snapshot.queryParamMap));
  readonly result = signal<PageResponse<AccommodationSummary> | null>(null);
  readonly loading = signal(true);
  readonly failed = signal(false);
  readonly cities = signal<string[]>([]);
  readonly showFilters = signal(false);

  /** Dates/guests forwarded to the detail page so the calculator is pre-filled. */
  readonly linkParams = computed(() => {
    const s = this.state().search;
    return { checkIn: s.checkIn, checkOut: s.checkOut, guests: s.guests };
  });

  readonly title = computed(() => {
    const city = this.state().search.city;
    return city ? `Stays in ${city}` : 'Find your perfect stay';
  });

  ngOnInit(): void {
    this.api.cities().pipe(catchError(() => of([]))).subscribe(c => this.cities.set(c));

    this.route.queryParamMap.pipe(
      map(params => this.parseState(params)),
      tap(state => { this.state.set(state); this.loading.set(true); this.failed.set(false); }),
      switchMap(state => this.api.search(this.toCriteria(state), { page: state.page, size: state.size, sort: state.sort }).pipe(
        catchError(err => {
          this.notify.error(err, 'Could not load accommodations');
          this.failed.set(true);
          return of(null);
        }),
      )),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(result => {
      this.result.set(result);
      this.loading.set(false);
    });
  }

  onSearch(search: SearchBarValue): void {
    this.navigate({ ...this.state(), search, page: 0 });
  }

  onFilters(filters: FiltersValue): void {
    this.navigate({ ...this.state(), filters, page: 0 });
  }

  onSort(sort: string): void {
    this.navigate({ ...this.state(), sort, page: 0 });
  }

  onPage(event: PageEvent): void {
    this.navigate({ ...this.state(), page: event.pageIndex, size: event.pageSize });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  resetAll(): void {
    this.navigate({ search: { city: null, checkIn: null, checkOut: null, guests: null }, filters: { ...EMPTY_FILTERS },
      sort: DEFAULT_SORT, page: 0, size: this.state().size });
  }

  retry(): void {
    this.navigate({ ...this.state() }, true);
  }

  // ---------- URL <-> state ----------

  private navigate(state: ListState, force = false): void {
    const { search, filters } = state;
    const queryParams: Record<string, string | number | null> = {
      city: search.city, checkIn: search.checkIn, checkOut: search.checkOut, guests: search.guests,
      type: filters.type, minPrice: filters.minPrice, maxPrice: filters.maxPrice,
      minStars: filters.minStars, minRating: filters.minRating,
      amenities: filters.amenities.length ? filters.amenities.join(',') : null,
      sort: state.sort !== DEFAULT_SORT ? state.sort : null,
      page: state.page ? state.page : null,
      size: state.size !== DEFAULT_SIZE ? state.size : null,
      _r: force ? Date.now() : null,
    };
    this.router.navigate([], { relativeTo: this.route, queryParams });
  }

  private parseState(p: ParamMap): ListState {
    const num = (key: string) => {
      const v = p.get(key);
      return v !== null && v !== '' && !isNaN(+v) ? +v : null;
    };
    return {
      search: { city: p.get('city'), checkIn: p.get('checkIn'), checkOut: p.get('checkOut'), guests: num('guests') },
      filters: {
        type: (p.get('type') as AccommodationType) || null,
        minPrice: num('minPrice'), maxPrice: num('maxPrice'),
        minStars: num('minStars'), minRating: num('minRating'),
        amenities: (p.get('amenities')?.split(',').filter(Boolean) ?? []) as Amenity[],
      },
      sort: p.get('sort') ?? DEFAULT_SORT,
      page: num('page') ?? 0,
      size: num('size') ?? DEFAULT_SIZE,
    };
  }

  private toCriteria(state: ListState): AccommodationSearchCriteria {
    return { ...state.search, ...state.filters };
  }
}
