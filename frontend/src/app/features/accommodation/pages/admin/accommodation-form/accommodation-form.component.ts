import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { AccommodationService } from '../../../services/accommodation.service';
import { NotificationService } from '../../../services/notification.service';
import { ACCOMMODATION_TYPES, AMENITIES, AccommodationRequest, AccommodationType, Amenity, ApiError } from '../../../models/accommodation.models';
import { AMENITY_META, TYPE_META } from '../../../utils/amenity.utils';
import { ImageFallbackDirective } from '../../../components/image-fallback.directive';

const URL_PATTERN = /^https?:\/\/\S+$/i;

@Component({
  selector: 'tm-accommodation-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, MatFormFieldModule, MatInputModule, MatSelectModule, MatChipsModule,
    MatButtonModule, MatIconModule, MatProgressBarModule, ImageFallbackDirective],
  templateUrl: './accommodation-form.component.html',
  styleUrl: './accommodation-form.component.scss',
})
export class AccommodationFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(AccommodationService);
  private readonly notify = inject(NotificationService);
  private readonly router = inject(Router);

  /** Route param, present in edit mode. */
  id = input<string | undefined>(undefined);

  readonly types = ACCOMMODATION_TYPES;
  readonly typeMeta = TYPE_META;
  readonly amenities = AMENITIES;
  readonly amenityMeta = AMENITY_META;
  readonly loading = signal(false);
  readonly saving = signal(false);

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    type: this.fb.nonNullable.control<AccommodationType>('HOTEL', Validators.required),
    description: ['', Validators.maxLength(4000)],
    city: ['', [Validators.required, Validators.maxLength(100)]],
    country: ['', [Validators.required, Validators.maxLength(100)]],
    address: ['', Validators.maxLength(255)],
    stars: [3, [Validators.required, Validators.min(1), Validators.max(5)]],
    rating: [4, [Validators.required, Validators.min(0), Validators.max(5)]],
    pricePerNight: this.fb.nonNullable.control<number | null>(null, [Validators.required, Validators.min(1), Validators.max(100000)]),
    maxGuests: [2, [Validators.required, Validators.min(1), Validators.max(30)]],
    destinationId: this.fb.control<number | null>(null),
    amenities: this.fb.nonNullable.control<Amenity[]>([]),
    imageUrls: this.fb.array<string>([]),
  });

  get imageUrls(): FormArray {
    return this.form.controls.imageUrls;
  }

  get isEdit(): boolean {
    return !!this.id();
  }

  ngOnInit(): void {
    const id = this.id();
    if (!id) {
      this.addImage();
      return;
    }
    this.loading.set(true);
    this.api.getById(+id).subscribe({
      next: a => {
        this.form.patchValue({
          name: a.name, type: a.type, description: a.description ?? '', city: a.city, country: a.country,
          address: a.address ?? '', stars: a.stars, rating: a.rating, pricePerNight: a.pricePerNight,
          maxGuests: a.maxGuests, destinationId: a.destinationId, amenities: a.amenities,
        });
        a.imageUrls.forEach(url => this.addImage(url));
        if (!a.imageUrls.length) this.addImage();
        this.loading.set(false);
      },
      error: err => {
        this.notify.error(err, 'Accommodation not found');
        this.router.navigate(['/accommodations/admin']);
      },
    });
  }

  addImage(url = ''): void {
    if (this.imageUrls.length >= 10) return;
    this.imageUrls.push(this.fb.nonNullable.control(url, [Validators.pattern(URL_PATTERN), Validators.maxLength(500)]));
  }

  removeImage(index: number): void {
    this.imageUrls.removeAt(index);
  }

  error(control: string): string | null {
    const c = this.form.get(control);
    if (!c || !c.errors || !(c.touched || c.dirty)) return null;
    if (c.errors['required']) return 'This field is required';
    if (c.errors['maxlength']) return `Max ${c.errors['maxlength'].requiredLength} characters`;
    if (c.errors['min']) return `Must be at least ${c.errors['min'].min}`;
    if (c.errors['max']) return `Must be at most ${c.errors['max'].max}`;
    if (c.errors['server']) return c.errors['server'];
    return 'Invalid value';
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.notify.error(null, 'Please fix the highlighted fields');
      return;
    }
    const v = this.form.getRawValue();
    const request: AccommodationRequest = {
      ...v,
      pricePerNight: Number(v.pricePerNight),
      description: v.description.trim() || null,
      address: v.address.trim() || null,
      imageUrls: v.imageUrls.map(u => (u ?? '').trim()).filter(Boolean),
    };
    this.saving.set(true);
    const call = this.isEdit ? this.api.update(+this.id()!, request) : this.api.create(request);
    call.subscribe({
      next: saved => {
        this.notify.success(this.isEdit ? 'Accommodation updated' : `"${saved.name}" created`);
        this.router.navigate(['/accommodations/admin']);
      },
      error: (err: HttpErrorResponse) => {
        this.saving.set(false);
        this.applyServerErrors(err);
        this.notify.error(err, 'Save failed');
      },
    });
  }

  /** Shows backend Bean Validation messages under the matching fields. */
  private applyServerErrors(err: HttpErrorResponse): void {
    const fieldErrors = (err.error as ApiError | null)?.fieldErrors ?? {};
    for (const [field, message] of Object.entries(fieldErrors)) {
      const control = this.form.get(field);
      if (control) {
        control.setErrors({ server: message });
        control.markAsTouched();
      }
    }
  }
}
