import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { ObservationElementMapTwoWayBinding } from '../models/ObservationElementMapTwoWayBinding';
import { Audit } from '../models/audit';
import { AuditRecord } from '../models/auditRecord';
import { AuditStats } from '../models/audit_stats';
import { PageAudits } from '../models/page_audits';
import { SimplePage } from '../models/simple_page';
import { MessageService } from './message.service';
import { SegmentIOService } from './segmentio.service';

@Injectable({
  providedIn: 'root'
})
export class AuditService {
  private http = inject(HttpClient);
  private messageService = inject(MessageService);
  private segmentio = inject(SegmentIOService);
  private api_ip = environment.api_url

  private auditsUrl = this.api_ip+'/audits'  // URL to web api
  private auditRecordsUrl = this.api_ip+'/auditrecords'  // URL to web api

  private observationUrl = this.api_ip+'/observations'  // URL to web api
  private domainUrl = this.api_ip+'/domains'  // URL to web api
   
      
  getAuditReport(audit_record_id: number):  Observable<Blob> {
    const url = `${this.auditsUrl}/${audit_record_id}/excel`;
    console.log("audit record id for report :: "+audit_record_id)
    return this.http.get(url, {responseType: 'blob'})
        .pipe(
          tap(() => this.log('fetched report')),
          catchError(this.handleError<Blob>('getReport', {} as Blob))
        );
  } 
  
  startIndividualAudit(audit_url: string): Observable<AuditRecord> {
    const url = `${this.auditsUrl}/start-individual`;
    console.log("audit url to run :: "+audit_url)
    return this.http.post<AuditRecord>(url, {'url': audit_url})
  }

  getMostRecentAuditRecord(audit_url: string): Observable<PageAudits> {
    const url = `${this.auditsUrl}/pages`;
    console.log("audit url to run :: "+audit_url)
    return this.http.get<PageAudits>(url, {params: {'url': audit_url}} ).pipe(
      tap(() => this.log(`fetched page audits for domain = ${audit_url}`)),
      catchError(this.handleError<PageAudits>(`getAudits id = ${audit_url}`))
    )
  }

  /**
   * Retrieves list of audits for user
   * @returns 
   */
  getAudits(): Observable<AuditRecord[]> {
    const url = `${this.auditsUrl}`;

    return this.http.get<AuditRecord[]>(url).pipe(
      tap(() => this.log(`fetched audits for user`)),
      catchError(this.handleError<AuditRecord[]>(`getAudits id `))
    )
  }

  getAuditElements(audit_record_id: number): Observable<ObservationElementMapTwoWayBinding> {
    const url = `${this.auditRecordsUrl}/${audit_record_id}/elements`;

    return this.http.get<ObservationElementMapTwoWayBinding>(url).pipe(
      tap(() => this.log(`fetched  element map for page = ${audit_record_id}`)),
      catchError(this.handleError<ObservationElementMapTwoWayBinding>(`getAudits id = ${audit_record_id}`))
    )
  }

  getDomainAuditStats(audit_record_id: number): Observable<AuditStats> {
    const url = `${this.auditRecordsUrl}/${audit_record_id}/stats`;
    
    return this.http.get<AuditStats>(url).pipe(
      tap(() => this.log(`fetched audit stats for audit record = ${audit_record_id}`)),
      catchError(this.handleError<AuditStats>(`getAuditStats id = ${audit_record_id}`))
    )
  }

  getAuditsByPage(page_url: string): Observable<PageAudits>  {
    const url = `${this.auditsUrl}/pages`;

    return this.http.get<PageAudits>(url, {params: {url: page_url}}).pipe(
      tap(() => this.log(`fetched page audits for domain = ${page_url}`)),
      catchError(this.handleError<PageAudits>(`getAudit id = ${page_url}`))
    )
  }

