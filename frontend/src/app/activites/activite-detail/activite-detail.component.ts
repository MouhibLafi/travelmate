import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { switchMap } from 'rxjs';

import { ActiviteService } from '../../services/activite.service';
import { FavoriService } from '../../services/favori.service';
import { Activite } from '../../models/activite.model';

@Component({
  selector: 'app-activite-detail',
  standalone: true,
  host: { class: 'm3' },   // integration : styles du module 3 limites a ses pages (styles/_module3.scss)
  imports: [CommonModule, RouterModule],
  template: `
    <div class="page">
      <a routerLink="/activites" class="back-link">← Retour aux activités</a>

      <div *ngIf="loading" class="loading-spinner">Chargement…</div>
      <div *ngIf="error" class="error-message">⚠ {{ error }}</div>

      <div *ngIf="activite && !loading" class="detail-layout">
        <!-- Colonne gauche -->
        <div class="detail-main">
          <!-- Image header -->
          <div class="detail-hero" [style.background]="getCatColor(activite.categorie.nom)">
            <span class="detail-hero__emoji">{{ getCatEmoji(activite.categorie.nom) }}</span>
            <div class="detail-hero__overlay">
              <span class="badge">{{ activite.categorie.nom }}</span>
            </div>
          </div>

          <div class="detail-body card">
            <div class="detail-body__header">
              <h1>{{ activite.nom }}</h1>
              <a [routerLink]="['/destinations', activite.destination.id]" class="detail-dest-link">
                📍 {{ activite.destination.nom }}, {{ activite.destination.pays }}
              </a>
            </div>

            <p class="detail-desc">{{ activite.description }}</p>

            <!-- Tags -->
            <div *ngIf="activite.tags" class="detail-tags">
              <span *ngFor="let tag of activite.tags.split(',')" class="tag">{{ tag.trim() }}</span>
            </div>
          </div>
        </div>

        <!-- Colonne droite — infos + action -->
        <aside class="detail-sidebar">
          <div class="card detail-info-card">
            <h3>Informations</h3>

            <div class="info-row">
              <span class="info-row__label">Prix</span>
              <span class="info-row__value" [class.info-row__value--free]="activite.prix === 0">
                {{ activite.prix === 0 ? 'Gratuit' : (activite.prix | number:'1.2-2') + ' €' }}
              </span>
            </div>

            <div class="info-row">
              <span class="info-row__label">Durée</span>
              <span class="info-row__value">{{ formatDuree(activite.dureeMinutes) }}</span>
            </div>

            <div class="info-row">
              <span class="info-row__label">Catégorie</span>
              <span class="info-row__value">{{ activite.categorie.nom }}</span>
            </div>

            <div class="info-row">
              <span class="info-row__label">Destination</span>
              <a [routerLink]="['/destinations', activite.destination.id]" class="info-row__value info-row__link">
                {{ activite.destination.nom }}
              </a>
            </div>

            <div class="detail-actions">
              <button
                *ngIf="!isFavori"
                class="btn btn--primary"
                style="width:100%"
                (click)="ajouterFavori()"
                [disabled]="favoriLoading"
              >
                ♥ Ajouter aux favoris
              </button>
              <button
                *ngIf="isFavori"
                class="btn btn--danger"
                style="width:100%"
                (click)="retirerFavori()"
                [disabled]="favoriLoading"
              >
                ✕ Retirer des favoris
              </button>

              <a routerLink="/recommandations" class="btn btn--secondary" style="width:100%;text-align:center">
                ✨ Activités similaires
              </a>
            </div>
          </div>
        </aside>
      </div>
    </div>
  `,
  styles: [`
    .detail-layout {
      display: grid;
      grid-template-columns: 1fr 320px;
      gap: 1.5rem;
      align-items: start;
    }

    .detail-main { display: flex; flex-direction: column; gap: 1rem; }

    .detail-hero {
      height: 240px;
      border-radius: var(--border-radius-lg);
      display: flex;
      align-items: center;
      justify-content: center;
      position: relative;
      overflow: hidden;
    }

    .detail-hero__emoji { font-size: 6rem; filter: drop-shadow(0 4px 8px rgba(0,0,0,0.2)); }

    .detail-hero__overlay {
      position: absolute;
      bottom: 1rem;
      left: 1rem;
    }

    .detail-body { padding: 1.5rem; display: flex; flex-direction: column; gap: 1rem; }

    .detail-body__header { display: flex; flex-direction: column; gap: 0.35rem; }

    .detail-dest-link {
      font-size: 0.875rem;
      color: var(--color-text-secondary);
      &:hover { color: var(--color-primary); text-decoration: none; }
    }

    .detail-desc { line-height: 1.7; color: var(--color-text-secondary); font-size: 0.95rem; }

    .detail-tags { display: flex; flex-wrap: wrap; gap: 0.4rem; }

    /* Sidebar */
    .detail-sidebar { position: sticky; top: 88px; }

    .detail-info-card {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;

      h3 { font-size: 1rem; margin-bottom: 0.75rem; }
    }

    .info-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 0.6rem 0;
      border-bottom: 1px solid var(--color-border);

      &:last-of-type { border-bottom: none; }
    }

    .info-row__label { font-size: 0.82rem; color: var(--color-text-secondary); font-weight: 500; }

    .info-row__value {
      font-size: 0.95rem;
      font-weight: 600;
      color: var(--color-text);
      &--free { color: var(--color-success); }
    }

    .info-row__link {
      font-weight: 600;
      color: var(--color-primary);
      &:hover { text-decoration: underline; }
    }

    .detail-actions {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
      margin-top: 1rem;
    }
  `]
})
export class ActiviteDetailComponent implements OnInit {
  activite?: Activite;
  isFavori = false;
  loading = false;
  favoriLoading = false;
  error = '';

