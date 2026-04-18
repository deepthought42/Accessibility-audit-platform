import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterModule } from '@angular/router';
import { Observable } from 'rxjs';
import { Breadcrumb, BreadcrumbService } from '../../../services/breadcrumb/breadcrumb.service';

/**
 * Breadcrumb bar. Subscribes to BreadcrumbService; renders nothing when empty.
 *
 * See docs/design/04-navigation-ia.md §3.
 */
@Component({
  standalone: true,
  imports: [CommonModule, RouterModule],
  selector: 'app-looksee-breadcrumb-bar',
  templateUrl: './breadcrumb-bar.component.html',
  styleUrls: ['./breadcrumb-bar.component.scss'],
})
export class LookseeBreadcrumbBarComponent {
  private readonly service = inject(BreadcrumbService);
  readonly crumbs$: Observable<Breadcrumb[]> = this.service.crumbs$;
}
