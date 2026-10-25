import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';

import { getErrorMessage } from '../services/api';
import { UserService } from '../services/user.service';
import { IaService } from './ia.service';
import { MetaFiltres, Recommandation } from './restaurant.model';
import { RestaurantService } from './restaurant.service';

// Budget par defaut issu du profil (meme logique que le backend)
const BUDGETS: Record<string, number> = {
  ECONOMIQUE: 20,
  MOYEN: 35,
  LUXE: 60
};

@Component({
  selector: 'tm-recommandations',
  imports: [RouterLink],
  templateUrl: './recommandations.component.html',
  styleUrls: ['./recommandations.component.css', './restaurants.css']
})
export class RecommandationsComponent {
  private readonly service = inject(RestaurantService);
  private readonly userService = inject(UserService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly ia = inject(IaService);

  protected readonly meta = signal<MetaFiltres | null>(null);
  protected readonly recommandations = signal<Recommandation[]>([]);
  protected readonly chargement = signal(true);
  protected readonly erreur = signal('');

  protected readonly budgetMax = signal(35);
  protected readonly budgetParDefaut = signal(35);
  protected readonly cuisine = signal('');
  protected readonly ville = signal('');
  protected readonly options = signal<string[]>([]);
  protected readonly limite = signal(5);

  protected readonly reanalyse = signal(false);
  protected readonly messageReanalyse = signal('');
  protected readonly erreurReanalyse = signal('');

  constructor() {
    this.service
      .filtres()
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: (meta) => this.meta.set(meta),
        error: () => this.meta.set(null)
      });

    this.userService
      .getPreferences()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (preferences) => {
          const budget = BUDGETS[preferences.budgetLevel ?? ''] ?? 35;
          this.budgetParDefaut.set(budget);
          this.budgetMax.set(budget);
          this.charger();
        },
        error: () => this.charger()
      });
  }

  protected lancerRecherche(event: Event): void {
    event.preventDefault();
    this.charger();
  }

  protected changerBudget(valeur: string): void {
    this.budgetMax.set(Number(valeur));
  }

  protected changerLimite(valeur: string): void {
    this.limite.set(Number(valeur));
  }

  protected basculerOption(option: string): void {
    this.options.update((actuelles) =>
      actuelles.includes(option)
        ? actuelles.filter((valeur) => valeur !== option)
        : [...actuelles, option]
    );
  }

  protected reinitialiser(): void {
    this.budgetMax.set(this.budgetParDefaut());
    this.cuisine.set('');
    this.ville.set('');
    this.options.set([]);
    this.limite.set(5);
    this.charger();
  }

  protected reanalyser(): void {
    this.reanalyse.set(true);
    this.messageReanalyse.set('');
    this.erreurReanalyse.set('');

    this.ia
      .reanalyser()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (resultat) => {
          this.reanalyse.set(false);
          this.messageReanalyse.set(resultat.message);
          this.ia.chargerStatut();
          this.charger();
        },
        error: (erreur: HttpErrorResponse) => {
          this.reanalyse.set(false);
          this.erreurReanalyse.set(getErrorMessage(erreur));
        }
      });
  }

  private charger(): void {
    this.chargement.set(true);
    this.erreur.set('');

    this.service
      .recommandations({
        budgetMax: this.budgetMax(),
        cuisine: this.cuisine() || undefined,
        ville: this.ville() || undefined,
        options: this.options(),
        limite: this.limite()
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (recommandations) => {
          this.recommandations.set(recommandations);
          this.chargement.set(false);
        },
        error: (erreur: HttpErrorResponse) => {
          this.recommandations.set([]);
          this.erreur.set(getErrorMessage(erreur));
          this.chargement.set(false);
        }
      });
  }
}
