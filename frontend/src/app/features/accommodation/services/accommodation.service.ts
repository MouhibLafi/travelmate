import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import {
  Accommodation, AccommodationRequest, AccommodationSearchCriteria, AccommodationSummary, AvailabilityResponse,
  BookedDatesResponse, BookedPeriod, BookedPeriodRequest, PageRequest, PageResponse, PriceAnalysis,
  StayCostRequest, StayCostResponse,
} from '../models/accommodation.models';

@Injectable({ providedIn: 'root' })
export class AccommodationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/accommodations`;

  // ---------- Catalog ----------

  search(criteria: AccommodationSearchCriteria, page: PageRequest): Observable<PageResponse<AccommodationSummary>> {
    const params = toParams({ ...criteria, page: page.page, size: page.size, sort: page.sort });
    return this.http.get<PageResponse<AccommodationSummary>>(this.baseUrl, { params });
  }

  getById(id: number): Observable<Accommodation> {
    return this.http.get<Accommodation>(`${this.baseUrl}/${id}`);
  }

  create(request: AccommodationRequest): Observable<Accommodation> {
    return this.http.post<Accommodation>(this.baseUrl, request);
  }

  update(id: number, request: AccommodationRequest): Observable<Accommodation> {
    return this.http.put<Accommodation>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  cities(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/cities`);
  }

  // ---------- Availability ----------

  checkAvailability(id: number, checkIn: string, checkOut: string): Observable<AvailabilityResponse> {
    return this.http.get<AvailabilityResponse>(`${this.baseUrl}/${id}/availability`, { params: toParams({ checkIn, checkOut }) });
  }

  bookedDates(id: number, from?: string, to?: string): Observable<BookedDatesResponse> {
    return this.http.get<BookedDatesResponse>(`${this.baseUrl}/${id}/booked-dates`, { params: toParams({ from, to }) });
  }

  periods(id: number): Observable<BookedPeriod[]> {
    return this.http.get<BookedPeriod[]>(`${this.baseUrl}/${id}/periods`);
  }

  addPeriod(id: number, request: BookedPeriodRequest): Observable<BookedPeriod> {
    return this.http.post<BookedPeriod>(`${this.baseUrl}/${id}/periods`, request);
  }

  deletePeriod(id: number, periodId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}/periods/${periodId}`);
  }

  // ---------- Stay cost & AI ----------

  stayCost(id: number, request: StayCostRequest): Observable<StayCostResponse> {
    return this.http.post<StayCostResponse>(`${this.baseUrl}/${id}/stay-cost`, request);
  }

  priceAnalysis(id: number, options: { checkIn?: string | null; checkOut?: string | null; guests?: number | null } = {}): Observable<PriceAnalysis> {
    return this.http.get<PriceAnalysis>(`${this.baseUrl}/${id}/price-analysis`, { params: toParams(options) });
  }
}

/** Builds HttpParams, skipping null/undefined/empty values; arrays become comma-separated lists. */
function toParams(values: Record<string, unknown>): HttpParams {
  let params = new HttpParams();
  for (const [key, value] of Object.entries(values)) {
    if (value === null || value === undefined || value === '') continue;
    if (Array.isArray(value)) {
      if (value.length) params = params.set(key, value.join(','));
    } else {
      params = params.set(key, String(value));
    }
  }
  return params;
}
