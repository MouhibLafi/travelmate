import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

import { FavoriService } from '../services/favori.service';
import { Activite } from '../models/activite.model';

@Component({
  selector: 'app-favoris',
  standalone: true,
  host: { class: 'm3' },   // integration : styles du module 3 limites a ses pages (styles/_module3.scss)
  imports: [CommonModule, RouterModule],
  template: `
    <div class="page">
      <div class="section-header">
        <div class="section-title">
          <h1>♥ Mes Favoris</h1>
          <p>{{ favoris.length }} activité{{ favoris.length > 1 ? 's' : '' }} sauvegardée{{ favoris.length > 1 ? 's' : '' }}</p>
        </div>
      </div>

      <div *ngIf="loading" class="loading-spinner">Chargement de vos favoris…</div>
      <div *ngIf="error" class="error-message">⚠ {{ error }}</div>

      <!-- Empty state -->
      <div *ngIf="!loading && !error && favoris.length === 0" class="fav-empty">
        <div class="fav-empty__heart">♡</div>
        <h2>Aucun favori pour l'instant</h2>
        <p>Explorez nos destinations et ajoutez des activités à vos favoris pour les retrouver ici.</p>
        <div class="fav-empty__actions">
          <a routerLink="/activites" class="btn btn--primary">Explorer les activités</a>
          <a routerLink="/recommandations" class="btn btn--secondary">✨ Obtenir des recommandations</a>
        </div>
      </div>

      <!-- Grid favoris -->
      <div *ngIf="!loading && !error && favoris.length > 0" class="grid-cards">
        <div *ngFor="let act of favoris" class="fav-card card">
          <!-- Header coloré -->
          <div class="fav-card__header" [style.background]="getCatColor(act.categorie.nom)">
            <span class="fav-card__emoji">{{ getCatEmoji(act.categorie.nom) }}</span>
            <button
              class="fav-card__remove"
              (click)="retirer(act)"
              title="Retirer des favoris"
              aria-label="Retirer des favoris"
            >✕</button>
          </div>

          <!-- Body -->
          <div class="fav-card__body">
            <span class="badge">{{ act.categorie.nom }}</span>
            <h3>
              <a [routerLink]="['/activites', act.id]">{{ act.nom }}</a>
            </h3>
            <p class="fav-card__dest">📍 {{ act.destination.nom }}, {{ act.destination.pays }}</p>
            <p class="fav-card__desc">{{ act.description }}</p>

            <div class="fav-card__tags" *ngIf="act.tags">
              <span *ngFor="let tag of act.tags.split(',').slice(0,3)" class="tag">{{ tag.trim() }}</span>
            </div>
          </div>

          <!-- Footer -->
          <div class="fav-card__footer">
            <div class="fav-card__meta">
              <span class="fav-price" [class.fav-price--free]="act.prix === 0">
                {{ act.prix === 0 ? 'Gratuit' : (act.prix | number:'1.0-0') + ' €' }}
              </span>
              <span class="fav-duree">🕐 {{ formatDuree(act.dureeMinutes) }}</span>
            </div>
            <a [routerLink]="['/activites', act.id]" class="btn btn--secondary btn--sm">Voir →</a>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    /* Empty state */
    .fav-empty {
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;
      padding: 4rem 2rem;
      gap: 1rem;

      h2 { font-size: 1.375rem; }
      p { color: var(--color-text-secondary); max-width: 400px; font-size: 0.95rem; }
    }

    .fav-empty__heart {
      font-size: 4rem;
      color: var(--color-border-strong);
      line-height: 1;
    }

    .fav-empty__actions {
      display: flex;
      gap: 1rem;
      flex-wrap: wrap;
      justify-content: center;
      margin-top: 0.5rem;
    }

    /* Fav card */
    .fav-card {
      display: flex;
      flex-direction: column;
      padding: 0;
      overflow: hidden;
      transition: transform var(--transition), box-shadow var(--transition);
      &:hover { transform: translateY(-4px); box-shadow: var(--shadow-card-hover); }
    }

    .fav-card__header {
      height: 120px;
      display: flex;
      align-items: center;
      justify-content: center;
      position: relative;
    }

    .fav-card__emoji { font-size: 3rem; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.15)); }

    .fav-card__remove {
      position: absolute;
      top: 0.625rem;
      right: 0.625rem;
      width: 28px;
      height: 28px;
      border-radius: 50%;
      border: none;
      background: rgba(0,0,0,0.3);
      color: white;
      font-size: 0.8rem;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: background var(--transition);
      &:hover { background: rgba(220,38,38,0.8); }
    }

    .fav-card__body {
      padding: 1rem 1.25rem 0.75rem;
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 0.4rem;
    }

    .fav-card__body h3 a {
      color: var(--color-text);
      font-size: 1rem;
      &:hover { color: var(--color-primary); text-decoration: none; }
    }

    .fav-card__dest { font-size: 0.78rem; color: var(--color-text-secondary); }

    .fav-card__desc {
      font-size: 0.82rem;
      color: var(--color-text-secondary);
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .fav-card__tags { display: flex; flex-wrap: wrap; gap: 0.25rem; }

    .fav-card__footer {
      padding: 0.75rem 1.25rem;
      border-top: 1px solid var(--color-border);
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .fav-card__meta { display: flex; align-items: center; gap: 1rem; }

    .fav-price { font-weight: 700; font-size: 0.95rem; color: var(--color-primary-dark); }
    .fav-price--free { color: var(--color-success); }
    .fav-duree { font-size: 0.8rem; color: var(--color-text-secondary); }
  `]
})
export class FavorisComponent implements OnInit {
  favoris: Activite[] = [];
  loading = false;
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

  constructor(private favoriService: FavoriService) {}

  ngOnInit(): void { this.charger(); }

  charger(): void {
    this.loading = true;
    this.error = '';
    this.favoriService.getFavoris().subscribe({
      next: d => { this.favoris = d; this.loading = false; },
      error: () => { this.error = 'Erreur lors du chargement.'; this.loading = false; }
    });
  }

  retirer(act: Activite): void {
    this.favoriService.retirer(act.id).subscribe({
      next: () => { this.favoris = this.favoris.filter(f => f.id !== act.id); },
      error: () => {}
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