  private catColors: Record<string, string> = {
    'Culture': 'linear-gradient(135deg,#a18cd1,#fbc2eb)',
    'Histoire': 'linear-gradient(135deg,#f6d365,#fda085)',
    'Nature': 'linear-gradient(135deg,#43e97b,#38f9d7)',
    'Gastronomie': 'linear-gradient(135deg,#f093fb,#f5576c)',
    'Aventure': 'linear-gradient(135deg,#4facfe,#00f2fe)',
    'Détente': 'linear-gradient(135deg,#a1c4fd,#c2e9fb)',
    'Sport': 'linear-gradient(135deg,#f77062,#fe5196)',
    'Shopping': 'linear-gradient(135deg,#fccb90,#d57eeb)',
  };

  private catEmojis: Record<string, string> = {
    'Culture': '🎨', 'Histoire': '🏛️', 'Nature': '🌿', 'Gastronomie': '🍽️',
    'Aventure': '🪂', 'Détente': '🌅', 'Sport': '🏄', 'Shopping': '🛍️',
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private activiteService: ActiviteService,
    private favoriService: FavoriService
  ) {}

  ngOnInit(): void {
    this.loading = true;
    this.route.paramMap.pipe(
      switchMap(p => this.activiteService.getById(Number(p.get('id'))))
    ).subscribe({
      next: a => { this.activite = a; this.loading = false; this.checkFavori(); },
      error: err => {
        if (err.status === 404) this.router.navigate(['/activites']);
        else this.error = 'Erreur lors du chargement.';
        this.loading = false;
      }
    });
  }

  private checkFavori(): void {
    if (this.activite)
      this.favoriService.estFavori(this.activite.id).subscribe({ next: v => this.isFavori = v, error: () => {} });
  }

  ajouterFavori(): void {
    if (!this.activite) return;
    this.favoriLoading = true;
    this.favoriService.ajouter(this.activite.id).subscribe({
      next: () => { this.isFavori = true; this.favoriLoading = false; },
      error: () => this.favoriLoading = false
    });
  }

  retirerFavori(): void {
    if (!this.activite) return;
    this.favoriLoading = true;
    this.favoriService.retirer(this.activite.id).subscribe({
      next: () => { this.isFavori = false; this.favoriLoading = false; },
      error: () => this.favoriLoading = false
    });
  }

  getCatColor(cat: string): string { return this.catColors[cat] ?? 'linear-gradient(135deg,#667eea,#764ba2)'; }
  getCatEmoji(cat: string): string { return this.catEmojis[cat] ?? '🎯'; }

  formatDuree(min: number): string {
    if (min < 60) return `${min} min`;
    const h = Math.floor(min / 60);
    const m = min % 60;
    return m > 0 ? `${h}h${m.toString().padStart(2,'0')}` : `${h}h`;
  }
}
