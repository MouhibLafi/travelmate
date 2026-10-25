import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { ActiviteService, RecommandationParams } from '../services/activite.service';
import { DestinationService } from '../services/destination.service';
import { RecommandationDTO } from '../models/recommandation.model';
import { Destination } from '../models/destination.model';

const INTERETS = [
  { nom: 'Culture', emoji: '🎨' },
  { nom: 'Histoire', emoji: '🏛️' },
  { nom: 'Nature', emoji: '🌿' },
  { nom: 'Gastronomie', emoji: '🍽️' },
  { nom: 'Aventure', emoji: '🪂' },
  { nom: 'Détente', emoji: '🌅' },
  { nom: 'Sport', emoji: '🏄' },
  { nom: 'Shopping', emoji: '🛍️' },
];

@Component({
  selector: 'app-recommandations',
  standalone: true,
  host: { class: 'm3' },   // integration : styles du module 3 limites a ses pages (styles/_module3.scss)
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="page">
      <!-- Hero -->
      <div class="reco-hero">
        <div class="reco-hero__icon">🤖</div>
        <div>
          <h1>Recommandations IA</h1>
          <p>Notre intelligence artificielle analyse vos centres d'intérêt pour vous suggérer les activités parfaites.</p>
        </div>
      </div>

      <!-- Form -->
      <div class="reco-form card">
        <!-- Intérêts -->
        <div class="form-section">
          <div class="form-section__title">
            <span>Vos centres d'intérêt</span>
            <span class="form-section__hint">{{ selectionnes.length }} sélectionné{{ selectionnes.length > 1 ? 's' : '' }}</span>
          </div>
          <div class="interets-grid">
            <button
              *ngFor="let i of tousInterets"
              class="interet-btn"
              [class.interet-btn--active]="isSelected(i.nom)"
              (click)="toggle(i.nom)"
              type="button"
            >
              <span class="interet-btn__emoji">{{ i.emoji }}</span>
              <span>{{ i.nom }}</span>
            </button>
          </div>
        </div>

        <div class="divider"></div>

        <!-- Options -->
        <div class="form-options">
          <div class="form-group">
            <label>Destination (optionnel)</label>
            <select [(ngModel)]="destinationId">
              <option [ngValue]="undefined">Toutes les destinations</option>
              <option *ngFor="let d of destinations" [ngValue]="d.id">{{ d.nom }} — {{ d.pays }}</option>
            </select>
          </div>

          <div class="form-group">
            <label>Budget max par activité (€)</label>
            <input type="number" min="0" [(ngModel)]="budgetMax" placeholder="Illimité" />
          </div>

          <div class="form-group">
            <label>Nombre de résultats : <strong>{{ limite }}</strong></label>
            <input type="range" min="1" max="15" [(ngModel)]="limite" />
            <div class="range-labels"><span>1</span><span>15</span></div>
          </div>
        </div>

        <div class="reco-form__actions">
          <button
            class="btn btn--primary btn--lg"
            (click)="charger()"
            [disabled]="selectionnes.length === 0 || loading"
          >
            <span *ngIf="!loading">✨ Obtenir mes recommandations</span>
            <span *ngIf="loading">Analyse en cours…</span>
          </button>
          <p *ngIf="selectionnes.length === 0" class="form-hint">Sélectionnez au moins un centre d'intérêt</p>
        </div>
      </div>

      <!-- Loading -->
      <div *ngIf="loading" class="loading-spinner">L'IA analyse vos préférences…</div>

      <!-- Error -->
      <div *ngIf="error" class="error-message">⚠ {{ error }}</div>

      <!-- Results -->
      <div *ngIf="recommandations.length > 0 && !loading" class="results-section">
        <div class="results-header">
          <h2>{{ recommandations.length }} recommandation{{ recommandations.length > 1 ? 's' : '' }} pour vous</h2>
          <div class="results-tags">
            <span *ngFor="let s of selectionnes" class="tag">{{ getEmoji(s) }} {{ s }}</span>
          </div>
        </div>

        <div class="reco-list">
          <div *ngFor="let r of recommandations; let i = index" class="reco-card card">
            <!-- Rank -->
            <div class="reco-rank" [class.reco-rank--top]="i < 3">{{ i + 1 }}</div>

            <!-- Content -->
            <div class="reco-content">
              <div class="reco-meta-row">
                <span class="badge">{{ r.activite.categorie.nom }}</span>
                <span class="reco-dest">📍 {{ r.activite.destination.nom }}, {{ r.activite.destination.pays }}</span>
                <span *ngIf="i === 0" class="reco-best-badge">⭐ Meilleure correspondance</span>
              </div>

              <h3>
                <a [routerLink]="['/activites', r.activite.id]">{{ r.activite.nom }}</a>
              </h3>

              <!-- Score bar -->
              <div class="score-section">
                <div class="score-label">
                  <span>Pertinence</span>
                  <strong>{{ r.score }}/100</strong>
                </div>
                <div class="progress-bar">
                  <div class="progress-bar__fill" [style.width.%]="r.score"></div>
                </div>
              </div>

              <!-- Raison IA -->
              <div class="reco-raison">
                <span class="reco-raison__icon">💬</span>
                <p>{{ r.raison }}</p>
              </div>

              <div class="reco-bottom">
                <div class="reco-price">
                  <span *ngIf="r.activite.prix === 0" class="price-free">Gratuit</span>
                  <span *ngIf="r.activite.prix > 0" class="price-value">{{ r.activite.prix | number:'1.0-0' }} €</span>
                </div>
                <div class="reco-duree">🕐 {{ formatDuree(r.activite.dureeMinutes) }}</div>
                <a [routerLink]="['/activites', r.activite.id]" class="btn btn--secondary btn--sm">
                  Voir l'activité →
                </a>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .reco-hero {
      display: flex;
      align-items: center;
      gap: 1.5rem;
      background: linear-gradient(135deg, var(--color-primary-dark) 0%, var(--color-primary) 100%);
      border-radius: var(--border-radius-lg);
      padding: 2rem 2.5rem;
      color: white;
      margin-bottom: 2rem;

      h1 { color: white; font-size: 1.75rem; margin-bottom: 0.35rem; }
      p { color: rgba(255,255,255,0.85); font-size: 0.95rem; max-width: 500px; }
    }

    .reco-hero__icon { font-size: 3.5rem; flex-shrink: 0; }

    /* Form */
    .reco-form { display: flex; flex-direction: column; gap: 1.5rem; margin-bottom: 2rem; }

    .form-section__title {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 1rem;
      font-weight: 600;
      font-size: 0.95rem;
    }

    .form-section__hint { font-size: 0.8rem; color: var(--color-text-secondary); font-weight: 400; }

    .interets-grid {
      display: flex;
      flex-wrap: wrap;
      gap: 0.75rem;
    }

    .interet-btn {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.6rem 1.25rem;
      border-radius: var(--border-radius-full);
      border: 1.5px solid var(--color-border);
      background: var(--color-surface);
      font-size: 0.9rem;
      font-weight: 500;
      cursor: pointer;
      transition: all var(--transition);
      color: var(--color-text-secondary);

      &:hover { border-color: var(--color-primary); color: var(--color-primary); background: var(--color-primary-50); }

      &--active {
        background: var(--color-primary);
        border-color: var(--color-primary);
        color: white;
        box-shadow: 0 2px 8px rgba(10,124,110,0.25);
      }

      &__emoji { font-size: 1.1rem; }
    }

    .form-options {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
      gap: 1.25rem;
    }

    .range-labels {
      display: flex;
      justify-content: space-between;
      font-size: 0.75rem;
      color: var(--color-text-muted);
      margin-top: 0.2rem;
    }

    input[type='range'] {
      -webkit-appearance: none;
      width: 100%;
      height: 6px;
      border-radius: 999px;
      background: var(--color-border);
      outline: none;
      border: none;
      box-shadow: none;
      padding: 0;

      &::-webkit-slider-thumb {
        -webkit-appearance: none;
        width: 18px;
        height: 18px;
        border-radius: 50%;
        background: var(--color-primary);
        cursor: pointer;
        box-shadow: 0 1px 4px rgba(0,0,0,0.2);
      }
    }

    .reco-form__actions {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      gap: 0.5rem;
    }

    .form-hint { font-size: 0.8rem; color: var(--color-text-muted); }

    /* Results */
    .results-header {
      display: flex;
      align-items: center;
      gap: 1rem;
      flex-wrap: wrap;
      margin-bottom: 1.5rem;

      h2 { flex-shrink: 0; }
    }

    .results-tags { display: flex; flex-wrap: wrap; gap: 0.4rem; }

    /* Reco card */
    .reco-list { display: flex; flex-direction: column; gap: 1rem; }

    .reco-card {
      display: flex;
      gap: 1.25rem;
      align-items: flex-start;
      padding: 1.25rem;
      transition: transform var(--transition), box-shadow var(--transition);
      &:hover { transform: translateX(4px); }
    }

    .reco-rank {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      background: var(--color-surface-2);
      color: var(--color-text-secondary);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 1rem;
      font-weight: 700;
      flex-shrink: 0;

      &--top {
        background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light));
        color: white;
      }
    }

    .reco-content { flex: 1; display: flex; flex-direction: column; gap: 0.75rem; }

    .reco-meta-row {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 0.75rem;
    }

    .reco-dest { font-size: 0.8rem; color: var(--color-text-secondary); }

    .reco-best-badge {
      padding: 0.15rem 0.6rem;
      border-radius: var(--border-radius-full);
      background: #FEF3C7;
      color: #92400E;
      font-size: 0.72rem;
      font-weight: 600;
    }

    .reco-content h3 a {
      color: var(--color-text);
      font-size: 1.1rem;
      &:hover { color: var(--color-primary); text-decoration: none; }
    }

    .score-section { display: flex; flex-direction: column; gap: 0.4rem; }

    .score-label {
      display: flex;
      justify-content: space-between;
      font-size: 0.78rem;
      color: var(--color-text-secondary);
      strong { color: var(--color-primary); }
    }

    .reco-raison {
      display: flex;
      align-items: flex-start;
      gap: 0.5rem;
      background: var(--color-primary-50);
      border-radius: var(--border-radius);
      padding: 0.75rem 1rem;

      &__icon { font-size: 1rem; flex-shrink: 0; margin-top: 0.1rem; }
      p { font-size: 0.875rem; color: var(--color-primary-dark); font-style: italic; }
    }

    .reco-bottom {
      display: flex;
      align-items: center;
      gap: 1.25rem;
    }

    .price-value { font-size: 1.15rem; font-weight: 700; color: var(--color-primary-dark); }
    .price-free { font-size: 1.15rem; font-weight: 700; color: var(--color-success); }
    .reco-duree { font-size: 0.875rem; color: var(--color-text-secondary); }
  `]
})
export class RecommandationsComponent implements OnInit {
  tousInterets = INTERETS;
  selectionnes: string[] = [];
  destinations: Destination[] = [];
  destinationId?: number;
  budgetMax?: number;
  limite = 5;
  recommandations: RecommandationDTO[] = [];
  loading = false;
  error = '';

  constructor(private activiteService: ActiviteService, private destinationService: DestinationService) {}

  ngOnInit(): void {
    this.destinationService.getAll().subscribe(d => this.destinations = d);
  }

  isSelected(nom: string): boolean { return this.selectionnes.includes(nom); }

  toggle(nom: string): void {
    const i = this.selectionnes.indexOf(nom);
    if (i >= 0) this.selectionnes.splice(i, 1);
    else this.selectionnes.push(nom);
  }

  getEmoji(nom: string): string {
    return this.tousInterets.find(i => i.nom === nom)?.emoji ?? '🎯';
  }

  charger(): void {
    if (this.selectionnes.length === 0) return;
    this.loading = true;
    this.error = '';
    this.recommandations = [];
    const params: RecommandationParams = {
      interets: this.selectionnes,
      destinationId: this.destinationId,
      budgetMax: this.budgetMax,
      limite: this.limite
    };
    this.activiteService.getRecommandations(params).subscribe({
      next: d => { this.recommandations = d; this.loading = false; },
      error: () => { this.error = 'Erreur lors du chargement des recommandations.'; this.loading = false; }
    });
  }

  formatDuree(min: number): string {
    if (min < 60) return `${min} min`;
    const h = Math.floor(min / 60);
    const m = min % 60;
    return m > 0 ? `${h}h${m.toString().padStart(2,'0')}` : `${h}h`;
  }
}
