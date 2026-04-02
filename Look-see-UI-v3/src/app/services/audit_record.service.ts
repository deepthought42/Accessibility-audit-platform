import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, of } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuditRecord } from '../models/auditRecord';
import { MessageService } from './message.service';

@Injectable({
  providedIn: 'root'
})
export class AuditRecordService {
  private http = inject(HttpClient);
  private messageService = inject(MessageService);
  private apiUrl = environment.api_url;

  getAuditRecords(): Observable<AuditRecord[]> {
    return this.http.get<AuditRecord[]>(`${this.apiUrl}/auditrecords`);
  }

  /** Log a AuditRecordService message with the MessageService */
  private log(message: string){
    this.messageService.add(`AuditRecordService: ${message}`);
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
