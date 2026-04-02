import { DOCUMENT } from '@angular/common';
import { Component, inject } from '@angular/core';
import { AuthService } from '@auth0/auth0-angular';
import { SegmentIOService } from '../../services/segmentio.service';
@Component({
  selector: 'app-auth-button',
  templateUrl: './auth-button.component.html',
  styleUrls: ['./auth-button.component.css']
})
export class AuthButtonComponent {
  auth = inject(AuthService);
  document = inject(DOCUMENT);
  segmentio = inject(SegmentIOService);
  
  loginWithRedirect() {
    this.auth.loginWithRedirect({
      appState: {
      },
      authorizationParams: {
        screen_hint: 'signup',
        any_custom_property: 'value'
      }
    });
    this.segmentio.trackLoginClick();
  }

  logout() {
    this.auth.logout({ logoutParams: { returnTo: this.document.location.origin } });
  }
}
