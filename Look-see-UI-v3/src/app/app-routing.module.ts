import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from '@auth0/auth0-angular';
import { AuditDashboardComponent } from './components/audit-dashboard/audit-dashboard.component';
import { AuditListComponent } from './components/audit-list/audit-list.component';
import { DomainsComponent } from './components/domains/domains.component';
import { HowItWorksComponent } from './components/how-it-works/how-it-works.component';
import { LandingComponent } from './components/landing/landing.component';
import { NotFoundComponent } from './components/not-found/not-found.component';
import { PageAuditReviewComponent } from './components/page-audit-review/page-audit-review.component';

import { IntegrationsPanelComponent } from './integrations-panel/integrations-panel.component';
import { UserProfileComponent } from './user-profile/user-profile.component';

/**
 * Route table.
 *
 * Canonical routes retained. Redirects (marked below) accept the new spec-04
 * route names (`/home`, `/sites`, `/audits`, `/audits/:id`, `/settings`,
 * `/integrations`) and forward them to existing components without requiring
 * every internal call-site to migrate at once.
 *
 * See docs/design/04-navigation-ia.md.
 */
const routes: Routes = [
  // ----- Canonical public / authed routes -----
  { path: '', component: LandingComponent },
  { path: 'dashboard', redirectTo: 'audit', pathMatch: 'full' },
  { path: 'account', component: UserProfileComponent, canActivate: [AuthGuard] },

  { path: 'domains', component: DomainsComponent, canActivate: [AuthGuard] },
  { path: 'audit', component: AuditListComponent, canActivate: [AuthGuard] },
  { path: 'audit/:id/page', component: PageAuditReviewComponent, canActivate: [AuthGuard] },
  { path: 'audit/:id/review', component: PageAuditReviewComponent },
  { path: 'audit/:id/domain', component: AuditDashboardComponent, canActivate: [AuthGuard] },

  { path: 'how-it-works', component: HowItWorksComponent },
  { path: 'integration', component: IntegrationsPanelComponent },

  // ----- Spec-04 forward-compatible aliases -----
  // Once /home, /sites, /audits, /settings components exist, flip these to
  // their own routes and redirect the old paths instead.
  { path: 'home', redirectTo: 'audit', pathMatch: 'full' },
  { path: 'sites', redirectTo: 'domains', pathMatch: 'full' },
  { path: 'audits', redirectTo: 'audit', pathMatch: 'full' },
  { path: 'audits/:id', redirectTo: 'audit/:id/review', pathMatch: 'full' },
  { path: 'settings', redirectTo: 'account', pathMatch: 'full' },
  { path: 'integrations', redirectTo: 'integration', pathMatch: 'full' },
  { path: 'help', redirectTo: 'how-it-works', pathMatch: 'full' },

  // ----- 404 -----
  { path: '**', component: NotFoundComponent },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
