import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { ActiviteService, ActiviteFiltres } from '../services/activite.service';
import { DestinationService } from '../services/destination.service';
import { Activite } from '../models/activite.model';
import { Categorie } from '../models/categorie.model';
import { Destination } from '../models/destination.model';

@Component({
  selector: 'app-activites',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="page">
      <div class="section-header">
        <div class="section-title">
          <h1>Activités</h1>
          <p>{{ activites.length }} activité{{ activites.length > 1 ? 's' : '' }} disponible{{ activites.length > 1 ? 's' : '' }}</p>
        </div>
        <a routerLink="/recommandations" class="btn btn--primary">
          ✨ Recommandations IA
        </a>
      </div>

      <div class="activites-layout">
        <!-- Sidebar filtres -->
        <aside class="filters-sidebar card">
          <div class="filters-sidebar__header">
            <h3>Filtres</h3>
            <button class="btn btn--ghost btn--sm" (click)="reinitialiser()">Réinitialiser</button>
          </div>

          <div class="filter-group">
            <label>Recherche</label>
            <div class="search-mini">
              <span>🔍</span>
              <input type="text" placeholder="Nom, description…" [(ngModel)]="filtres.q" (keyup.enter)="charger()" />
            </div>
          </div>

          <div class="filter-group">
            <label>Destination</label>
            <select [(ngModel)]="filtres.destinationId">
              <option [ngValue]="undefined">Toutes les destinations</option>
              <option *ngFor="let d of destinations" [ngValue]="d.id">{{ d.nom }}</option>
            </select>
          </div>

          <div class="filter-group">
            <label>Catégorie</label>
            <div class="category-chips">
              <button
                class="chip"
                [class.chip--active]="!filtres.categorie"
                (click)="filtres.categorie = ''; charger()"
              >Toutes</button>
              <button
                *ngFor="let c of categories"
                class="chip"
                [class.chip--active]="filtres.categorie === c.nom"
                (click)="filtres.categorie = c.nom; charger()"
              >{{ c.nom }}</button>
            </div>
          </div>

          <div class="filter-group">
            <label>Prix (€)</label>
            <div class="range-inputs">
              <input type="number" placeholder="Min" min="0" [(ngModel)]="filtres.prixMin" />
              <span class="range-sep">—</span>
              <input type="number" placeholder="Max" min="0" [(ngModel)]="filtres.prixMax" />
            </div>
          </div>

          <div class="filter-group">
            <label>Durée max (min)</label>
            <input type="number" placeholder="Ex: 180" min="0" [(ngModel)]="filtres.dureeMax" />
          </div>

          <div class="filter-group">
            <label>Trier par</label>
            <select [(ngModel)]="filtres.tri">
              <option value="nom">Nom (A → Z)</option>
              <option value="prix">Prix (croissant)</option>
              <option value="duree">Durée (croissante)</option>
            </select>
          </div>

          <button class="btn btn--primary" style="width:100%;margin-top:0.5rem" (click)="charger()">
            Appliquer les filtres
          </button>
        </aside>

        <!-- Liste activités -->
        <div class="activites-content">
          <div *ngIf="loading" class="loading-spinner">Chargement…</div>
          <div *ngIf="error" class="error-message">⚠ {{ error }}</div>

          <div *ngIf="!loading && !error && activites.length === 0" class="empty-state">
            <div class="empty-icon">🎭</div>
            <strong>Aucune activité trouvée</strong>
            <p>Modifiez vos filtres pour voir plus de résultats.</p>
          </div>

          <div *ngIf="!loading && !error && activites.length > 0" class="activites-list">
            <a
              *ngFor="let act of activites"
              class="activite-item card card--clickable"
              [routerLink]="['/activites', act.id]"
            >
              <div class="activite-item__img" [style.background]="getCatColor(act.categorie.nom)">
                <span>{{ getCatEmoji(act.categorie.nom) }}</span>
              </div>
              <div class="activite-item__body">
                <div class="activite-item__top">
                  <span class="badge">{{ act.categorie.nom }}</span>
                  <span class="activite-item__dest">📍 {{ act.destination.nom }}</span>
                </div>
                <h3 class="activite-item__name">{{ act.nom }}</h3>
                <p class="activite-item__desc">{{ act.description }}</p>
                <div class="activite-item__tags" *ngIf="act.tags">
                  <span *ngFor="let tag of act.tags.split(',').slice(0,3)" class="tag">{{ tag.trim() }}</span>
                </div>
              </div>
              <div class="activite-item__meta">
                <div class="meta-pill" [class.meta-pill--free]="act.prix === 0">
                  <span class="meta-pill__label">Prix</span>
                  <span class="meta-pill__value">{{ act.prix === 0 ? 'Gratuit' : (act.prix | number:'1.0-0') + ' €' }}</span>
                </div>
                <div class="meta-pill">
                  <span class="meta-pill__label">Durée</span>
                  <span class="meta-pill__value">{{ formatDuree(act.dureeMinutes) }}</span>
                </div>
              </div>
            </a>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .activites-layout {
      display: grid;
      grid-template-columns: 260px 1fr;
      gap: 1.5rem;
      align-items: start;
    }

    /* Sidebar */
    .filters-sidebar {
      position: sticky;
      top: 88px;
      display: flex;
      flex-direction: column;
      gap: 1.25rem;
      padding: 1.25rem;
    }

    .filters-sidebar__header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      h3 { font-size: 0.95rem; }
    }

    .filter-group {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;

      label {
        font-size: 0.75rem;
        font-weight: 600;
        color: var(--color-text-secondary);
        text-transform: uppercase;
        letter-spacing: 0.04em;
      }
    }

    .search-mini {
      position: relative;
      span {
        position: absolute;
        left: 0.75rem;
        top: 50%;
        transform: translateY(-50%);
        font-size: 0.9rem;
        pointer-events: none;
      }
      input { padding-left: 2.25rem; }
    }

    .category-chips {
      display: flex;
      flex-wrap: wrap;
      gap: 0.4rem;
    }

    .chip {
      padding: 0.25rem 0.75rem;
      border-radius: var(--border-radius-full);
      border: 1.5px solid var(--color-border);
      background: var(--color-surface);
      font-size: 0.78rem;
      font-weight: 500;
      cursor: pointer;
      transition: all var(--transition);
      color: var(--color-text-secondary);

      &:hover { border-color: var(--color-primary); color: var(--color-primary); }

      &--active {
        background: var(--color-primary);
        border-color: var(--color-primary);
        color: white;
      }
    }

    .range-inputs {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      .range-sep { color: var(--color-text-muted); font-size: 0.9rem; flex-shrink: 0; }
      input { flex: 1; min-width: 0; }
    }

    /* Activite list */
    .activites-list {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .activite-item {
      display: grid;
      grid-template-columns: 80px 1fr auto;
      gap: 1.25rem;
      align-items: center;
      text-decoration: none;
      color: inherit;
      padding: 1rem 1.25rem;
      transition: transform var(--transition), box-shadow var(--transition);

      &:hover { transform: translateX(4px); text-decoration: none; }
    }

    .activite-item__img {
      width: 80px;
      height: 80px;
      border-radius: var(--border-radius);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 2rem;
      flex-shrink: 0;
    }

    .activite-item__body { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 0.35rem; }

    .activite-item__top {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }

    .activite-item__dest { font-size: 0.78rem; color: var(--color-text-secondary); }

    .activite-item__name { font-size: 1rem; font-weight: 600; }

    .activite-item__desc {
      font-size: 0.82rem;
      color: var(--color-text-secondary);
      display: -webkit-box;
      -webkit-line-clamp: 1;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .activite-item__tags { display: flex; flex-wrap: wrap; gap: 0.25rem; }

    .activite-item__meta {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      align-items: flex-end;
      flex-shrink: 0;
    }

    .meta-pill {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      gap: 0.1rem;

      &__label { font-size: 0.65rem; color: var(--color-text-muted); text-transform: uppercase; letter-spacing: 0.04em; }
      &__value { font-size: 0.95rem; font-weight: 700; color: var(--color-primary-dark); }
      &--free .meta-pill__value { color: var(--color-success); }
    }
  `]
})
export class ActivitesComponent implements OnInit {
  activites: Activite[] = [];
  categories: Categorie[] = [];
  destinations: Destination[] = [];
  loading = false;
  error = '';
  filtres: ActiviteFiltres = { tri: 'nom' };

  private catColors: Record<string, string> = {
    'Culture':     'linear-gradient(135deg,#a18cd1,#fbc2eb)',
    'Histoire':    'linear-gradient(135deg,#f6d365,#fda085)',
    'Nature':      'linear-gradient(135deg,#43e97b,#38f9d7)',
    'Gastronomie': 'linear-gradient(135deg,#f093fb,#f5576c)',
    'Aventure':    'linear-gradient(135deg,#4facfe,#00f2fe)',
    'Détente':     'linear-gradient(135deg,#a1c4fd,#c2e9fb)',
    'Sport':       'linear-gradient(135deg,#f77062,#fe5196)',
    'Shopping':    'linear-gradient(135deg,#fccb90,#d57eeb)',
  };

  private catEmojis: Record<string, string> = {
    'Culture': '🎨', 'Histoire': '🏛️', 'Nature': '🌿', 'Gastronomie': '🍽️',
    'Aventure': '🪂', 'Détente': '🌅', 'Sport': '🏄', 'Shopping': '🛍️',
  };

  constructor(private activiteService: ActiviteService, private destinationService: DestinationService) {}

  ngOnInit(): void {
    this.activiteService.getCategories().subscribe(d => this.categories = d);
    this.destinationService.getAll().subscribe(d => this.destinations = d);
    this.charger();
  }

  charger(): void {
    this.loading = true;
    this.error = '';
    const f: ActiviteFiltres = { ...this.filtres };
    if (!f.categorie) delete f.categorie;
    this.activiteService.getAll(f).subscribe({
      next: d => { this.activites = d; this.loading = false; },
      error: () => { this.error = 'Erreur lors du chargement.'; this.loading = false; }
    });
  }

  reinitialiser(): void { this.filtres = { tri: 'nom' }; this.charger(); }

  getCatColor(cat: string): string { return this.catColors[cat] ?? 'linear-gradient(135deg,#667eea,#764ba2)'; }
  getCatEmoji(cat: string): string { return this.catEmojis[cat] ?? '🎯'; }

  formatDuree(min: number): string {
    if (min < 60) return `${min} min`;
    const h = Math.floor(min / 60);
    const m = min % 60;
    return m > 0 ? `${h}h${m.toString().padStart(2,'0')}` : `${h}h`;
  }
}
