import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { IconComponent } from '../components/icon/icon.component';
import { getErrorMessage } from '../services/api';
import {
  CatalogItem, ITEM_TYPES, ItemRequest, ItemType, Itinerary, ItineraryDay, ItineraryItem, formatEuros,
} from './itinerary.model';
import { ItineraryService } from './itinerary.service';

// Les types qui ont un catalogue (modules 3, 4, 5)
const CATALOG_TYPES: ItemType[] = ['ACTIVITE', 'RESTAURANT', 'HEBERGEMENT'];

// Formulaire pour ajouter un element a une journee, ou modifier un element existant
@Component({
  selector: 'tm-item-form',
  imports: [FormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, IconComponent],
  templateUrl: './item-form.component.html',
  styleUrl: './item-form.component.css',
})
export class ItemFormComponent implements OnInit {
  @Input({ required: true }) itineraryId!: number;
  @Input({ required: true }) day!: ItineraryDay;
  @Input() item: ItineraryItem | null = null;   // null = ajout, sinon = modification

  @Output() saved = new EventEmitter<Itinerary>();
  @Output() cancelled = new EventEmitter<void>();

  types = ITEM_TYPES;
  catalog: CatalogItem[] = [];
  saving = false;
  errorMessage = '';
  formatEuros = formatEuros;

  // Champs du formulaire
  type: ItemType = 'ACTIVITE';
  refId: number | null = null;
  name = '';
  category: string | null = null;
  startTime = '';
  endTime = '';
  unitPrice: number | null = 0;
  quantity: number | null = 1;
  notes = '';

  constructor(private itineraryService: ItineraryService) {}

  ngOnInit(): void {
    if (this.item) {
      this.type = this.item.type;
      this.refId = this.item.refId;
      this.name = this.item.name;
      this.category = this.item.category;
      this.startTime = this.item.startTime?.slice(0, 5) ?? '';
      this.endTime = this.item.endTime?.slice(0, 5) ?? '';
      this.unitPrice = this.item.unitPrice;
      this.quantity = this.item.quantity;
      this.notes = this.item.notes ?? '';
    }
    this.loadCatalog();
  }

  hasCatalog(): boolean {
    return CATALOG_TYPES.includes(this.type);
  }

  onTypeChange(): void {
    this.refId = null;
    this.loadCatalog();
  }

  // Un element du catalogue choisi : on reprend son nom, sa categorie et son prix
  onCatalogChange(): void {
    const c = this.catalog.find((x) => x.id === Number(this.refId));
    if (c) {
      this.name = c.name;
      this.category = c.category;
      this.unitPrice = c.price;
    }
  }

  save(): void {
    const request: ItemRequest = {
      type: this.type,
      refId: this.refId ? Number(this.refId) : null,
      name: this.name,
      category: this.category,
      startTime: this.startTime || null,
      endTime: this.endTime || null,
      unitPrice: this.unitPrice,
      quantity: this.quantity,
      notes: this.notes || null,
    };
    const call = this.item
      ? this.itineraryService.updateItem(this.item.id, request)
      : this.itineraryService.addItem(this.itineraryId, this.day.id, request);

    this.saving = true;
    this.errorMessage = '';
    call.subscribe({
      next: (it) => this.saved.emit(it),
      error: (err) => {
        this.errorMessage = getErrorMessage(err);
        this.saving = false;
      },
    });
  }

  private loadCatalog(): void {
    this.catalog = [];
    if (!this.hasCatalog()) {
      return;
    }
    this.itineraryService.getCatalog(this.itineraryId, this.type).subscribe((list) => (this.catalog = list));
  }
}
