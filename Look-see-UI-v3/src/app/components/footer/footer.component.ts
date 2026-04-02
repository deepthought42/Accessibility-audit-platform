import { Component, inject } from '@angular/core';
import { AuthService } from '@auth0/auth0-angular';
import { faCopyright } from '@fortawesome/free-regular-svg-icons';

@Component({
  selector: 'app-footer',
  templateUrl: './footer.component.html',
  styleUrls: [ './footer.component.scss'
  ]
})
export class FooterComponent {
  faCopyright = faCopyright
  auth = inject(AuthService);
  currentYear = new Date().getFullYear();
}
