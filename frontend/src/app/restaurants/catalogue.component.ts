import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { Subject, debounceTime, startWith } from 'rxjs';

import { getErrorMessage } from '../services/api';
import { MetaFiltres, Restaurant } from './restaurant.model';
import { RestaurantService } from './restaurant.service';
import { RestaurantCardComponent } from './restaurant-card.component';

type Tri = 'note' | 'prix-asc' | 'prix-desc' | 'nom';

@Component({
  selector: 'tm-catalogue',
  imports: [RestaurantCardComponent, RouterLink],
  templateUrl: './catalogue.component.html',
  styleUrls: ['./catalogue.component.css', './restaurants.css']
})
export class CatalogueComponent {
  private readonly service = inject(RestaurantService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly rafraichir$ = new Subject<void>();

  protected readonly restaurants = signal<Restaurant[]>([]);
  protected readonly meta = signal<MetaFiltres | null>(null);
  protected readonly chargement = signal(true);
  protected readonly erreur = signal('');

  protected readonly recherche = signal('');
  protected readonly cuisine = signal('');
  protected readonly ville = signal('');
  protected readonly budgetMax = signal(50);
  protected readonly noteMin = signal(0);
  protected readonly options = signal<string[]>([]);
  protected readonly tri = signal<Tri>('note');
  protected readonly filtresOuverts = signal(false);

  protected readonly resultats = computed(() => {
    const liste = [...this.restaurants()];
    switch (this.tri()) {
      case 'prix-asc':
        return liste.sort((a, b) => a.prixMoyen - b.prixMoyen);
      case 'prix-desc':
        return liste.sort((a, b) => b.prixMoyen - a.prixMoyen);
      case 'nom':
        return liste.sort((a, b) => a.nom.localeCompare(b.nom, 'fr'));
      default:
        return liste.sort((a, b) => b.noteMoyenne - a.noteMoyenne || b.nombreAvis - a.nombreAvis);
    }
  });

  protected readonly filtresActifs = computed(() => {
    let total = 0;
    if (this.recherche().trim()) {
      total++;
    }
    if (this.cuisine()) {
      total++;
    }
    if (this.ville()) {
      total++;
    }
    if (this.budgetMax() < 50) {
      total++;
    }
    if (this.noteMin() > 0) {
      total++;
    }
    total += this.options().length;
    return total;
  });

  constructor() {
    this.rafraichir$
      .pipe(startWith(undefined), debounceTime(250), takeUntilDestroyed())
      .subscribe(() => this.charger());

    this.service
      .filtres()
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: (meta) => this.meta.set(meta),
        error: () => this.meta.set(null)
      });
  }

  protected rechercher(valeur: string): void {
    this.recherche.set(valeur);
    this.rafraichir$.next();
  }

  protected changerCuisine(valeur: string): void {
    this.cuisine.set(valeur);
    this.rafraichir$.next();
  }

  protected changerVille(valeur: string): void {
    this.ville.set(valeur);
    this.rafraichir$.next();
  }

  protected changerBudget(valeur: string): void {
    this.budgetMax.set(Number(valeur));
    this.rafraichir$.next();
  }

  protected changerNote(valeur: string): void {
    this.noteMin.set(Number(valeur));
    this.rafraichir$.next();
  }

  protected basculerOption(option: string): void {
    this.options.update((actuelles) =>
      actuelles.includes(option)
        ? actuelles.filter((valeur) => valeur !== option)
        : [...actuelles, option]
    );
    this.rafraichir$.next();
  }

  protected reinitialiser(): void {
    this.recherche.set('');
    this.cuisine.set('');
    this.ville.set('');
    this.budgetMax.set(50);
    this.noteMin.set(0);
    this.options.set([]);
    this.rafraichir$.next();
  }

  protected charger(): void {
    this.chargement.set(true);
    this.erreur.set('');

    this.service
      .rechercher({
        q: this.recherche().trim() || undefined,
        cuisine: this.cuisine() || undefined,
        ville: this.ville() || undefined,
        prixMax: this.budgetMax() < 50 ? this.budgetMax() : undefined,
        noteMin: this.noteMin() || undefined,
        options: this.options()
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (restaurants) => {
          this.restaurants.set(restaurants);
          this.chargement.set(false);
        },
        error: (erreur: HttpErrorResponse) => {
          this.restaurants.set([]);
          this.erreur.set(getErrorMessage(erreur));
          this.chargement.set(false);
        }
      });
  }
}
