import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, of } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MessageService } from '../../services/message.service';
import { SegmentIOService } from '../../services/segmentio.service';
import { AuditStats } from '../audit_stats';
import { Competitor } from '../competitor/competitor';
import { DomainSettings } from '../domain_settings/domainSettings';
import { PageStatistic } from '../page_statistic';
import { CreateTestUser } from '../testUser/createTestUser';
import { TestUser } from '../testUser/testUser';
import { CreateDomain } from './create_domain';
import { Domain } from './domain';

@Injectable({
  providedIn: 'root'
})
export class DomainService {
  private http = inject(HttpClient);
  private messageService = inject(MessageService);
  private segmentio = inject(SegmentIOService);
  private apiUrl = environment.api_url;
  private env_ip = this.apiUrl+"/domains"

  getDomains(): Observable<Domain[]> {
    return this.http.get<Domain[]>(this.env_ip);
  }

  addDomain(domain: CreateDomain): Observable<Domain> {
    return this.http.post<Domain>(this.env_ip, domain);
  }

  deleteDomain(domain_id: number): Observable<Domain> {
    return this.http.delete<Domain>(this.env_ip+"/"+domain_id)
  }

  startAudit(domain: Domain): Observable<Domain> {
    const start_audit_endpoint = `${this.env_ip}/${domain.id}/start`;

    return this.http.post<Domain>(start_audit_endpoint, {})
  }

  addTestUser(domain: Domain, test_user: CreateTestUser) {
    const test_user_endpoint = `${this.env_ip}/${domain.id}/users`;

    return this.http.post<TestUser>(test_user_endpoint, test_user)
  }

  deleteTestUser(domain: Domain, user_id: number) {
    const test_user_endpoint = `${this.env_ip}/${domain.id}/users/${user_id}`;

    return this.http.delete<TestUser>(test_user_endpoint)
  }

  startAuditForDomainId(domain_id: number): Observable<Domain> {
    const start_audit_endpoint = `${this.env_ip}/${domain_id}/start`;
    
    return this.http.post<Domain>(start_audit_endpoint, {})
  }

  getDomainPages(domain_id: number): Observable<PageStatistic[]>  {
    const url = `${this.env_ip}/${domain_id}/pages`;

    return this.http.get<PageStatistic[]>(url)
  }

  getDomainExcelReport(domain_id: number): Observable<Blob> {
    this.segmentio.trackExportDomainReportClick( domain_id )
    return this.http.get(this.env_ip+`/${domain_id}/report/excel`, {responseType: 'blob'})
  }

  getDomainPdfReport(domain_id: number): Observable<Blob> {
    this.segmentio.trackExportDomainReportClick( domain_id )
    return this.http.get(this.env_ip+`/${domain_id}/report/pdf`, {responseType: 'blob'})
  }

  getStats(domain_id: number): Observable<AuditStats> {
    const url = `${this.env_ip}/${domain_id}/stats`;
    
    return this.http.get<AuditStats>(url)
  }

  /* SETTINGS ENDPOINTS */

  updateTargetUserExpertise(domain_id: number, design_system_id: number, wcag_level: string, expertise_level: string): Observable<DomainSettings> {
    const domain_settings = { designSystem: { 
                              id: design_system_id,
                              wcagComplianceLevel: wcag_level, 
                              audienceProficiency: expertise_level,
                              allowedImageCharacteristics: [],
                              colorPalette: [] 
                            },
                            testUsers: []
                          } as DomainSettings
    const url = `${this.env_ip}/${domain_id}/settings/expertise`;
    return this.http.post<DomainSettings>(url, domain_settings)
  }


  updateWcagLevel(domain_id: number, design_system_id: number, wcag_level: string, expertise_level: string): Observable<DomainSettings> {
    const domain_settings = { designSystem: {
                              id: design_system_id,
                              wcagComplianceLevel: wcag_level, 
                              audienceProficiency: expertise_level,
                              allowedImageCharacteristics: [],
                              colorPalette: []
                            },
                            testUsers: []
                          } as DomainSettings

    return this.http.post<DomainSettings>(`${this.env_ip}/${domain_id}/settings/wcag`, domain_settings)
  }

  updateImageCharacteristicPolicy(domain_id: number, characteristics: string[]) {
    return this.http.post<DomainSettings>(`${this.env_ip}/${domain_id}/policies`, characteristics)
  }

  /* COMPETITOR ENDPOINTS */
  getAllCompetitors(domain_id: number,): Observable<Competitor[]> {
    return this.http.get<Competitor[]>(`${this.env_ip}/${domain_id}/competitors`, {})
  }

  addCompetitor(domain_id: number, competitor: Competitor): Observable<Competitor> {
    return this.http.post<Competitor>(`${this.env_ip}/${domain_id}/competitors`, competitor)
  }

  deleteCompetitor(domain_id: number, competitor_id: number): Observable<Competitor> {
    return this.http.delete<Competitor>(`${this.env_ip}/${domain_id}/competitors/${competitor_id}`, {})
  }
  
  getCompetitorColorPalettes(domain_id: number): Observable<[]> {
    return this.http.get<[]>(`${this.env_ip}/${domain_id}/competitors/palettes`)
  }

  getSettings(domain_id: number) {
    return this.http.get<DomainSettings>(`${this.env_ip}/${domain_id}/settings`, {})
  }



  /** Log a DomainService message with the MessageService */
  private log(message: string){
    this.messageService.add(`DomainService: ${message}`);
  }

  /**
   * Handle Http operation that failed.
   * Let the app continue.
   * @param operation - name of the operation that failed
   * @param result - optional value to return as the observable result
   */
  private handleError<T>(operation = 'operation', result?: T) {
    return (error: unknown): Observable<T> => {

      // TODO: send the error to remote logging infrastructure
      console.error(error); // log to console instead

      // TODO: better job of transforming error for user consumption
      this.log(`${operation} failed: ${error instanceof Error ? error.message : String(error)}`);

      // Let the app keep running by returning an empty result.
      return of(result as T);
    };
  }
}
