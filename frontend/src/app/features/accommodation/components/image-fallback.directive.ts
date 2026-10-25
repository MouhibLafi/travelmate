import { Directive, HostBinding, HostListener, Input } from '@angular/core';
import { IMAGE_PLACEHOLDER } from '../utils/amenity.utils';

/** <img [tmImg]="url"> — shows a branded placeholder when the url is empty or broken. */
@Directive({ selector: 'img[tmImg]', standalone: true })
export class ImageFallbackDirective {
  @HostBinding('src') src = IMAGE_PLACEHOLDER;
  @HostBinding('attr.loading') loading = 'lazy';

  @Input() set tmImg(url: string | null | undefined) {
    this.src = url || IMAGE_PLACEHOLDER;
  }

  @HostListener('error')
  onError(): void {
    if (this.src !== IMAGE_PLACEHOLDER) this.src = IMAGE_PLACEHOLDER;
  }
}
