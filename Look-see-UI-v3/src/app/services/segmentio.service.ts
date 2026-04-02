import { Injectable, inject } from '@angular/core';
import { AuthService } from '@auth0/auth0-angular';
import { environment } from '../../environments/environment';
import { Domain } from '../models/domain/domain';
import { TestUser } from '../models/testUser/testUser';

@Injectable({
  providedIn: 'root'
})
export class SegmentIOService {
  private auth = inject(AuthService);
  private segment_key = environment.segment_key;

  sendTestUserCreatedEvent(domain: Domain, test_user: TestUser) {
    window.analytics.track('Added Domain', {
      domain_id: domain.id,
      test_user_id: test_user.id
    });
  }

  sendDomainCreatedEvent(domain: Domain) {
    window.analytics.track('Added Domain', {
      domain_id: domain.id,
      domain_url: domain.url
    });
  }
  
  identify() {
    window.analytics.identify()
  }

  sendRecommendationDeletedMessage(audit_key: string, recommendation: string){
    window.analytics.track('Deleted Recommendation', {
      audit_key: audit_key,
      recommendation: recommendation
    });
  }

  sendRequestAccessMessage(feature_name: string, user_id: string){
    window.analytics.track('Request access to '+feature_name, {
      feature: feature_name,
      user_id: user_id
    });
  }

  sendUxAuditStartedMessage(page_url: string){
    window.analytics.track('Click start single page UX audit', {
      url: page_url
    });
  }

  //TRACKING METHODS
  sendDomainUxAuditStartedMessage(domain_id: number){
    window.analytics.track('Click start Full site UX audit', {
      domain_id: domain_id
    });
  }

  trackReportRequested(audit_record_id: number, email: string) {
    window.analytics.track('Request Report ', {
      audit_record_id: audit_record_id,
      email: email
    });
  }

  sendRecommendationAddedMessage( issue_key: string, recommendation: string){
    window.analytics.track('Added recommendation ', {
      issue_key: issue_key,
      recommendation: recommendation
    });
  }

  sendObservationAddedMessage(issue_type: string, description: string, recommendation: string){
    window.analytics.track('Added observation', {
      issue_type: issue_type,
      description: description,
      recommendation: recommendation
    });
  }

  trackExportReportAuthenticatedClick(page_url: string, page_key: string){
    window.analytics.track('Clicked Export Page Report button', {
      url: page_url,
      page_key: page_key,
      is_logged_in: true
    });
  }

  trackExportReportNonAuthenticatedClick(page_url: string, page_key: string){
    window.analytics.track('Clicked Export Page Report button', {
      url: page_url,
      page_key: page_key,
      is_logged_in: false
    });
  }
  
  trackExportDomainReportClick(domain_id: number){
    window.analytics.track('Clicked Export Domain Report button', {
      domain_id: domain_id
    });
  }

  trackLoginClick(): void {
    if (typeof window !== 'undefined' && window.analytics) {
      window.analytics.track('Clicked login button');
    }
  }

  trackLogoutClick(){
    window.analytics.track('Clicked logout button');
  }

  trackCompetitiveAnalysisClick(){
    window.analytics.track('Clicked competitive analysis');
  }


  trackCompetitiveAnalysisAnalyzeClick(){
    window.analytics.track('Clicked analyze button in competitive analysis');
  }
}
