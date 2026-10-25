import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { forkJoin } from 'rxjs';
import { IconComponent } from '../components/icon/icon.component';
import { INTERESTS } from '../models/user.model';
import { getErrorMessage } from '../services/api';
import { Trip } from '../trips/trip.model';
import { TripService } from '../trips/trip.service';
import { Itinerary, formatDate, formatEuros } from './itinerary.model';
import { ItineraryService } from './itinerary.service';

// MODULE 6 - Page "Mes itineraires" : les voyages du module 2, et pour chacun son itineraire genere par l'IA
@Component({
  selector: 'tm-itineraries',
  imports: [RouterLink, MatButtonModule, MatProgressBarModule, IconComponent],
  templateUrl: './itineraries.component.html',
  styleUrl: './itineraries.component.css',
})
export class ItinerariesComponent implements OnInit, OnDestroy {
  trips: Trip[] = [];
  itineraryByTrip: Record<number, Itinerary> = {};
  loading = true;
  errorMessage = '';

  // Partie IA : voyage en cours de generation et secondes ecoulees (le modele local met 15 a 40 s)
  generatingTripId: number | null = null;
  seconds = 0;
  private timer: ReturnType<typeof setInterval> | undefined;

  formatEuros = formatEuros;

  constructor(private tripService: TripService, private itineraryService: ItineraryService,
              private router: Router) {}

  ngOnInit(): void {
    // Integration avec le module 2 : les voyages de l'utilisateur
    forkJoin({ trips: this.tripService.getMyTrips(), itineraries: this.itineraryService.getMyItineraries() })
      .subscribe({
        next: ({ trips, itineraries }) => {
          this.trips = trips;
          itineraries.forEach((it) => (this.itineraryByTrip[it.tripId] = it));
          this.loading = false;
        },
        error: (err) => {
          this.errorMessage = getErrorMessage(err);
          this.loading = false;
        },
      });
  }

  ngOnDestroy(): void {
    this.stopTimer();
  }

  generate(trip: Trip): void {
    this.generatingTripId = trip.id;
    this.errorMessage = '';
    this.startTimer();
    this.itineraryService.generate(trip.id).subscribe({
      // La page de l'itineraire affiche l'ajustement automatique du budget, passe dans la navigation
      next: (result) => this.router.navigate(['/itineraires', result.itinerary.id], {
        state: { optimization: result.optimization },
      }),
      error: (err) => {
        this.errorMessage = getErrorMessage(err);
        this.generatingTripId = null;
        this.stopTimer();
      },
    });
  }

  getDuration(trip: Trip): number {
    const ms = new Date(trip.endDate).getTime() - new Date(trip.startDate).getTime();
    return Math.round(ms / 86_400_000) + 1;
  }

  formatRange(trip: Trip): string {
    return `${formatDate(trip.startDate)} → ${formatDate(trip.endDate)}`;
  }

  getInterestLabel(value: string): string {
    return INTERESTS.find((i) => i.value === value)?.label ?? value;
  }

  private startTimer(): void {
    this.stopTimer();
    this.seconds = 0;
    this.timer = setInterval(() => this.seconds++, 1000);
  }

  private stopTimer(): void {
    clearInterval(this.timer);
  }
}
