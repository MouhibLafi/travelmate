import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { debounceTime, distinctUntilChanged, Subject, switchMap } from 'rxjs';

import { DestinationService } from '../services/destination.service';
import { Destination } from '../models/destination.model';

@Component({
  selector: 'app-destinations',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="page">
      <!-- Hero -->
      <div class="hero-banner">
        <h1>Explorez le monde</h1>
        <p>Découvrez nos destinations et trouvez l'aventure qui vous correspond.</p>
      </div>

      <!-- Search -->
      <div class="search-row">
        <div class="search-box">
          <span class="search-icon">🔍</span>
          <input
            type="text"
            placeholder="Rechercher une destination, un pays, une thématique…"
            [(ngModel)]="searchTerm"
            (ngModelChange)="onSearch($event)"
          />
          <button *ngIf="searchTerm" class="search-clear" (click)="clearSearch()">✕</button>
        </div>
      </div>

      <!-- Results info -->
      <div *ngIf="!loading && !error" class="results-info">
        <span>{{ destinations.length }} destination{{ destinations.length > 1 ? 's' : '' }}</span>
        <span *ngIf="searchTerm" class="search-label">pour « {{ searchTerm }} »</span>
      </div>

      <!-- Loading -->
      <div *ngIf="loading" class="loading-spinner">Chargement des destinations…</div>

      <!-- Error -->
      <div *ngIf="error" class="error-message">⚠ {{ error }}</div>

      <!-- Empty -->
      <div *ngIf="!loading && !error && destinations.length === 0" class="empty-state">
        <div class="empty-icon">🗺️</div>
        <strong>Aucune destination trouvée</strong>
        <p>Essayez un autre terme ou explorez toutes nos destinations.</p>
        <button class="btn btn--secondary" (click)="clearSearch()">Voir toutes les destinations</button>
      </div>

      <!-- Grid -->
      <div *ngIf="!loading && !error && destinations.length > 0" class="grid-cards">
        <a
          *ngFor="let dest of destinations"
          class="card dest-card card--clickable"
          [routerLink]="['/destinations', dest.id]"
        >
          <!-- Image placeholder -->
          <div class="dest-card__image" [style.background]="getGradient(dest)">
            <span class="dest-card__emoji">{{ getEmoji(dest) }}</span>
          </div>

          <div class="dest-card__body">
            <div class="dest-card__header">
              <h3>{{ dest.nom }}</h3>
              <span class="dest-card__pays">{{ getFlagEmoji(dest.pays) }} {{ dest.pays }}</span>
            </div>
            <p class="dest-card__desc">{{ dest.description }}</p>
            <div class="dest-card__tags" *ngIf="dest.tags">
              <span *ngFor="let tag of dest.tags.split(',').slice(0,3)" class="tag">{{ tag.trim() }}</span>
            </div>
          </div>

          <div class="dest-card__footer">
            <span class="dest-card__cta">Voir les activités →</span>
          </div>
        </a>
      </div>
    </div>
  `,
  styles: [`
    /* Search */
    .search-row {
      margin-bottom: 1.5rem;
    }

    .search-box {
      position: relative;
      max-width: 560px;

      .search-icon {
        position: absolute;
        left: 1rem;
        top: 50%;
        transform: translateY(-50%);
        font-size: 1rem;
        pointer-events: none;
      }

      input {
        padding-left: 2.75rem;
        padding-right: 2.5rem;
        height: 48px;
        border-radius: var(--border-radius-full);
        border: 1.5px solid var(--color-border);
        box-shadow: var(--shadow-sm);
        font-size: 0.95rem;

        &:focus {
          border-color: var(--color-primary);
          box-shadow: 0 0 0 3px rgba(10,124,110,0.12);
        }
      }

      .search-clear {
        position: absolute;
        right: 0.875rem;
        top: 50%;
        transform: translateY(-50%);
        background: none;
        border: none;
        cursor: pointer;
        color: var(--color-text-muted);
        font-size: 0.875rem;
        padding: 0.2rem;
        &:hover { color: var(--color-text); }
      }
    }

    .results-info {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 0.875rem;
      color: var(--color-text-secondary);
      margin-bottom: 1.25rem;
      font-weight: 500;

      .search-label { color: var(--color-text-muted); }
    }

    /* Destination card */
    .dest-card {
      display: flex;
      flex-direction: column;
      text-decoration: none;
      color: inherit;
      padding: 0;
      overflow: hidden;
      transition: transform var(--transition), box-shadow var(--transition);

      &:hover {
        transform: translateY(-4px);
        box-shadow: var(--shadow-card-hover);
        text-decoration: none;
      }
    }

    .dest-card__image {
      height: 160px;
      display: flex;
      align-items: center;
      justify-content: center;
      position: relative;
    }

    .dest-card__emoji {
      font-size: 4rem;
      filter: drop-shadow(0 2px 4px rgba(0,0,0,0.15));
    }

    .dest-card__body {
      padding: 1.25rem 1.25rem 0.75rem;
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    .dest-card__header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 0.5rem;

      h3 { font-size: 1.15rem; }
    }

    .dest-card__pays {
      font-size: 0.8rem;
      color: var(--color-text-secondary);
      white-space: nowrap;
    }

    .dest-card__desc {
      font-size: 0.875rem;
      color: var(--color-text-secondary);
      line-height: 1.5;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .dest-card__tags {
      display: flex;
      flex-wrap: wrap;
      gap: 0.3rem;
    }

    .dest-card__footer {
      padding: 0.75rem 1.25rem 1rem;
      border-top: 1px solid var(--color-border);
      margin-top: 0.5rem;
    }

    .dest-card__cta {
      font-size: 0.85rem;
      font-weight: 600;
      color: var(--color-primary);
    }
  `]
})
export class DestinationsComponent implements OnInit {
  destinations: Destination[] = [];
  searchTerm = '';
  loading = false;
  error = '';

  private search$ = new Subject<string>();

  private gradients: Record<string, string> = {
    'Italie':    'linear-gradient(135deg, #f6d365 0%, #fda085 100%)',
    'France':    'linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%)',
    'Tunisie':   'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
    'Portugal':  'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
    'Suisse':    'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
  };

  private emojis: Record<string, string> = {
    'Rome': '🏛️', 'Paris': '🗼', 'Tunis': '🕌',
    'Lisbonne': '🚋', 'Interlaken': '⛰️',
  };

  private flags: Record<string, string> = {
    'Italie': '🇮🇹', 'France': '🇫🇷', 'Tunisie': '🇹🇳',
    'Portugal': '🇵🇹', 'Suisse': '🇨🇭',
  };

  constructor(private destinationService: DestinationService) {}

  ngOnInit(): void {
    this.search$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(term => {
        this.loading = true;
        this.error = '';
        return this.destinationService.getAll(term || undefined);
      })
    ).subscribe({
      next: data => { this.destinations = data; this.loading = false; },
      error: () => { this.error = 'Impossible de charger les destinations.'; this.loading = false; }
    });
    this.search$.next('');
  }

  onSearch(term: string): void { this.search$.next(term); }

  clearSearch(): void {
    this.searchTerm = '';
    this.search$.next('');
  }

  getGradient(dest: Destination): string {
    return this.gradients[dest.pays] ?? 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)';
  }

  getEmoji(dest: Destination): string {
    return this.emojis[dest.nom] ?? '🌍';
  }

  getFlagEmoji(pays: string): string {
    return this.flags[pays] ?? '🌐';
  }
}
