import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faSpinner, IconDefinition } from '@fortawesome/free-solid-svg-icons';

export type LookseeButtonVariant =
  | 'primary'
  | 'accent'
  | 'secondary'
  | 'ghost'
  | 'danger'
  | 'link';

export type LookseeButtonSize = 'sm' | 'md' | 'lg';

export type LookseeButtonType = 'button' | 'submit' | 'reset';

/**
 * Look-see design-system button. One component, six variants, three sizes.
 *
 * Tokens: references CSS custom properties from src/theme/tokens.scss so light
 * and dark modes both work automatically.
 *
 * See docs/design/01-design-system.md §5 for the full spec.
 */
@Component({
  standalone: true,
  imports: [CommonModule, FontAwesomeModule],
  selector: 'looksee-button',
  templateUrl: './button.component.html',
  styleUrls: ['./button.component.scss'],
})
export class LookseeButtonComponent {
  @Input() variant: LookseeButtonVariant = 'primary';
  @Input() size: LookseeButtonSize = 'md';
  @Input() type: LookseeButtonType = 'button';
  @Input() disabled = false;
  @Input() loading = false;
  @Input() fullWidth = false;
  @Input() ariaLabel?: string;
  @Input() leadingIcon?: IconDefinition;
  @Input() trailingIcon?: IconDefinition;

  @Output() pressed = new EventEmitter<MouseEvent>();

  readonly faSpinner = faSpinner;

  get isInteractive(): boolean {
    return !this.disabled && !this.loading;
  }

  get rootClasses(): string[] {
    return [
      'looksee-btn',
      `looksee-btn--${this.variant}`,
      `looksee-btn--${this.size}`,
      this.fullWidth ? 'looksee-btn--full' : '',
      this.loading ? 'looksee-btn--loading' : '',
    ].filter(Boolean);
  }

  onClick(event: MouseEvent): void {
    if (!this.isInteractive) {
      event.preventDefault();
      event.stopPropagation();
      return;
    }
    this.pressed.emit(event);
  }
}
