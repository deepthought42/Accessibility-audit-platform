import { Injectable } from '@angular/core';
import Pusher from 'pusher-js';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private pusher_key = environment.pusher.key;
  private pusher_cluster = environment.pusher.cluster;

  //Pusher Variable
  pusher: Pusher;
  channel: unknown;

  constructor() {
    this.pusher = new Pusher(this.pusher_key, {
      cluster: this.pusher_cluster,
    });
  }

  /**
   * Listen to Channels and Subscribe them
   *
   * event_name : ('audit-update', etc) 
   * 
   */
  listenChannel(channel_name:string, event_name:string, callback:(data: string) => void){
    console.log("subscribing to pusher")
    //console.log("binding to channel "+channel_name )
    const channel = this.pusher.subscribe(channel_name);
    console.log("Subscribed to pusher successuflly "+channel_name)
    channel.bind(event_name, callback);
  }

  // Execute function after connected
  connectedExecute() {
    /*
      Your code here
     */
  }

   // Execute function after connected
   disconnectedExecute() {
    /*
      Your code here
     */
  }

  // Unsubscribe channels
  unsubscribeChannel(names: string | string[]) {
    const channelNames = Array.isArray(names) ? names : [names];

    for (const name of channelNames) {
      this.pusher.unsubscribe(name)

      /* Logging for debug. Kept for debugging when needed
      Pusher.log = msg => {
        console.log(msg);
      }
      */
      this.pusher.connection.bind("disconnected", this.disconnectedExecute);
    }
/*
    this.pusher.allChannels().forEach((channel: { name: any; }) => {
      this.pusher.unsubscribe(channel.name)
      console.log("Unsubscribe: ", channel.name);
    })
    */
  }
}
