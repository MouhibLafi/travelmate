import { Component, DestroyRef, ElementRef, computed, effect, inject, input, signal, untracked, viewChild } from '@angular/core';
import { CurrencyPipe, DatePipe, DecimalPipe, PercentPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subscription, interval } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatTooltipModule } from '@angular/material/tooltip';

import { AccommodationService } from '../../services/accommodation.service';
import { errorMessage } from '../../services/notification.service';
import { PriceAnalysis, PriceVerdict } from '../../models/accommodation.models';
import { TYPE_META } from '../../utils/amenity.utils';
import { StarRatingComponent } from '../star-rating/star-rating.component';
import { ImageFallbackDirective } from '../image-fallback.directive';

const GAUGE_RANGE = 40; // the gauge spans -40% .. +40%

@Component({
  selector: 'tm-ai-price-panel',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, DecimalPipe, PercentPipe, RouterLink, MatButtonModule, MatIconModule, MatChipsModule,
    MatExpansionModule, MatTooltipModule, StarRatingComponent, ImageFallbackDirective],
  templateUrl: './ai-price-panel.component.html',
  styleUrl: './ai-price-panel.component.scss',
})
export class AiPricePanelComponent {
  private readonly api = inject(AccommodationService);
  private readonly destroyRef = inject(DestroyRef);

  accommodationId = input.required<number>();
  city = input<string>('');
  checkIn = input<string | null>(null);
  checkOut = input<string | null>(null);
  guests = input<number | null>(null);

  readonly status = signal<'idle' | 'loading' | 'done' | 'error'>('idle');
  readonly analysis = signal<PriceAnalysis | null>(null);
  readonly error = signal<string | null>(null);
  readonly loadingStep = signal(0);
  readonly typeMeta = TYPE_META;

  private readonly carousel = viewChild<ElementRef<HTMLElement>>('carousel');
  private request?: Subscription;
  private ticker?: Subscription;

  readonly loadingMessages = computed(() => [
    'AI is analyzing comparable listings…',
    `Finding similar stays${this.city() ? ' in ' + this.city() : ''}…`,
    'Computing the market baseline…',
    'Asking llama3 for a fair price…',
    'Validating the AI answer…',
  ]);

  readonly ai = computed(() => this.analysis()?.aiResult ?? null);

  /** Marker position (0-100%) on the verdict gauge. */
  readonly markerPosition = computed(() => {
    const diff = this.ai()?.differencePercent ?? 0;
    const clamped = Math.max(-GAUGE_RANGE, Math.min(GAUGE_RANGE, diff));
    return ((clamped + GAUGE_RANGE) / (2 * GAUGE_RANGE)) * 100;
  });

  readonly verdictLabel = computed(() => verdictText(this.ai()?.priceVerdict));

  constructor() {
    // A new accommodation (e.g. after clicking an alternative) resets the panel.
    effect(() => {
      this.accommodationId();
      untracked(() => this.reset());
    });
    this.destroyRef.onDestroy(() => { this.request?.unsubscribe(); this.ticker?.unsubscribe(); });
  }

  analyze(): void {
    this.request?.unsubscribe();
    this.status.set('loading');
    this.error.set(null);
    this.startTicker();
    this.request = this.api.priceAnalysis(this.accommodationId(), {
      checkIn: this.checkIn(), checkOut: this.checkOut(), guests: this.guests(),
    }).subscribe({
      next: result => {
        this.analysis.set(result);
        this.status.set('done');
        this.ticker?.unsubscribe();
      },
      error: err => {
        this.error.set(errorMessage(err, 'The price analysis failed'));
        this.status.set('error');
        this.ticker?.unsubscribe();
      },
    });
  }

  scrollCarousel(direction: 1 | -1): void {
    this.carousel()?.nativeElement.scrollBy({ left: direction * 280, behavior: 'smooth' });
  }

  private reset(): void {
    this.request?.unsubscribe();
    this.ticker?.unsubscribe();
    this.status.set('idle');
    this.analysis.set(null);
    this.error.set(null);
  }

  private startTicker(): void {
    this.ticker?.unsubscribe();
    this.loadingStep.set(0);
    this.ticker = interval(2200).subscribe(i =>
      this.loadingStep.set(Math.min(i + 1, this.loadingMessages().length - 1)));
  }
}

function verdictText(verdict: PriceVerdict | undefined): string {
  switch (verdict) {
    case 'UNDERPRICED': return 'Great deal';
    case 'OVERPRICED': return 'Overpriced';
    case 'FAIR': return 'Fair price';
    default: return '';
  }
}
