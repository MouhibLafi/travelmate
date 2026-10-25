import { Component, Input, OnChanges } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { IconComponent } from '../components/icon/icon.component';
import { getErrorMessage } from '../services/api';
import {
  BudgetSummary, Expense, ITEM_TYPES, ItemType, Itinerary, formatDate, formatEuros, getTypeLabel,
} from './itinerary.model';
import { ItineraryService } from './itinerary.service';

// Suivi du budget : cout prevu (itineraire) compare aux depenses reelles, par categorie
@Component({
  selector: 'tm-budget-tracking',
  imports: [FormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, IconComponent],
  templateUrl: './budget-tracking.component.html',
  styleUrl: './budget-tracking.component.css',
})
export class BudgetTrackingComponent implements OnChanges {
  @Input({ required: true }) itinerary!: Itinerary;

  summary: BudgetSummary | null = null;
  expenses: Expense[] = [];
  errorMessage = '';

  types = ITEM_TYPES;
  formatEuros = formatEuros;
  formatDate = formatDate;
  getTypeLabel = getTypeLabel;

  // Formulaire d'une depense
  category: ItemType = 'RESTAURANT';
  amount: number | null = null;
  date = new Date().toISOString().slice(0, 10);
  description = '';

  constructor(private itineraryService: ItineraryService) {}

  // Recharge a chaque changement de l'itineraire (element modifie, budget ajuste...)
  ngOnChanges(): void {
    this.reload();
  }

  addExpense(form: NgForm): void {
    this.errorMessage = '';
    this.itineraryService.addExpense(this.itinerary.id, {
      category: this.category, amount: Number(this.amount), date: this.date, description: this.description || null,
    }).subscribe({
      next: () => {
        this.amount = null;
        this.description = '';
        form.resetForm({ category: this.category, amount: null, date: this.date, description: '' });
        this.reload();
      },
      error: (err) => (this.errorMessage = getErrorMessage(err)),
    });
  }

  deleteExpense(expense: Expense): void {
    this.itineraryService.deleteExpense(expense.id).subscribe({
      next: () => this.reload(),
      error: (err) => (this.errorMessage = getErrorMessage(err)),
    });
  }

  // Largeur d'une barre, par rapport au plus grand montant du graphique
  percent(value: number): number {
    const max = Math.max(1, ...(this.summary?.categories ?? []).flatMap((c) => [c.estimated, c.spent]));
    return (value / max) * 100;
  }

  private reload(): void {
    this.itineraryService.getBudget(this.itinerary.id).subscribe({
      next: (s) => (this.summary = s),
      error: (err) => (this.errorMessage = getErrorMessage(err)),
    });
    this.itineraryService.getExpenses(this.itinerary.id).subscribe((list) => (this.expenses = list));
  }
}
