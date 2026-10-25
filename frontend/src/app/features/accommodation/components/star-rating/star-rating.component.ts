import { Component, computed, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

/** Hotel classification (1-5 stars). */
@Component({
  selector: 'tm-star-rating',
  standalone: true,
  imports: [MatIconModule],
  template: `
    <span class="stars" [attr.aria-label]="stars() + ' stars'">
      @for (filled of slots(); track $index) {
        <mat-icon [class.empty]="!filled">star</mat-icon>
      }
    </span>
  `,
  styles: [`
    .stars { display: inline-flex; align-items: center; vertical-align: middle; }
    mat-icon { font-size: var(--tm-star-size, 16px); width: var(--tm-star-size, 16px); height: var(--tm-star-size, 16px); color: #F5B400; }
    mat-icon.empty { color: #D9DEE5; }
  `],
})
export class StarRatingComponent {
  stars = input.required<number>();
  slots = computed(() => Array.from({ length: 5 }, (_, i) => i < this.stars()));
}
