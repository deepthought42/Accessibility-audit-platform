import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, of } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MessageService } from '../../services/message.service';
import { Competitor } from './competitor';

@Injectable({
  providedIn: 'root'
})
export class CompetitorService {
  private http = inject(HttpClient);
  private messageService = inject(MessageService);
  private apiUrl = environment.api_url;

  startAnalysis(competitor_id: number) {
    return this.http.get<void>(`${this.apiUrl}/competitors/${competitor_id}`);
  }

  getCompetitors(): Observable<Competitor[]> {
    return this.http.get<Competitor[]>(`${this.apiUrl}/competitors`);
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
