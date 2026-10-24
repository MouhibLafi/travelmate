import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { IconComponent } from '../../components/icon/icon.component';
import { AiResponse, BUDGETS, INTERESTS, Preferences } from '../../models/user.model';
import { AiService } from '../../services/ai.service';
import { getErrorMessage } from '../../services/api';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'tm-preferences',
  imports: [FormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatProgressBarModule, IconComponent],
  templateUrl: './preferences.component.html',
  styleUrl: './preferences.component.css',
})
export class PreferencesComponent implements OnInit {
  // Listes affichees dans la page
  interestsList = INTERESTS;
  budgetsList = BUDGETS;

  // Les preferences de l'utilisateur
  preferences: Preferences = { description: '', interests: [], budgetLevel: null, aiGenerated: false };

  // Partie IA
  suggestion: AiResponse | null = null;
  analyzing = false;
  aiError = '';

  // Messages
  successMessage = '';
  errorMessage = '';
  saving = false;

  constructor(private userService: UserService, private aiService: AiService) {}

  // Au chargement de la page : on recupere les preferences enregistrees
  ngOnInit(): void {
    this.userService.getPreferences().subscribe({
      next: (data) => (this.preferences = data),
      error: (error) => (this.errorMessage = getErrorMessage(error)),
    });
  }

  // ========== IA ==========

  // 1. On envoie la phrase a l'IA (llama3 via le backend)
  analyze(): void {
    this.analyzing = true;
    this.aiError = '';
    this.suggestion = null;

    this.aiService.analyzeProfile(this.preferences.description ?? '').subscribe({
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

  // 2. L'utilisateur accepte la suggestion : elle remplit le formulaire (il peut encore la modifier)
  applySuggestion(): void {
    if (!this.suggestion) return;
    this.preferences.interests = [...this.suggestion.interests];
    this.preferences.budgetLevel = this.suggestion.budgetLevel;
    this.preferences.aiGenerated = true;
    this.suggestion = null;
  }

  ignoreSuggestion(): void {
    this.suggestion = null;
  }

  // ========== Formulaire ==========

  isSelected(interest: string): boolean {
    return this.preferences.interests.includes(interest);
  }

  // Clic sur un centre d'interet : on l'ajoute ou on l'enleve
  toggleInterest(interest: string): void {
    if (this.isSelected(interest)) {
      this.preferences.interests = this.preferences.interests.filter((i) => i !== interest);
    } else {
      this.preferences.interests = [...this.preferences.interests, interest];
    }
    this.preferences.aiGenerated = false; // modifie a la main
  }

  selectBudget(budget: string): void {
    this.preferences.budgetLevel = budget;
    this.preferences.aiGenerated = false;
  }

  save(): void {
    this.saving = true;
    this.successMessage = '';
    this.errorMessage = '';

    this.userService.updatePreferences(this.preferences).subscribe({
      next: (data) => {
        this.preferences = data;
        this.successMessage = 'Vos préférences ont été enregistrées.';
        this.saving = false;
      },
      error: (error) => {
        this.errorMessage = getErrorMessage(error);
        this.saving = false;
      },
    });
  }

  // ========== Affichage ==========

  getInterestLabel(value: string): string {
    return this.interestsList.find((i) => i.value === value)?.label ?? value;
  }

  getBudgetLabel(value: string): string {
    return this.budgetsList.find((b) => b.value === value)?.label ?? value;
  }

  // 0.87 -> 87
  getConfidencePercent(): number {
    return Math.round((this.suggestion?.confidence ?? 0) * 100);
  }
}
