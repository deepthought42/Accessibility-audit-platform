import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { CommonModule } from '@angular/common';
import { HttpClientModule, provideHttpClient, withInterceptors } from '@angular/common/http';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuthService, authHttpInterceptorFn } from '@auth0/auth0-angular';
import { FaIconLibrary, FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faCheckSquare as farCheckSquare, faSquare as farSquare } from '@fortawesome/free-regular-svg-icons';
import {
  faAngleDown,
  faAngleUp,
  faBook,
  faCheckSquare,
  faChevronRight,
  faCircleCheck,
  faCircleNodes,
  faCircleRadiation,
  faCopyright,
  faDownload,
  faInfoCircle,
  faMagnifyingGlass,
  faSearch,
  faSpinner,
  faSquare,
  faTriangleExclamation,
  fas
} from '@fortawesome/free-solid-svg-icons';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { AuditDashboardComponent } from './components/audit-dashboard/audit-dashboard.component';
import { AuditFormComponent } from './components/audit-form/audit-form.component';
import { AuditListComponent } from './components/audit-list/audit-list.component';
import { AuditOnboardingComponent } from './components/audit-onboarding/audit-onboarding.component';
import { AuthButtonComponent } from './components/auth-button/auth-button.component';
import { DomainsComponent } from './components/domains/domains.component';
import { FooterComponent } from './components/footer/footer.component';
import { HowItWorksComponent } from './components/how-it-works/how-it-works.component';
import { LandingComponent } from './components/landing/landing.component';
import { LoadingComponent } from './components/loading/loading.component';
import { NavBarComponent } from './components/nav-bar/nav-bar.component';
import { PageAuditReviewComponent } from './components/page-audit-review/page-audit-review.component';

import { StartSinglePageAuditLoginRequired } from './components/start-audit-login-required-dialog/start-audit-login-required-dialog';

import { ScoreBadgeComponent } from './components/shared/score-badge/score-badge.component';
import { ScoreGaugeComponent } from './components/shared/score-gauge/score-gauge.component';
import { IntegrationsPanelComponent } from './integrations-panel/integrations-panel.component';
import { FilterByCategoryPipe } from './pipes/category_filter.pipe';
import { MatchesCategoryPipe } from './pipes/matches_category.pipe';
import { FilterSeverityPipe } from './pipes/severity_filter.pipe';
import { AccountService } from './services/account.service';
import { AuditorService } from './services/auditor.service';
import { MessageService } from './services/message.service';

// import { UpgradeSubscriptionComponent } from './upgrade-subscription/upgrade-subscription.component';
import { UserProfileComponent } from './user-profile/user-profile.component';

// Import the injector module and the HTTP client module from Angular

// Import the HTTP interceptor from the Auth0 Angular SDK

@NgModule({
  declarations: [
    AppComponent,
    NavBarComponent,
    AuthButtonComponent,
    UserProfileComponent,
    AuditListComponent,
    AuditDashboardComponent,
    IntegrationsPanelComponent,
    DomainsComponent,
    HowItWorksComponent,
    FooterComponent,
    MatchesCategoryPipe,

    LandingComponent,
    AuditOnboardingComponent,

    AuditFormComponent,
    PageAuditReviewComponent,
    LoadingComponent,
    StartSinglePageAuditLoginRequired,
    FilterByCategoryPipe,
    FilterSeverityPipe,
    ScoreBadgeComponent,
    ScoreGaugeComponent
  ],
  imports: [
    HttpClientModule,
    BrowserModule,
    AppRoutingModule,
    FontAwesomeModule,
    MatProgressSpinnerModule,
    MatProgressBarModule,
    MatDialogModule,
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatTooltipModule
  ],
  providers: [
    AccountService,

    MessageService,
    AuditorService,
    provideHttpClient(withInterceptors([authHttpInterceptorFn])),
    {
      provide: Window,
      useValue: window,
    },
    {
      provide: MatDialogRef,
      useValue: {}
    },
    {
      provide: MAT_DIALOG_DATA,
      useValue: {}
    },
  ],
  bootstrap: [AppComponent]
})

export class AppModule {
  constructor(library: FaIconLibrary, public auth_service: AuthService) {
    library.addIconPacks(fas);
    library.addIcons(
      faSquare,
      faCheckSquare,
      farSquare,
      farCheckSquare,
      faInfoCircle,
      faCircleCheck,
      faTriangleExclamation,
      faCircleRadiation,
      faChevronRight,
      faSpinner,
      faMagnifyingGlass,
      faCircleNodes,
      faBook,
      faDownload,
      faAngleDown,
      faAngleUp,
      faSearch,
      faCopyright
    );

    this.auth_service.error$.subscribe(() => {
      // Handle Error here
      this.auth_service.loginWithRedirect()
    });
  }
}
