import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { MessageService } from './message.service';

@Injectable({
  providedIn: 'root'
})
export class ReportService {
  private http = inject(HttpClient);
  private messageService = inject(MessageService);
  private apiUrl = environment.api_url;
  private demo_ip = "api-demo.look-see.com"
  private dev_ip = "localhost"

  private reports_url = this.apiUrl+'/audits'  // URL to web api

  getReport(key: string): Observable<string> {
    return this.http.get<string>(this.reports_url+"/report/excel", {params: {'page_state_key': key}})
      .pipe(
        tap(() => this.log('fetched report')),
        catchError(this.handleError<string>('getReport', 'this is a result'))
      );
  }

  /** Log a ReportService message with the MessageService */
  private log(message: string){
    this.messageService.add(`ReportService: ${message}`);
  }

  getExcelReport(key: string): Observable<Blob> {
    return this.http.get(this.reports_url+"/"+sessionStorage.getItem("audit_record_id")+"/report/excel", {responseType: 'blob', params: {'page_state_key': key}})
      .pipe(
        tap(() => this.log('fetched report')),
        catchError(this.handleError<Blob>('getReport', {} as Blob))
      );
  }

  getPDFReport(key: string): Observable<Blob> {
    return this.http.get(this.reports_url+"/"+sessionStorage.getItem("audit_record_id")+"/report/pdf", {responseType: 'blob', params: {'page_state_key': key}})
      .pipe(
        tap(() => this.log('fetched report')),
        catchError(this.handleError<Blob>('getReport', {} as Blob))
      );
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
