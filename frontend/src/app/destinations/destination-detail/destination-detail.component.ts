import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { switchMap } from 'rxjs';

import { DestinationService } from '../../services/destination.service';
import { ActiviteService } from '../../services/activite.service';
import { Destination } from '../../models/destination.model';
import { Activite } from '../../models/activite.model';

@Component({
  selector: 'app-destination-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="page">
      <a routerLink="/destinations" class="back-link">← Retour aux destinations</a>

      <div *ngIf="loading" class="loading-spinner">Chargement…</div>
      <div *ngIf="error" class="error-message">⚠ {{ error }}</div>

      <div *ngIf="destination && !loading">
        <!-- Hero destination -->
        <div class="dest-hero" [style.background]="getGradient(destination)">
          <div class="dest-hero__emoji">{{ getEmoji(destination) }}</div>
          <div class="dest-hero__info">
            <span class="dest-hero__flag">{{ getFlag(destination.pays) }}</span>
            <h1>{{ destination.nom }}</h1>
            <p>{{ destination.pays }}</p>
          </div>
        </div>

        <!-- Description + tags -->
        <div class="dest-details card">
          <p class="dest-desc">{{ destination.description }}</p>
          <div *ngIf="destination.tags" class="dest-tags">
            <span *ngFor="let tag of destination.tags.split(',')" class="tag">{{ tag.trim() }}</span>
          </div>
        </div>

        <!-- Activités -->
        <div class="activites-section">
          <div class="section-header">
            <div class="section-title">
              <h2>Activités à {{ destination.nom }}</h2>
              <p>{{ activites.length }} activité{{ activites.length > 1 ? 's' : '' }} disponible{{ activites.length > 1 ? 's' : '' }}</p>
            </div>
            <a [routerLink]="['/recommandations']" class="btn btn--secondary btn--sm">✨ Recommandations IA</a>
          </div>

          <div *ngIf="activites.length === 0" class="empty-state">
            <div class="empty-icon">🎭</div>
            <strong>Aucune activité disponible</strong>
          </div>

          <div class="grid-cards">
            <a
              *ngFor="let act of activites"
              class="act-card card card--clickable"
              [routerLink]="['/activites', act.id]"
            >
              <div class="act-card__img" [style.background]="getCatColor(act.categorie.nom)">
                <span>{{ getCatEmoji(act.categorie.nom) }}</span>
              </div>
              <div class="act-card__body">
                <span class="badge">{{ act.categorie.nom }}</span>
                <h3>{{ act.nom }}</h3>
                <p>{{ act.description }}</p>
                <div class="act-card__footer">
                  <span class="act-price" [class.act-price--free]="act.prix === 0">
                    {{ act.prix === 0 ? 'Gratuit' : (act.prix | number:'1.0-0') + ' €' }}
                  </span>
                  <span class="act-duree">🕐 {{ formatDuree(act.dureeMinutes) }}</span>
                </div>
              </div>
            </a>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    /* Hero */
    .dest-hero {
      border-radius: var(--border-radius-lg);
      height: 220px;
      display: flex;
      align-items: center;
      padding: 2rem 2.5rem;
      gap: 2rem;
      margin-bottom: 1.25rem;
      position: relative;
      overflow: hidden;

      &::after {
        content: '';
        position: absolute;
        inset: 0;
        background: rgba(0,0,0,0.2);
        border-radius: var(--border-radius-lg);
      }
    }

    .dest-hero__emoji {
      font-size: 5rem;
      filter: drop-shadow(0 4px 8px rgba(0,0,0,0.2));
      position: relative;
      z-index: 1;
    }

    .dest-hero__info {
      position: relative;
      z-index: 1;
      color: white;

      h1 { color: white; font-size: 2.25rem; margin-bottom: 0.2rem; }
      p { font-size: 1rem; opacity: 0.9; }
    }

    .dest-hero__flag { font-size: 1.75rem; display: block; margin-bottom: 0.3rem; }

    /* Details */
    .dest-details {
      margin-bottom: 2rem;
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }

    .dest-desc { color: var(--color-text-secondary); line-height: 1.7; font-size: 0.95rem; }
    .dest-tags { display: flex; flex-wrap: wrap; gap: 0.4rem; }

    /* Act card */
    .activites-section { margin-top: 0.5rem; }

    .act-card {
      display: flex;
      flex-direction: column;
      padding: 0;
      overflow: hidden;
      text-decoration: none;
      color: inherit;
    }

    .act-card__img {
      height: 120px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 3rem;
    }

    .act-card__body {
      padding: 1rem;
      display: flex;
      flex-direction: column;
      gap: 0.4rem;

      h3 { font-size: 1rem; }
      p { font-size: 0.82rem; color: var(--color-text-secondary); display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
    }

    .act-card__footer {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-top: 0.25rem;
    }

    .act-price { font-weight: 700; color: var(--color-primary-dark); font-size: 0.95rem; }
    .act-price--free { color: var(--color-success); }
    .act-duree { font-size: 0.8rem; color: var(--color-text-secondary); }
  `]
})
export class DestinationDetailComponent implements OnInit {
  destination?: Destination;
  activites: Activite[] = [];
  loading = false;
  error = '';

  private gradients: Record<string, string> = {
    'Italie': 'linear-gradient(135deg, #f6d365, #fda085)',
    'France': 'linear-gradient(135deg, #a18cd1, #fbc2eb)',
    'Tunisie': 'linear-gradient(135deg, #f093fb, #f5576c)',
    'Portugal': 'linear-gradient(135deg, #4facfe, #00f2fe)',
    'Suisse': 'linear-gradient(135deg, #43e97b, #38f9d7)',
  };
  private emojis: Record<string, string> = { 'Rome':'🏛️','Paris':'🗼','Tunis':'🕌','Lisbonne':'🚋','Interlaken':'⛰️' };
  private flags: Record<string, string> = { 'Italie':'🇮🇹','France':'🇫🇷','Tunisie':'🇹🇳','Portugal':'🇵🇹','Suisse':'🇨🇭' };
  private catColors: Record<string, string> = {
    'Culture':'linear-gradient(135deg,#a18cd1,#fbc2eb)', 'Histoire':'linear-gradient(135deg,#f6d365,#fda085)',
    'Nature':'linear-gradient(135deg,#43e97b,#38f9d7)', 'Gastronomie':'linear-gradient(135deg,#f093fb,#f5576c)',
    'Aventure':'linear-gradient(135deg,#4facfe,#00f2fe)', 'Détente':'linear-gradient(135deg,#a1c4fd,#c2e9fb)',
    'Sport':'linear-gradient(135deg,#f77062,#fe5196)', 'Shopping':'linear-gradient(135deg,#fccb90,#d57eeb)',
  };
  private catEmojis: Record<string, string> = {
    'Culture':'🎨','Histoire':'🏛️','Nature':'🌿','Gastronomie':'🍽️',
    'Aventure':'🪂','Détente':'🌅','Sport':'🏄','Shopping':'🛍️',
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private destinationService: DestinationService,
    private activiteService: ActiviteService
  ) {}

  ngOnInit(): void {
    this.loading = true;
    this.route.paramMap.pipe(
      switchMap(p => this.destinationService.getById(Number(p.get('id'))))
    ).subscribe({
      next: d => {
        this.destination = d;
        this.activiteService.getAll({ destinationId: d.id }).subscribe({
          next: a => { this.activites = a; this.loading = false; },
          error: () => this.loading = false
        });
      },
      error: err => {
        if (err.status === 404) this.router.navigate(['/destinations']);
        else this.error = 'Erreur lors du chargement.';
        this.loading = false;
      }
    });
  }

  getGradient(d: Destination): string { return this.gradients[d.pays] ?? 'linear-gradient(135deg,#667eea,#764ba2)'; }
  getEmoji(d: Destination): string { return this.emojis[d.nom] ?? '🌍'; }
  getFlag(pays: string): string { return this.flags[pays] ?? '🌐'; }
  getCatColor(cat: string): string { return this.catColors[cat] ?? 'linear-gradient(135deg,#667eea,#764ba2)'; }
  getCatEmoji(cat: string): string { return this.catEmojis[cat] ?? '🎯'; }

  formatDuree(min: number): string {
    if (min < 60) return `${min} min`;
    const h = Math.floor(min / 60);
    const m = min % 60;
    return m > 0 ? `${h}h${m.toString().padStart(2,'0')}` : `${h}h`;
  }
}
