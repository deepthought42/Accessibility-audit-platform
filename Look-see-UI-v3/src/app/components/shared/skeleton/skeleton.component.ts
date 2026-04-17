import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

export type SkeletonVariant = 'text' | 'title' | 'circle' | 'rect' | 'row';

/**
 * Animated loading placeholder. Respects prefers-reduced-motion.
 *
 * See docs/design/06-shared-components.md §8.
 */
@Component({
  standalone: true,
  imports: [CommonModule],
  selector: 'looksee-skeleton',
  templateUrl: './skeleton.component.html',
  styleUrls: ['./skeleton.component.scss'],
})
export class LookseeSkeletonComponent {
  @Input() variant: SkeletonVariant = 'text';
  @Input() width?: string;
  @Input() height?: string;
  @Input() rows = 1;

  get rowArray(): number[] {
    return Array.from({ length: this.rows }, (_, i) => i);
  }

  get customStyle(): Record<string, string> {
    const style: Record<string, string> = {};
    if (this.width) style['width'] = this.width;
    if (this.height) style['height'] = this.height;
    return style;
  }
}
