import { Component, Input, OnChanges } from '@angular/core';

@Component({
  selector: 'app-score-gauge',
  templateUrl: './score-gauge.component.html',
})
export class ScoreGaugeComponent implements OnChanges {
  @Input() score: number = 0;
  @Input() size: number = 120;
  @Input() strokeWidth: number = 8;
  @Input() label: string = '';

  radius: number = 0;
  circumference: number = 0;
  dashoffset: number = 0;
  strokeColor: string = '#ef4444';

  ngOnChanges(): void {
    this.recalculate();
  }

  private recalculate(): void {
    this.radius = (this.size - this.strokeWidth) / 2;
    this.circumference = 2 * Math.PI * this.radius;
    this.dashoffset = this.circumference * (1 - this.score / 100);

    if (this.score >= 80) {
      this.strokeColor = '#10b981';
    } else if (this.score >= 60) {
      this.strokeColor = '#f59e0b';
    } else {
      this.strokeColor = '#ef4444';
    }
  }
}
