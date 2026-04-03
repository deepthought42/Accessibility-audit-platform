import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from '@auth0/auth0-angular';
import { AuditDashboardComponent } from './components/audit-dashboard/audit-dashboard.component';
import { AuditListComponent } from './components/audit-list/audit-list.component';
import { DomainsComponent } from './components/domains/domains.component';
import { HowItWorksComponent } from './components/how-it-works/how-it-works.component';
import { LandingComponent } from './components/landing/landing.component';
import { PageAuditReviewComponent } from './components/page-audit-review/page-audit-review.component';

import { IntegrationsPanelComponent } from './integrations-panel/integrations-panel.component';
import { UserProfileComponent } from './user-profile/user-profile.component';

const routes: Routes = [
  { path: '', component: LandingComponent },
  { path: 'dashboard', component: AuditDashboardComponent },
  { path: 'account', component: UserProfileComponent, canActivate: [AuthGuard]},

  { path: 'domains', component: DomainsComponent, canActivate: [AuthGuard]},
  { path: 'audit', component: AuditListComponent, canActivate: [AuthGuard]},
  { path: 'audit/:id/page', component: PageAuditReviewComponent, canActivate: [AuthGuard]},
  { path: 'audit/:id/review', component: PageAuditReviewComponent },
  { path: 'audit/:id/domain', component: AuditDashboardComponent, canActivate: [AuthGuard]},
  { path: 'how-it-works', component: HowItWorksComponent},
  { path: 'integration', component: IntegrationsPanelComponent}
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
