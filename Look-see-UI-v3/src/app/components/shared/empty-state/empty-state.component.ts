import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { LookseeButtonComponent } from '../button/button.component';

export type EmptyStateIllustration =
  | 'audits'
  | 'sites'
  | 'issues'
  | 'search'
  | 'generic';

export interface EmptyStateAction {
  label: string;
  href?: string;
  variant?: 'primary' | 'accent' | 'secondary' | 'ghost' | 'link';
}

/**
 * Empty state — illustration + headline + body + actions.
 *
 * See docs/design/06-shared-components.md §7.
 */
@Component({
  standalone: true,
  imports: [CommonModule, LookseeButtonComponent],
  selector: 'looksee-empty-state',
  templateUrl: './empty-state.component.html',
  styleUrls: ['./empty-state.component.scss'],
})
export class LookseeEmptyStateComponent {
  @Input() illustration: EmptyStateIllustration = 'generic';
  @Input() title = '';
  @Input() body?: string;
  @Input() primaryAction?: EmptyStateAction;
  @Input() secondaryAction?: EmptyStateAction;
  @Input() compact = false;

  @Output() primaryActivated = new EventEmitter<void>();
  @Output() secondaryActivated = new EventEmitter<void>();

  /** Illustration paths map to existing assets in src/assets/. */
  get illustrationSrc(): string {
    const base = 'assets/';
    switch (this.illustration) {
      case 'audits':  return `${base}audit_website.jpg`;
      case 'sites':   return `${base}look-see_target_check.png`;
      case 'issues':  return `${base}review_audit.png`;
      case 'search':  return `${base}look-see_target_check.png`;
      case 'generic': default:
        return `${base}look-see_target_check.png`;
    }
  }

  onPrimary(): void {
    this.primaryActivated.emit();
  }

  onSecondary(): void {
    this.secondaryActivated.emit();
  }
}
