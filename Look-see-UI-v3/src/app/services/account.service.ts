import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, of } from 'rxjs';
import { environment } from '../../environments/environment';
import { Account } from '../models/account';
import { MessageService } from './message.service';

@Injectable({
  providedIn: 'root'
})
export class AccountService {
  private http = inject(HttpClient);
  private env_url = environment.api_url+'/accounts'  // URL to web api
  private messageService = inject(MessageService);

  /** Log a AuditService message with the MessageService */
  private log(message: string){
    this.messageService.add(`AuditService: ${message}`);
  }

  getAccountDetails(): Observable<Account> {
    return this.http.get<Account>(this.env_url)
    /*
      .pipe(
        catchError(this.handleError<Account>('failed to retrieve account details', {} as Account))
      );
      */
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
