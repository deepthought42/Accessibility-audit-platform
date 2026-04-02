import { Component, Input } from '@angular/core';
import {
  faCircleCheck,
  faCircleRadiation,
  faSpinner,
  faTriangleExclamation,
  IconDefinition
} from '@fortawesome/free-solid-svg-icons';

export type ScoreBadgeSize = 'sm' | 'md' | 'lg';

@Component({
  selector: 'app-score-badge',
  templateUrl: './score-badge.component.html',
})
export class ScoreBadgeComponent {
  @Input() score: number = 0;
  @Input() size: ScoreBadgeSize = 'md';
  @Input() showValue: boolean = true;
  @Input() showIcon: boolean = true;
  @Input() loading: boolean = false;

  readonly faSpinner = faSpinner;

  get icon(): IconDefinition {
    if (this.score >= 80) return faCircleCheck;
    if (this.score >= 60) return faTriangleExclamation;
    return faCircleRadiation;
  }

  get color(): string {
    if (this.score >= 80) return '#10b981';
    if (this.score >= 60) return '#f59e0b';
    return '#ef4444';
  }

  get iconSizeClass(): string {
    switch (this.size) {
      case 'sm': return 'text-sm';
      case 'lg': return 'text-2xl';
      default:   return 'text-lg';
    }
  }

  get textSizeClass(): string {
    switch (this.size) {
      case 'sm': return 'text-xs';
      case 'lg': return 'text-xl';
      default:   return 'text-sm';
    }
  }
}