  /**
   * Retrieve page audits for the audit record with the given id
   * 
   * @param id audit record id
   * @returns array of AuditRecords
   */
  getPageAuditsForDomainAudit(id: number): Observable<AuditRecord[]>  {
    const url = `${this.auditsUrl}/${id}`;

    return this.http.get<AuditRecord[]>(url).pipe(
      tap(() => this.log(`fetched page audits for domain record = ${id}`)),
      catchError(this.handleError<AuditRecord[]>(`getAudits id = ${id}`))
    )
  }

  getMostRecentDomainAudit(host: string): Observable<PageAudits[]>  {
    const url = `${this.domainUrl}/audits`;

    return this.http.get<PageAudits[]>(url, {params: {host: host}}).pipe(
      tap(() => this.log(`fetched page audits for domain = ${host}`)),
      catchError(this.handleError<PageAudits[]>(`getAudits id = ${host}`))
    )
  }

  addRecommendation(
    audit_key: string,
    recommendation: string
  ): Observable<string> {
    const url = `${this.auditsUrl}/${audit_key}/recommendations/add`;
    console.log("URL   :   " + url)

    return this.http.post<string>(url, recommendation)
                .pipe(
                  catchError(this.handleError('addHero', recommendation))
                );
  }

  addObservationRecommendation(
    observation_key: string, 
    recommendation: string
  ): Observable<string> {
    const url = `${this.observationUrl}/${observation_key}/recommendations/add`;
    console.log("URL   :   " + url)

    return this.http.post<string>(url, recommendation)
                .pipe(
                  catchError(this.handleError('addHero', recommendation))
                );
  }

  deleteRecommendation(
    observation_key: string,
    recommendation: string
  ): Observable<ArrayBuffer> {
    const url = `${this.observationUrl}/${observation_key}/recommendations`;
    console.log("URL   :   " + url)

    return this.http.delete<ArrayBuffer>(url, {params: {recommendation: recommendation}})
                
  }

  addObservation(
    type: string,
    description: string, 
    recommendations: string[],
    why_it_matters: string,
    ada_compliance: string,
    priority: string,
    audit_key: string
  ): Observable<string> {
    const url = `${this.auditsUrl}/${audit_key}/observations`;
    console.log("URL   :   " + url)

    return this.http.post<string>(url, { description, why_it_matters, ada_compliance, priority, recommendations, type })
                .pipe(
                  catchError(this.handleError('addHero', "An error occurred while saving UX issue"))
                );
  }

  getOverallScore(audits:Audit[]) : number {
    let points = 0;
    let max_points = 0;

    audits.forEach((item) => {
      points += item.points;
      max_points += item.totalPossiblePoints;
    })
    return (points / max_points);
  }

  requestReport(email_report: FormGroup, audit_record_id: number): Observable<string> {
    const url = `${this.auditRecordsUrl}/${audit_record_id}/report`;
    
    this.segmentio.trackReportRequested(audit_record_id, email_report.value)
    return this.http.post<string>(url, email_report.value )
  }

  getPage(audit_record_id: number): Observable<SimplePage[]> {
    const url = `${this.auditRecordsUrl}/${audit_record_id}/pages`;
    
    return this.http.get<SimplePage[]>(url).pipe(
      tap(() => this.log(`fetched audit for domain = ${audit_record_id}`)),
      catchError(this.handleError<SimplePage[]>(`getAudit id = ${audit_record_id}`))
    )
  }

  getAuditStats(audit_record_id: number): Observable<AuditStats> {
    const url = `${this.auditRecordsUrl}/${audit_record_id}/stats`;
    
    return this.http.get<AuditStats>(url).pipe(
      tap(() => this.log(`fetched audit stats for audit record = ${audit_record_id}`)),
      catchError(this.handleError<AuditStats>(`getAuditStats id = ${audit_record_id}`))
    )
  }

  /**
   * Handle Http operation that failed.
   * Let the app continue.
   * @param operation - name of the operation that failed
   * @param result - optional value to return as the observable result
   */
  private log(message: string){
    this.messageService.add(`AuditService: ${message}`);
  }

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
