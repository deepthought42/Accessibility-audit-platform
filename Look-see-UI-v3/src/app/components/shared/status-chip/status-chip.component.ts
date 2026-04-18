import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import {
  faCheck,
  faCircle,
  faSpinner,
  faTriangleExclamation,
  faXmark,
  IconDefinition,
} from '@fortawesome/free-solid-svg-icons';

export type StatusChipKind =
  // Audit status
  | 'queued'
  | 'running'
  | 'complete'
  | 'failed'
  | 'cancelled'
  // Issue severity
  | 'critical'
  | 'major'
  | 'minor'
  // Issue status
  | 'open'
  | 'in-progress'
  | 'done'
  | 'ignored'
  // Generic
  | 'beta'
  | 'new'
  | 'available'
  | 'waitlist';

export type StatusChipSize = 'sm' | 'md';

interface ChipStyle {
  label: string;
  icon?: IconDefinition;
  spin?: boolean;
}

const CHIP_STYLES: Record<StatusChipKind, ChipStyle> = {
  queued:        { label: 'Queued' },
  running:       { label: 'Auditing…', icon: faSpinner, spin: true },
  complete:      { label: 'Complete', icon: faCheck },
  failed:        { label: 'Failed', icon: faTriangleExclamation },
  cancelled:     { label: 'Cancelled', icon: faXmark },
  critical:      { label: 'Critical' },
  major:         { label: 'Major' },
  minor:         { label: 'Minor' },
  open:          { label: 'Open' },
  'in-progress': { label: 'In progress' },
  done:          { label: 'Done', icon: faCheck },
  ignored:       { label: 'Ignored' },
  beta:          { label: 'Beta' },
  new:           { label: 'New' },
  available:     { label: 'Available' },
  waitlist:      { label: 'Waitlist' },
};

/**
 * Status / severity / generic chip. Visuals driven by semantic tokens from
 * src/theme/tokens.scss so both light and dark modes work automatically.
 *
 * See docs/design/06-shared-components.md §2.
 */
@Component({
  standalone: true,
  imports: [CommonModule, FontAwesomeModule],
  selector: 'app-looksee-status-chip',
  templateUrl: './status-chip.component.html',
  styleUrls: ['./status-chip.component.scss'],
})
export class LookseeStatusChipComponent {
  @Input() kind!: StatusChipKind;
  @Input() size: StatusChipSize = 'md';
  @Input() label?: string;
  @Input() showDot = false;

  get style(): ChipStyle {
    return CHIP_STYLES[this.kind];
  }

  get displayLabel(): string {
    return this.label ?? this.style.label;
  }

  get a11yLabel(): string {
    return `Status: ${this.displayLabel}`;
  }

  get rootClasses(): string[] {
    return [
      'looksee-chip',
      `looksee-chip--${this.kind}`,
      `looksee-chip--${this.size}`,
    ];
  }

  readonly faCircle = faCircle;
}
