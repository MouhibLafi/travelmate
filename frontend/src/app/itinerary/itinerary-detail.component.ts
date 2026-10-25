import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { IconComponent } from '../components/icon/icon.component';
import { getErrorMessage } from '../services/api';
import { BudgetTrackingComponent } from './budget-tracking.component';
import { ItemFormComponent } from './item-form.component';
import {
  Itinerary, ItineraryDay, ItineraryItem, OptimizationResult, formatDate, formatEuros, getTypeLabel,
} from './itinerary.model';
import { ItineraryService } from './itinerary.service';

// MODULE 6 - Un itineraire : jour par jour, ajustement du budget par l'IA, suivi des depenses, export PDF
@Component({
  selector: 'tm-itinerary-detail',
  imports: [RouterLink, MatButtonModule, MatProgressBarModule, IconComponent, ItemFormComponent, BudgetTrackingComponent],
  templateUrl: './itinerary-detail.component.html',
  styleUrl: './itinerary-detail.component.css',
})
export class ItineraryDetailComponent implements OnInit, OnDestroy {
  itinerary: Itinerary | null = null;
  tab: 'plan' | 'budget' = 'plan';
  errorMessage = '';

  // Resultat de l'ajustement du budget par l'IA (automatique a la generation, ou bouton)
  optimization: OptimizationResult | null = null;

  // Action en cours, et secondes ecoulees pendant un appel a l'IA
  busy: 'optimize' | 'regenerate' | 'pdf' | null = null;
  seconds = 0;
  private timer: ReturnType<typeof setInterval> | undefined;

  // Formulaire d'element : journee concernee, et element si modification
  formDay: ItineraryDay | null = null;
  formItem: ItineraryItem | null = null;

  formatEuros = formatEuros;
  formatDate = formatDate;
  getTypeLabel = getTypeLabel;

  constructor(private route: ActivatedRoute, private router: Router, private itineraryService: ItineraryService) {}

  ngOnInit(): void {
    // Passe par la page "Mes itineraires" quand la generation a ajuste le budget automatiquement
    this.optimization = history.state?.optimization ?? null;
    this.load(Number(this.route.snapshot.paramMap.get('id')));
  }

  ngOnDestroy(): void {
    this.stopTimer();
  }

  // Pourcentage du budget utilise par le cout estime (pour la jauge)
  budgetUsage(): number {
    const it = this.itinerary;
    return it && it.budget > 0 ? (it.totalEstimatedCost / it.budget) * 100 : 0;
  }

  optimize(): void {
    this.startAi('optimize');
    this.itineraryService.optimize(this.itinerary!.id).subscribe({
      next: (result) => {
        this.optimization = result;
        this.itinerary = result.itinerary;
        this.endAction();
      },
      error: (err) => this.fail(err),
    });
  }

  regenerate(): void {
    if (!confirm('Régénérer tout l’itinéraire ? Vos modifications seront perdues.')) {
      return;
    }
    this.startAi('regenerate');
    this.optimization = null;
    this.itineraryService.generate(this.itinerary!.tripId).subscribe({
      next: (result) => {
        this.itinerary = result.itinerary;
        this.optimization = result.optimization;
        this.endAction();
        this.router.navigate(['/itineraires', result.itinerary.id], { replaceUrl: true });
      },
      error: (err) => this.fail(err),
    });
  }

  downloadPdf(): void {
    const it = this.itinerary!;
    this.busy = 'pdf';
    this.itineraryService.downloadPdf(it.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `itineraire-${it.destination.split(',')[0].toLowerCase()}.pdf`;
        link.click();
        URL.revokeObjectURL(url);
        this.endAction();
      },
      error: (err) => this.fail(err),
    });
  }

  deleteItinerary(): void {
    if (!confirm('Supprimer cet itinéraire ?')) {
      return;
    }
    this.itineraryService.delete(this.itinerary!.id).subscribe({
      next: () => this.router.navigate(['/itineraires']),
      error: (err) => this.fail(err),
    });
  }

  openForm(day: ItineraryDay, item: ItineraryItem | null): void {
    this.formDay = day;
    this.formItem = item;
  }

  closeForm(): void {
    this.formDay = null;
    this.formItem = null;
  }

  onItemSaved(it: Itinerary): void {
    this.itinerary = it;
    this.closeForm();
  }

  deleteItem(item: ItineraryItem): void {
    if (!confirm(`Retirer « ${item.name} » ?`)) {
      return;
    }
    this.itineraryService.deleteItem(item.id).subscribe({
      next: (it) => (this.itinerary = it),
      error: (err) => this.fail(err),
    });
  }

  // "09:00:00" -> "09:00"
  time(value: string | null): string {
    return value ? value.slice(0, 5) : '';
  }

  formatDay(date: string): string {
    return new Date(date).toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' });
  }

  private load(id: number): void {
    this.itineraryService.getById(id).subscribe({
      next: (it) => (this.itinerary = it),
      error: (err) => (this.errorMessage = getErrorMessage(err)),
    });
  }

  private startAi(action: 'optimize' | 'regenerate'): void {
    this.busy = action;
    this.errorMessage = '';
    this.stopTimer();
    this.seconds = 0;
    this.timer = setInterval(() => this.seconds++, 1000);
  }

  private endAction(): void {
    this.busy = null;
    this.stopTimer();
  }

  private fail(err: HttpErrorResponse): void {
    this.errorMessage = getErrorMessage(err);
    this.endAction();
  }

  private stopTimer(): void {
    clearInterval(this.timer);
  }
}
