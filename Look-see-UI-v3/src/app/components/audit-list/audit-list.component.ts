import { ChangeDetectorRef, Component, Input, OnInit, inject } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { ThemePalette } from '@angular/material/core';
import { ProgressSpinnerMode } from '@angular/material/progress-spinner';
import { Router } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';
import { faCircleCheck } from '@fortawesome/free-regular-svg-icons';
import { faChevronRight, faCircleRadiation, faExclamationCircle, faFrown, faMagnifyingGlass, faMeh, faPencil, faSmileBeam, faSpinner, faTriangleExclamation } from '@fortawesome/free-solid-svg-icons';
import { AuditRecord } from '../../models/auditRecord';
import { AuditStatus, auditStatusChipKind, normaliseAuditStatus } from '../../models/audit-status';
import { Domain } from '../../models/domain/domain';
import { AuditService } from '../../services/audit.service';
import { SegmentIOService } from '../../services/segmentio.service';
import { WebSocketService } from '../../services/websocket/web-socket.service';


@Component({
  selector: 'app-audit-list',
  templateUrl: './audit-list.component.html',
  styleUrl: './audit-list.component.css'
})
export class AuditListComponent implements OnInit {
  @Input()
  public data?: AuditRecord

  faSmileBeam = faSmileBeam
  faMeh = faMeh
  faFrown = faFrown
  faSpinner = faSpinner
  faChevronRight = faChevronRight
  faPencil = faPencil
  faCircleCheck = faCircleCheck
  faCircleExclamation = faExclamationCircle
  faCircleRadiation = faCircleRadiation
  faTriangleExclamation = faTriangleExclamation
  faMagnifyingGlass = faMagnifyingGlass

  color: ThemePalette = 'primary'
  accent_color: ThemePalette = 'accent'
  error_color: ThemePalette = "warn"

  mode: ProgressSpinnerMode = 'indeterminate'
  determinate_mode: ProgressSpinnerMode = 'determinate'

  isLoading = false
  isAuditStarted = false;
  //loading bar config
  diameter = 50

  domain_error = false
  error = ""
  is_loading_audits = false;

  audit_records: AuditRecord[] = []
  domains: Domain[] = []
  isDomainSettingsDisplayed = false
  
  user_channel_name = ""
  audit_msg = ""
  
  types = [
    'Page', 'Domain'
  ];
  
  form = new FormGroup({
    type: new FormControl(this.types[0]),
    url: new FormControl(),
  });

  router = inject(Router);
  auth = inject(AuthService);
  audit_service = inject(AuditService);
  websocketService = inject(WebSocketService);
  changeDetection = inject(ChangeDetectorRef);
  segmentio = inject(SegmentIOService);

  ngOnInit(): void {
    this.is_loading_audits=true
    this.audit_service.getAudits().subscribe((data) => {
      console.log("retrieved audit records : "+data)
      this.audit_records = data
      this.is_loading_audits=false
    })
  }

  ngOnDestry(): void {
    this.websocketService.unsubscribeChannel(this.user_channel_name)
  }

  selectAudit(audit_record:AuditRecord){
    if(audit_record.level=="PAGE"){
      this.router.navigate(['/audit/'+audit_record.id+'/page']);
    }
    else{
      this.router.navigate(['/audit/'+audit_record.id+'/domain']);
    }
  }

  addItem(audit_record:AuditRecord){
    this.audit_records.push(audit_record)
  }

  viewSettings(): void {
    alert("It looks like this feature isn't done yet. If you have opinions or ideas on what should be included in domain settings, please email our founder at bkindred@look-see.com.")
  }

  viewPagesAudited(): void {
    alert("It looks like this feature isn't done yet. If you have opinions or ideas on what should be included in domain settings, please email our founder at bkindred@look-see.com.")
  }

  /**
   * Derive the canonical AuditStatus for a given record. Used by the template
   * to pass a typed `kind` into <app-looksee-status-chip>.
   *
   * See docs/design/02-audit-status-and-progress.md §1.
   */
  statusFor(record: AuditRecord): ReturnType<typeof auditStatusChipKind> {
    const hasScores = (record.contentAuditScore ?? 0) > 0
      || (record.infoArchScore ?? 0) > 0
      || (record.aestheticScore ?? 0) > 0;
    const status: AuditStatus = normaliseAuditStatus(record.status, { hasScores });
    return auditStatusChipKind(status);
  }
}
