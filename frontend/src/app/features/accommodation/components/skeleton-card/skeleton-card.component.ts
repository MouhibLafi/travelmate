import { Component } from '@angular/core';

@Component({
  selector: 'tm-skeleton-card',
  standalone: true,
  template: `
    <div class="tm-card skeleton">
      <div class="tm-skeleton image"></div>
      <div class="body">
        <div class="tm-skeleton line short"></div>
        <div class="tm-skeleton line"></div>
        <div class="tm-skeleton line medium"></div>
        <div class="tm-skeleton line price"></div>
      </div>
    </div>
  `,
  styles: [`
    .skeleton { overflow: hidden; }
    .image { height: 190px; border-radius: 0; }
    .body { padding: 16px; display: flex; flex-direction: column; gap: 10px; }
    .line { height: 14px; width: 100%; }
    .short { width: 35%; }
    .medium { width: 60%; }
    .price { width: 30%; height: 20px; align-self: flex-end; }
  `],
})
export class SkeletonCardComponent {}
