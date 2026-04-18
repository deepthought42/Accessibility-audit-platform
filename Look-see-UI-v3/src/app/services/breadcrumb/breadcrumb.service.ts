import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

/**
 * A single breadcrumb entry. `href` absent = current page (rendered plain).
 * See docs/design/04-navigation-ia.md §3.
 */
export interface Breadcrumb {
  label: string;
  href?: string;
}

/**
 * App-wide breadcrumb state. Route components set breadcrumbs in ngOnInit;
 * `<app-breadcrumb-bar>` reads `crumbs$` and renders.
 *
 * For async data loads, components should push twice: once with placeholder
 * labels ("Loading…"), then again when resolved.
 */
@Injectable({ providedIn: 'root' })
export class BreadcrumbService {
  private readonly _crumbs$ = new BehaviorSubject<Breadcrumb[]>([]);
  readonly crumbs$: Observable<Breadcrumb[]> = this._crumbs$.asObservable();

  set(crumbs: Breadcrumb[]): void {
    this._crumbs$.next(crumbs);
  }

  clear(): void {
    this._crumbs$.next([]);
  }

  snapshot(): Breadcrumb[] {
    return this._crumbs$.value;
  }
}
