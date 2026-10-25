import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { getErrorMessage } from '../services/api';
import { AuthService } from '../services/auth.service';
import { FavoriService } from './favori.service';
import { IaService } from './ia.service';
import { AnalyseAvis, Avis, Restaurant, Sentiment } from './restaurant.model';
import { RestaurantService } from './restaurant.service';

@Component({
  selector: 'tm-detail',
  imports: [RouterLink, DecimalPipe],
  templateUrl: './detail.component.html',
  styleUrls: ['./detail.component.css', './restaurants.css']
})
export class DetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(RestaurantService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly auth = inject(AuthService);
  protected readonly favoris = inject(FavoriService);
  protected readonly ia = inject(IaService);

  protected readonly restaurant = signal<Restaurant | null>(null);
  protected readonly avis = signal<Avis[]>([]);
  protected readonly analyse = signal<AnalyseAvis | null>(null);
  protected readonly chargement = signal(true);
  protected readonly erreur = signal('');
  protected readonly id = signal(0);

  protected readonly note = signal(5);
  protected readonly commentaire = signal('');
  protected readonly envoi = signal(false);
  protected readonly erreurFormulaire = signal('');
  protected readonly succes = signal('');

  constructor() {
    this.route.paramMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      const id = Number(params.get('id'));
      if (!Number.isFinite(id) || id <= 0) {
        this.erreur.set('Restaurant introuvable.');
        this.chargement.set(false);
        return;
      }
      this.id.set(id);
      this.charger(id);
    });
  }

  protected charger(id: number): void {
    this.chargement.set(true);
    this.erreur.set('');

    this.service
      .detail(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (restaurant) => {
          this.restaurant.set(restaurant);
          this.chargement.set(false);
        },
        error: (erreur: HttpErrorResponse) => {
          this.erreur.set(getErrorMessage(erreur));
          this.chargement.set(false);
        }
      });

    this.chargerAvis(id);
    this.chargerAnalyse(id);
  }

  protected envoyerAvis(event: Event): void {
    event.preventDefault();
    this.erreurFormulaire.set('');
    this.succes.set('');

    const commentaire = this.commentaire().trim();

    if (commentaire.length < 3) {
      this.erreurFormulaire.set('Merci de rédiger un commentaire de 3 caractères minimum.');
      return;
    }

    this.envoi.set(true);
    this.service
      .ajouterAvis(this.id(), { note: this.note(), commentaire })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (avis) => {
          this.envoi.set(false);
          this.note.set(5);
          this.commentaire.set('');
          this.succes.set(
            `Merci ${avis.auteur} ! Votre avis a été analysé : ${this.libelleSentiment(
              avis.sentiment
            ).toLowerCase()} (${avis.sourceSentiment === 'OLLAMA' ? 'Llama 3' : 'moteur local'}).`
          );
          this.chargerAvis(this.id());
          this.chargerAnalyse(this.id());
          this.service
            .detail(this.id())
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((restaurant) => this.restaurant.set(restaurant));
        },
        error: (erreur: HttpErrorResponse) => {
          this.envoi.set(false);
          this.erreurFormulaire.set(getErrorMessage(erreur));
        }
      });
  }

  protected libelleSentiment(sentiment: Sentiment): string {
    switch (sentiment) {
      case 'POSITIF':
        return 'Positif';
      case 'NEGATIF':
        return 'Négatif';
      default:
        return 'Neutre';
    }
  }

  protected classeSentiment(sentiment: Sentiment): string {
    switch (sentiment) {
      case 'POSITIF':
        return 'badge badge-success';
      case 'NEGATIF':
        return 'badge badge-error';
      default:
        return 'badge badge-muted';
    }
  }

  protected dateLisible(iso: string): string {
    return new Intl.DateTimeFormat('fr-FR', {
      day: 'numeric',
      month: 'long',
      year: 'numeric'
    }).format(new Date(iso));
  }

  protected pourcentage(valeur: number): string {
    return `${Math.round(valeur)} %`;
  }

  private chargerAvis(id: number): void {
    this.service
      .avis(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (avis) => this.avis.set(avis),
        error: () => this.avis.set([])
      });
  }

  private chargerAnalyse(id: number): void {
    this.service
      .analyse(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (analyse) => this.analyse.set(analyse),
        error: () => this.analyse.set(null)
      });
  }
}
