import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { AuthService } from '@auth0/auth0-angular';
import { faCaretDown, faCaretRight, faFileExcel, faFilePdf, faFrown, faMeh, faSmileBeam, faUser } from '@fortawesome/free-solid-svg-icons';
import { Domain } from '../../models/domain/domain';
import { DomainService } from '../../models/domain/domain.service';

@Component({
  selector: 'app-domains',
  templateUrl: './domains.component.html',
  styleUrls: ['./domains.component.scss']
})
export class DomainsComponent implements OnInit, OnDestroy {
  domain: Domain  = {} as Domain
  domains: Domain[] = []

  faUser = faUser
  faCaretRight = faCaretRight
  faCaretDown = faCaretDown
  faSmileBeam = faSmileBeam
  faMeh = faMeh
  faFrown = faFrown
  
  faFilePdf = faFilePdf
  faFileExcel = faFileExcel
  interval_id: number | undefined // TODO: Fix type

  isAddDomainModalDisplayed = false;
  login_message = "Sign up for FREE"

  auth = inject(AuthService);
  domain_service = inject(DomainService);

  ngOnInit(): void {
   // window.analytics.page('Domains Page')
    
    //get domains
    this.domain_service.getDomains().subscribe((data) => {
      console.log("received domains")
      this.domains = data
    })
  }

  ngOnDestroy() {
    if (this.interval_id) {
      clearInterval(this.interval_id);
    }
  }

  showDomainsModal(): void{
    this.isAddDomainModalDisplayed = true;
  }
}