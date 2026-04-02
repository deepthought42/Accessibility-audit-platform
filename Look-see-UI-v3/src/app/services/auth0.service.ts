import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { AuthService } from '@auth0/auth0-angular';
import { Observable, of } from 'rxjs';
import { MessageService } from './message.service';
import { SegmentIOService } from './segmentio.service';

@Injectable({
  providedIn: 'root'
})
export class Auth0Service {
  private auth = inject(AuthService);
  private http = inject(HttpClient);
  private messageService = inject(MessageService);
  private segmentio = inject(SegmentIOService);

  /** Log a AuditService message with the MessageService */
  private log(message: string){
    this.messageService.add(`AuditService: ${message}`);
  }

  loginWithRedirect(): void {
    this.segmentio.trackLoginClick()
    this.auth.loginWithRedirect()
  }

  loginWithPopup(): void {
    this.segmentio.trackLoginClick()
    this.auth.loginWithPopup()
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
