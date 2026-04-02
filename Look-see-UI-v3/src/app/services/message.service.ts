import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

export interface Message {
  content: string;
  style?: string;
}

@Injectable({
  providedIn: 'root'
})
export class MessageService {
  private messageSource = new Subject<Message>();
  public messages$ = this.messageSource.asObservable();

  add(message: string, style = 'info') {
    this.messageSource.next({ content: message, style });
  }

  clear() {
    this.messageSource.next({ content: '' });
  }
}
