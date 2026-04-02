import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from '@auth0/auth0-angular';
import { AuditDashboardComponent } from './components/audit-dashboard/audit-dashboard.component';
import { AuditListComponent } from './components/audit-list/audit-list.component';
import { DomainsComponent } from './components/domains/domains.component';
import { HowItWorksComponent } from './components/how-it-works/how-it-works.component';
import { PageAuditReviewComponent } from './components/page-audit-review/page-audit-review.component';

import { IntegrationsPanelComponent } from './integrations-panel/integrations-panel.component';
import { UserProfileComponent } from './user-profile/user-profile.component';

/*
const routes: Routes = [
  //{ path: '', component:QuickAuditComponent }, 
  { path: 'dashboard', component: AuditRecordsComponent },
  //{ path: 'how-it-works', component: HowItWorksComponent},
  //{ path: 'domains', component: DomainsComponent},
  //{ path: 'domains/:domain_id', component: DomainDetailsComponent, canActivate: [AuthGuard]},
  { path: 'account', component: UserProfileComponent, canActivate: [AuthGuard]},
  //{ path: 'settings', component: SettingsComponent, canActivate: [AuthGuard]},
  //{ path: 'domains/:domain_id/settings', component: DomainSettingsComponent, canActivate: [AuthGuard]},
  //{ path: 'domains/:domain_id/policies', component: DomainSettingsComponent, canActivate: [AuthGuard]},
  //{ path: 'plans/agency', component: SubscriptionAgencyComponent},
  //{ path: 'plans/saas', component: SubscriptionCompanyComponent},
  { path: 'plans', component: UpgradeSubscriptionComponent},
  //{ path: 'portal', component: FeaturePortalComponent}
];
*/

const routes: Routes = [
  { path: '', redirectTo:'audit', pathMatch: 'full' },
  { path: 'dashboard', component: AuditDashboardComponent },
  { path: 'account', component: UserProfileComponent, canActivate: [AuthGuard]},

  { path: 'domains', component: DomainsComponent, canActivate: [AuthGuard]},
  { path: 'audit', component: AuditListComponent, canActivate: [AuthGuard]},
  { path: 'audit/:id/page', component: PageAuditReviewComponent, canActivate: [AuthGuard]},
  { path: 'audit/:id/domain', component: AuditDashboardComponent, canActivate: [AuthGuard]},
  { path: 'how-it-works', component: HowItWorksComponent},
  { path: 'integration', component: IntegrationsPanelComponent}
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
