import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';
import { faCaretDown, faCaretRight, faFrown, faMeh, faSmileBeam, faUser } from '@fortawesome/free-solid-svg-icons';

@Component({
  selector: 'app-how-it-works',
  templateUrl: './how-it-works.component.html',
  styleUrls: ['./how-it-works.component.scss']
})
export class HowItWorksComponent {
  auth = inject(AuthService);
  router = inject(Router);
  user_menu_displayed = false
  faUser = faUser

  display = false;

  faCaretRight = faCaretRight
  faCaretDown = faCaretDown
  faSmileBeam = faSmileBeam
  faMeh = faMeh
  faFrown = faFrown
  
  aesthetics = false
  info_architecture = false
  content = false
  interactions = false
  select_all = false

  showAccountPage(): void{
    this.router.navigateByUrl('/account').then(e => {
      if (!e) {
        console.log("Uh oh! It looks like we misplaced that page. Please wait a moment and then try again");
      }
    });
  }

  showSettingsPage(): void {
    this.router.navigateByUrl('/settings').then(e => {
      if (!e) {
        console.log("Uh oh! It looks like we misplaced that page. Please wait a moment and then try again");
      }
    });
  }
}