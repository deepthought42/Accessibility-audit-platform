import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { AuditRecord } from '../models/auditRecord';
import { MessageService } from './message.service';

@Injectable({
  providedIn: 'root'
})
export class AuditorService {
  private http = inject(HttpClient);
  private messageService = inject(MessageService);
  private api_url = environment.api_url;

  private demo_ip = "35.226.217.125"
  private auditor_url = environment.api_url+'/auditor'  // URL to web api
  
  startIndividualAudit(audit_url: string): Observable<AuditRecord> {
    const url = `${this.auditor_url}/start-individual`;
    console.log("auditor url to run :: "+audit_url)
    return this.http.post<AuditRecord>(url, {'url': audit_url})
  }
  /**
   * Sends request to API to start an audit of the given type (PAGE or DOMAIN)
   * 
   * @param url 
   * @param type 
   * @returns 
   */
  startAudit(url: string, type: string): Observable<AuditRecord> {
    const api_url = `${this.auditor_url}/start`;
    console.log("auditor url to run :: "+url)
    return this.http.post<AuditRecord>(api_url, {'url': url, 'type': type})
        .pipe(
          tap(() => this.log('started audit')),
          catchError(this.handleError<AuditRecord>('startAudit', {} as AuditRecord))
        );
  }

  /** Log a AuditService message with the MessageService */
  private log(message: string){
    this.messageService.add(`AuditorService: ${message}`);
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
