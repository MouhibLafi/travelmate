import { Component, OnInit, ViewChild, inject } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { IconComponent } from '../components/icon/icon.component';
import { getErrorMessage } from '../services/api';
import { AdminRestaurantService, RestaurantRequest } from './admin-restaurant.service';
import { Restaurant } from './restaurant.model';
import { RestaurantService } from './restaurant.service';

// Page ADMIN du module 4 : ajouter, modifier et supprimer les restaurants du catalogue.
// Les voyageurs ont leurs propres pages (catalogue, fiche, recommandations, favoris).
@Component({
  selector: 'tm-admin-restaurants',
  imports: [FormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, IconComponent, RouterLink],
  templateUrl: './admin-restaurants.component.html',
  styleUrl: './admin-restaurants.component.css',
})
export class AdminRestaurantsComponent implements OnInit {
  private readonly adminService = inject(AdminRestaurantService);
  private readonly restaurantService = inject(RestaurantService);

  restaurants: Restaurant[] = [];
  recherche = '';

  // Valeurs proposees dans le formulaire (celles deja utilisees dans le catalogue)
  cuisines: string[] = [];
  villes: string[] = [];
  options: string[] = ['Halal', 'Sans gluten', 'Vegan', 'Végétarien'];

  // Formulaire : editingId = null pour un ajout, sinon l'id du restaurant modifie
  @ViewChild('restaurantForm') restaurantForm?: NgForm;
  editingId: number | null = null;
  form: RestaurantRequest = this.formulaireVide();

  successMessage = '';
  errorMessage = '';

  ngOnInit(): void {
    this.charger();
    this.restaurantService.filtres().subscribe((meta) => {
      this.cuisines = meta.cuisines;
      this.villes = meta.villes;
      this.options = [...new Set([...this.options, ...meta.optionsAlimentaires])].sort();
    });
  }

  charger(): void {
    this.adminService.lister().subscribe({
      next: (liste) => (this.restaurants = liste),
      error: (error) => (this.errorMessage = getErrorMessage(error)),
    });
  }

  // Restaurants affiches dans le tableau (recherche sur le nom, la cuisine ou la ville)
  get restaurantsFiltres(): Restaurant[] {
    const texte = this.recherche.trim().toLowerCase();
    if (!texte) return this.restaurants;
    return this.restaurants.filter((r) =>
      [r.nom, r.cuisine, r.ville].some((valeur) => valeur.toLowerCase().includes(texte)));
  }

  basculerOption(option: string): void {
    const choisies = this.form.optionsAlimentaires;
    this.form.optionsAlimentaires = choisies.includes(option)
      ? choisies.filter((o) => o !== option)
      : [...choisies, option];
  }

  // Charge un restaurant dans le formulaire
  modifier(restaurant: Restaurant): void {
    this.editingId = restaurant.id;
    this.form = {
      nom: restaurant.nom,
      cuisine: restaurant.cuisine,
      description: restaurant.description,
      adresse: restaurant.adresse,
      ville: restaurant.ville,
      prixMoyen: restaurant.prixMoyen,
      imageUrl: restaurant.imageUrl ?? '',
      optionsAlimentaires: [...restaurant.optionsAlimentaires],
    };
    this.successMessage = '';
    this.errorMessage = '';
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  annuler(): void {
    this.editingId = null;
    this.form = this.formulaireVide();
    // Remet aussi les champs a l'etat "non touche" (sinon ils restent encadres en rouge)
    this.restaurantForm?.resetForm(this.form);
  }

  enregistrer(): void {
    this.successMessage = '';
    this.errorMessage = '';
    const requete = this.editingId === null
      ? this.adminService.creer(this.form)
      : this.adminService.modifier(this.editingId, this.form);

    requete.subscribe({
      next: (restaurant) => {
        this.successMessage = this.editingId === null
          ? `« ${restaurant.nom} » a été ajouté au catalogue.`
          : `« ${restaurant.nom} » a été modifié.`;
        this.annuler();
        this.charger();
      },
      error: (error) => (this.errorMessage = getErrorMessage(error)),
    });
  }

  supprimer(restaurant: Restaurant): void {
    if (!confirm(`Supprimer « ${restaurant.nom} » ? Ses avis et ses favoris seront supprimés aussi.`)) {
      return;
    }
    this.adminService.supprimer(restaurant.id).subscribe({
      next: () => {
        this.successMessage = `« ${restaurant.nom} » a été supprimé.`;
        if (this.editingId === restaurant.id) this.annuler();
        this.charger();
      },
      error: (error) => (this.errorMessage = getErrorMessage(error)),
    });
  }

  private formulaireVide(): RestaurantRequest {
    return { nom: '', cuisine: '', description: '', adresse: '', ville: '', prixMoyen: null, imageUrl: '', optionsAlimentaires: [] };
  }
}
