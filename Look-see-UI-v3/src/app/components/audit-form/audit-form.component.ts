import { Component, EventEmitter, Output, inject } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { ThemePalette } from '@angular/material/core';
import { ProgressBarMode } from '@angular/material/progress-bar';
import { Router } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';
import { faCog, faFrown, faMeh, faSmileBeam, faSpinner } from '@fortawesome/free-solid-svg-icons';
import { AuditRecord } from '../../models/auditRecord';
import { DomainService } from '../../models/domain/domain.service';
import { AuditorService } from '../../services/auditor.service';
import { SegmentIOService } from '../../services/segmentio.service';

@Component({
  selector: 'app-audit-form',
  templateUrl: './audit-form.component.html',
  styleUrl: './audit-form.component.css'
})
export class AuditFormComponent {
  @Output()
  public auditRecordData: EventEmitter<AuditRecord> = new EventEmitter<AuditRecord>();

  faSmileBeam = faSmileBeam
  faMeh = faMeh
  faFrown = faFrown
  faSpinner = faSpinner
  faCog = faCog

  isLoading = false
  isAuditStarted = false;
  //loading bar config
  color: ThemePalette = 'primary'
  mode: ProgressBarMode = 'indeterminate'
  value = 50
  diameter = 50

  domain_error = false
  error = ""

  audit_records: AuditRecord[] = []
  isDomainSettingsDisplayed = false

  types = [
    'Page', 'Domain'
  ];
  
  form = new FormGroup({
    type: new FormControl<string>(this.types[0].toUpperCase(), {
      asyncValidators: [],
      validators: [],
      nonNullable: true,
    }),
    url: new FormControl(),
  });

  @Output() error_emitted_event = new EventEmitter<string>();

  router = inject(Router);
  auth = inject(AuthService);
  domain_service = inject(DomainService);
  auditorService = inject(AuditorService);
  segmentio = inject(SegmentIOService);
  
  startAudit(): void {
    this.isLoading = true;

    console.log("audit type = "+this.form.value.type)
    console.log("URL Value :: "+this.form.value.url)
    this.segmentio.sendDomainUxAuditStartedMessage(this.form.value.url)
    this.initiateAudit(this.form.value.url, this.form.value.type?.toUpperCase() ?? "");
  }

  /**
   * 
   * @param domain 
   */
  initiateAudit(url: string, type: string): void {
    // sessionStorage.setItem("host", domain.replace("https://", "").replace("http://", ""));
    if(!url || !url.length) {
      this.domain_error = true
      this.isLoading = false
      this.error = "URL cannot be blank"
    }
    else if(!this.isValidURL(url)) {
      this.domain_error = true
      this.isLoading = false
      this.error = "URL must be valid. Please be sure to follow the format https://www.example.com"
    }
    else {
      this.isLoading = true
      sessionStorage.setItem("url", url.replace("https://", "").replace("http://", ""));
      this.auditorService.startAudit(url, type).subscribe(
        (data) => {
          this.segmentio.sendUxAuditStartedMessage(url)
          console.log("starting " + type + " audit for url "+url)
          this.isLoading = false
          //this.audit_records.push(data)
          this.auditRecordData.emit(data)

          //send data to audit list
        },
        () => {
          this.error = "uh oh...it looks like our servers decided to take a coffee break. Please wait a minute then try again.";
          this.isLoading = false
        }
      );
    }
  }

  isValidURL(domain_url: string): boolean {
    const urlregex = /^((https?|ftp):\/\/)?([a-zA-Z0-9.-]+(:[a-zA-Z0-9.&%$-]+)*@)*((25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9][0-9]?)(\.(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])){3}|([a-zA-Z0-9-]+\.)*[a-zA-Z0-9-]+\.(com|app|edu|gov|int|mil|net|org|biz|arpa|info|name|pro|aero|coop|museum|website|space|[a-zA-Z]{2}))(:[0-9]+)*(\/($|[a-zA-Z0-9.,?'\\+&%$#=~_-]+))*$/;
    return urlregex.test(domain_url);
  }

  /* ERROR HANDLING METHODS */
  addError(error: string): void {
    this.error_emitted_event.emit(error);
  }
}
