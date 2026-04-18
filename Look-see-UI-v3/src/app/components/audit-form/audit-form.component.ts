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
import { normaliseUrl } from '../../utils/url';

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
    const normalised = normaliseUrl(url);
    if (!normalised.ok) {
      this.domain_error = true;
      this.isLoading = false;
      this.error = normalised.error;
      return;
    }

    const cleanUrl = normalised.url;
    this.isLoading = true;
    sessionStorage.setItem('url', cleanUrl.replace(/^https?:\/\//, ''));
    this.auditorService.startAudit(cleanUrl, type).subscribe(
      (data) => {
        this.segmentio.sendUxAuditStartedMessage(cleanUrl);
        this.isLoading = false;
        this.auditRecordData.emit(data);
      },
      () => {
        this.error = "Something went wrong. Try again in a moment.";
        this.isLoading = false;
      }
    );
  }

  /**
   * @deprecated use `normaliseUrl()` from utils/url.
   */
  isValidURL(domain_url: string): boolean {
    return normaliseUrl(domain_url).ok;
  }

  /* ERROR HANDLING METHODS */
  addError(error: string): void {
    this.error_emitted_event.emit(error);
  }
}
