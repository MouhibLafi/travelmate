import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { IconComponent } from '../components/icon/icon.component';
import { BUDGETS, INTERESTS } from '../models/user.model';
import { getErrorMessage } from '../services/api';
import { UserService } from '../services/user.service';
import { Trip, TripSuggestion } from './trip.model';
import { TripService } from './trip.service';

// MODULE 2 - Planification des voyages : IA + formulaire (creer / modifier) + liste
@Component({
  selector: 'tm-trips',
  imports: [FormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatProgressBarModule, IconComponent],
  templateUrl: './trips.component.html',
  styleUrl: './trips.component.css',
})
export class TripsComponent implements OnInit {
  trips: Trip[] = [];

  // Listes partagees avec le module 1 (meme vocabulaire)
  interestsList = INTERESTS;
  budgetsList = BUDGETS;

  // Formulaire
  editingId: number | null = null;   // null = creation, sinon = modification de ce voyage
  destination = '';
  startDate = '';
  endDate = '';
  budget: number | null = null;
  interests: string[] = [];
  budgetLevel: string | null = null;

  // Partie IA
  aiText = '';
  suggestion: TripSuggestion | null = null;
  analyzing = false;
  aiError = '';

  // Preferences du module 1 (pour preremplir un nouveau voyage)
  private preferredInterests: string[] = [];
  private preferredBudget: string | null = null;

  errorMessage = '';
  successMessage = '';

  constructor(private tripService: TripService, private userService: UserService) {}

  ngOnInit(): void {
    this.loadTrips();

    // Integration avec le module 1 : on preremplit avec les preferences de l'utilisateur
    this.userService.getPreferences().subscribe((prefs) => {
      this.preferredInterests = prefs.interests;
      this.preferredBudget = prefs.budgetLevel;
      this.resetForm();
    });
  }

  loadTrips(): void {
    this.tripService.getMyTrips().subscribe({
      next: (trips) => (this.trips = trips),
      error: (error) => (this.errorMessage = getErrorMessage(error)),
    });
  }

  // ========== IA ==========

  // 1. On envoie la demande a l'IA (llama3 via le backend)
  analyze(): void {
    this.analyzing = true;
    this.aiError = '';
    this.suggestion = null;

    this.tripService.fillForm(this.aiText).subscribe({
      next: (result) => {
        this.suggestion = result;
        this.analyzing = false;
      },
      error: (error) => {
        this.aiError = getErrorMessage(error);
        this.analyzing = false;
      },
    });
  }

  // 2. L'utilisateur accepte : la suggestion remplit le formulaire (il peut encore tout modifier)
  applySuggestion(): void {
    if (!this.suggestion) return;
    const s = this.suggestion;
    this.editingId = null;
    this.destination = s.destination ?? '';
    this.startDate = s.startDate;
    this.endDate = s.endDate;
    this.budget = s.budget;
    // Si l'IA n'a trouve aucun centre d'interet, on garde ceux des preferences
    this.interests = s.interests.length > 0 ? [...s.interests] : [...this.preferredInterests];
    this.budgetLevel = s.budgetLevel ?? this.preferredBudget;
    this.suggestion = null;
    this.successMessage = '';
  }

  ignoreSuggestion(): void {
    this.suggestion = null;
  }

  getConfidencePercent(): number {
    return Math.round((this.suggestion?.confidence ?? 0) * 100);
  }

  // ========== Formulaire ==========

  toggleInterest(interest: string): void {
    if (this.interests.includes(interest)) {
      this.interests = this.interests.filter((i) => i !== interest);
    } else {
      this.interests = [...this.interests, interest];
    }
  }

  // Charge un voyage existant dans le formulaire
  edit(trip: Trip): void {
    this.editingId = trip.id;
    this.destination = trip.destination;
    this.startDate = trip.startDate;
    this.endDate = trip.endDate;
    this.budget = trip.budget;
    this.interests = [...trip.interests];
    this.budgetLevel = trip.budgetLevel;
    this.errorMessage = '';
    this.successMessage = '';
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  cancelEdit(): void {
    this.resetForm();
  }

  save(): void {
    this.errorMessage = '';
    this.successMessage = '';

    const data = {
      destination: this.destination,
      startDate: this.startDate,
      endDate: this.endDate,
      budget: this.budget,
      interests: this.interests,
      budgetLevel: this.budgetLevel,
    };
    const request = this.editingId === null
      ? this.tripService.create(data)
      : this.tripService.update(this.editingId, data);

    request.subscribe({
      next: () => {
        this.successMessage = this.editingId === null ? 'Voyage créé.' : 'Voyage modifié.';
        this.resetForm();
        this.loadTrips();
      },
      error: (error) => (this.errorMessage = getErrorMessage(error)),
    });
  }

  delete(trip: Trip): void {
    if (!confirm(`Supprimer le voyage à ${trip.destination} ?`)) {
      return;
    }
    this.tripService.delete(trip.id).subscribe({
      next: () => {
        if (this.editingId === trip.id) this.resetForm();
        this.loadTrips();
      },
      error: (error) => (this.errorMessage = getErrorMessage(error)),
    });
  }

  // Formulaire vide, preremplit avec les preferences du module 1
  private resetForm(): void {
    this.editingId = null;
    this.destination = '';
    this.startDate = '';
    this.endDate = '';
    this.budget = null;
    this.interests = [...this.preferredInterests];
    this.budgetLevel = this.preferredBudget;
  }

  // ========== Affichage ==========

  getInterestLabel(value: string): string {
    return INTERESTS.find((i) => i.value === value)?.label ?? value;
  }

  getBudgetLabel(value: string | null): string {
    return BUDGETS.find((b) => b.value === value)?.label ?? '—';
  }

  // "2026-12-20" -> "20/12/2026"
  formatDate(value: string): string {
    const [year, month, day] = value.split('-');
    return `${day}/${month}/${year}`;
  }

  // "2026-12-12" + "2026-12-15" -> "12–15 déc. 2026" (format court de la charte : "Jun 12–16")
  formatRange(start: string, end: string): string {
    const day = (d: string) => new Date(d + 'T00:00').toLocaleDateString('fr-FR', { day: 'numeric' });
    const dayMonth = (d: string) => new Date(d + 'T00:00').toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' });
    const full = (d: string) => new Date(d + 'T00:00').toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' });
    if (start.slice(0, 7) === end.slice(0, 7)) return `${day(start)}–${full(end)}`;     // meme mois
    if (start.slice(0, 4) === end.slice(0, 4)) return `${dayMonth(start)} – ${full(end)}`; // meme annee
    return `${full(start)} – ${full(end)}`;
  }

  // Voyage deja termine ? (date de fin avant aujourd'hui)
  isPast(trip: Trip): boolean {
    return trip.endDate < new Date().toISOString().slice(0, 10);
  }

  // Nombre de jours, jour de depart compris
  getDuration(trip: { startDate: string; endDate: string }): number {
    const ms = new Date(trip.endDate).getTime() - new Date(trip.startDate).getTime();
    return Math.round(ms / 86400000) + 1;
  }
}
