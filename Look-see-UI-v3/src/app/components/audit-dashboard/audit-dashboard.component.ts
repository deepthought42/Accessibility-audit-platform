import { ChangeDetectorRef, Component, EventEmitter, OnInit, Output, inject } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { ThemePalette } from '@angular/material/core';
import { ProgressSpinnerMode } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';
import { faCircleCheck } from '@fortawesome/free-regular-svg-icons';
import { faCaretDown, faCaretRight, faChevronRight, faCircleRadiation, faDraftingCompass, faExclamationCircle, faFrown, faInfoCircle, faMeh, faPencil, faSmileBeam, faSpinner, faTriangleExclamation } from '@fortawesome/free-solid-svg-icons';
import auditCategoriesConfig from '../../config/audit_categories.json';
import { Audit } from '../../models/audit';
import { AuditRecord } from '../../models/auditRecord';
import { Domain } from '../../models/domain/domain';
import { DomainAuditStats } from '../../models/domain_audit_stats';
import { AuditService } from '../../services/audit.service';
import { SegmentIOService } from '../../services/segmentio.service';
import { WebSocketService } from '../../services/websocket/web-socket.service';

@Component({
  selector: 'app-audit-dashboard',
  templateUrl: './audit-dashboard.component.html',
  styleUrl: './audit-dashboard.component.css'
})
export class AuditDashboardComponent implements OnInit {
  faSmileBeam = faSmileBeam
  faMeh = faMeh
  faFrown = faFrown
  faSpinner = faSpinner
  faChevronRight = faChevronRight
  faPencil = faPencil
  faCircleCheck = faCircleCheck
  faCircleExclamation = faExclamationCircle
  faCaretRight = faCaretRight
  faCaretDown = faCaretDown
  faInfoCircle = faInfoCircle
  faDraftingCompass = faDraftingCompass
  faTriangleExclamation = faTriangleExclamation
  faCircleRadiation = faCircleRadiation

  domain_error = false
  error = ""
  href = ''
  color: ThemePalette = 'primary'
  accent_color: ThemePalette = 'accent'
  error_color: ThemePalette = "warn"

  mode: ProgressSpinnerMode = 'indeterminate'
  determinate_mode: ProgressSpinnerMode = 'determinate'
  value = 50
  diameter100 = 100
  diameter50 = 50
  diameter80 = 80
  diameter150 = 150
  audit = {type: "page", url: ""}

  x_axis: string[] = []
  y_axis: number[] = []

  issue_severity_x_axis: string[] = []
  issue_severity_y_axis: number[] = []

  private _selected: Domain = {} as Domain

  audit_stats: DomainAuditStats = {} as DomainAuditStats
  audit_records: AuditRecord[] = []

  types = [
    'Page', 'Domain'
  ];

  @Output() error_emitted_event = new EventEmitter<string>();

  @Output() audit_started_event = new EventEmitter<{is_audit_started: boolean, audit_record_id: number, url: string}>();

  form = new FormGroup({
    type: new FormControl(this.types[0]),
    url: new FormControl(),
  });

  isLoading = false
  is_loading_audits = false
  panelOpenState = true
  audits: Audit[] = []
  host = ''

  dropdownPopoverShow = false
  isAuditStarted = false;
  current_url = ''
  audit_record_id = -1

  user_channel_name=""
  // array of categories
  audit_categories = auditCategoriesConfig.audit_categories_config

  domain_stat = {
    full_site_score: 94.12345
  }
  
  router = inject(Router);
  webSocketService = inject(WebSocketService);
  audit_service = inject(AuditService);
  changeDetection = inject(ChangeDetectorRef);
  auth = inject(AuthService);
  segmentio = inject(SegmentIOService);
  route = inject(ActivatedRoute);

  ngOnInit(): void {
    //this.audit_stats = stats //TEMPORARY

    this.audit_record_id= this.route.snapshot.params['id'];
    this.audit_service.getAuditStats(this.audit_record_id)
      .subscribe({
        next: (audit_stat_data:DomainAuditStats) => {
          console.log("audit stats = "+JSON.stringify(audit_stat_data))
          this.audit_stats = audit_stat_data
          this.issue_severity_x_axis = ['Low', 'Medium', 'High']
          this.changeDetection.detectChanges()
        },
        error: (err) => console.log("An error occurred while getting stats for the domain "+this._selected.url+" -  "+err),
        complete: () => {
          this.isLoading = false;
        }
      })
    
    //retrieve all audit records for this domain
    this.audit_service.getPageAuditsForDomainAudit(this.audit_record_id).subscribe((data) => {
      console.log("retrieved audit records : "+data)
      this.audit_records = data
      this.is_loading_audits=false
    })

    this.auth.user$.subscribe((data) => {
      console.log("subscribing....")
      if(data?.sub){
        console.log("data sub = "+data?.sub)
        this.user_channel_name = data?.sub.replace("|", "")
        console.log("user channel = "+this.user_channel_name)
        this.webSocketService.listenChannel( this.user_channel_name , 'auditUpdate',
          (response)=>{
            const audit_msg = JSON.parse(response)
            console.log("audit update msg = "+audit_msg)
            sessionStorage.setItem("audit_stats", response)
          }
        )

        this.webSocketService.listenChannel( this.user_channel_name , 'pageFound',
          (response)=>{
            const audit_msg = JSON.parse(response)
            console.log("page Found msg = "+audit_msg)
            this.audit_records.push(audit_msg)
            sessionStorage.setItem("audit_stats", response)
          }
        )
      }
    });
  }

  ngOnDestry(): void {
    this.webSocketService.unsubscribeChannel(this.user_channel_name)
  }

  selectAudit(audit_record:AuditRecord){
    this.router.navigate(['/audit/'+audit_record.id+'/page']);
  }

  toggleTooltip() {
    if (this.dropdownPopoverShow) {
      this.dropdownPopoverShow = false;
    } else {
      this.dropdownPopoverShow = true;
    }
  }

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

  getScore(audit:Audit){
    return (audit.points/audit.totalPossiblePoints || 0 ) * 100;
  }

  getDomainScore(subcategory:string){
    //calculate average of all matching scores and return
    let total = 0;
    let match_count = 0;
    for(const audit of this.audits){
      if(audit.subcategory === subcategory){
        total += (audit.points/audit.totalPossiblePoints || 0 ) * 100;
        match_count++;
      }
    }

    return (total / match_count) || 0;
  }

  isAuditRunning(): boolean {
    return this.audit_stats.status === "IN_PROGRESS";
  }

  /**
   * TODO: MOVE TO SINGLE PAGE LOADING SCREEN 
  getProgressPercentage(): number {
    return Math.floor(((this.audit_stats.aestheticAuditProgress + this._selected.dataExtractionProgress + this._selected.contentAuditProgress + this._selected.infoArchitectureAuditProgress)/4.0) * 100)
  }
  */

  getCategoryScore(category:string): number{
    //calculate average of all matching scores and return
    let total = 0;
    let match_count = 0;
    for(const audit of this.audits){
      if(audit.category === category){
        total += (audit.points/audit.totalPossiblePoints || 0 )*100;
        match_count++;
      }
    }
    return (total / match_count) || 0;
  }

  //audit messages to use for timeout
  audit_messages: string[] = [
    "Crawling site",
    "Reviewing webpage",
    "Breaking down page structure",
    "Analyzing html elements",
    "Building observations",
    "Completed audit on webpage"
  ]

  //index variables for different categories
  color_idx = 0
  content_idx = 0
  visuals_idx = 0
  info_architecture_idx = 0
  performance_idx = 0
  typography_idx = 0


  showAuditUpdateMessage(category_index: number, index: number): void{
    if(this.audit_categories != undefined && this.audit_categories[category_index] != undefined) {
      this.audit_categories[category_index].loading_msg = this.audit_messages[index]
    }
  }
  
  //timeout to show different loading messages
  show_loading_messages(): void {
    const category_index = Math.floor(Math.random() * 3)
    const msg_index = Math.floor(Math.random() * 3)
    this.showAuditUpdateMessage(category_index, msg_index)
  }

  //TRACKING METHODS
  sendAuditViewedMessage(audit_name: string){
    window.analytics.track('UX Audit Viewed', {
      name: audit_name
    });
  }  
  
  isNaN(value: number){
    return isNaN(value)
  }

  //DUMMY data for testing
  getDomainAudits(): Audit[] {
    return [
      {
        id: 1,
        subcategory: 'Non-Text Contrast',
        category: 'Color Management',
        name: 'Non-Text Contrast',
        points: 25,
        totalPossiblePoints: 84,
        level: 'domain',
        url: 'https://www.look-see.com/search',
        recommendations: [],
        createdAt: '2021-02-02T16:28:35.833',
        key: 'audit::50c7399b451cc38dd048561d1e2ef1c362e858f0c9d74665b546900efa75f626',
        messages: [],
        whyItMatters: '',
        adaCompliance: ''
      },
      {
        id: 2,
        name: 'Text Contrast',
        category: 'Color Management',
        subcategory: 'Text Contrast',
        points: 42,
        totalPossiblePoints: 111,
        level: 'domain',
        url: '',
        recommendations: [],
        createdAt: '2021-02-02T16:28:35.833',
        key: 'audit::textcontrast',
        messages: [],
        whyItMatters: '',
        adaCompliance: ''
      },
      {
        id: 3,
        name: 'Color Palette',
        category: 'Color Management',
        subcategory: 'Color Palette',
        points: 42,
        totalPossiblePoints: 111,
        level: 'domain',
        url: '',
        recommendations: [],
        createdAt: '2021-02-02T16:28:35.833',
        key: 'audit::colorpalette',
        messages: [],
        whyItMatters: '',
        adaCompliance: ''
      },
      {
        id: 4,
        name: 'Ease of Understanding',
        category: 'Written Content',
        subcategory: 'Ease of Understanding',
        points: 42,
        totalPossiblePoints: 111,
        level: 'domain',
        url: '',
        recommendations: [],
        createdAt: '2021-02-02T16:28:35.833',
        key: 'audit::easeofunderstanding',
        messages: [],
        whyItMatters: '',
        adaCompliance: ''
      },
      {
        id: 5,
        name: 'Paragraphing',
        category: 'Written Content',
        subcategory: 'Paragraphing',
        points: 42,
        totalPossiblePoints: 111,
        level: 'domain',
        url: '',
        recommendations: [],
        createdAt: '2021-02-02T16:28:35.833',
        key: 'audit::paragraphing',
        messages: [],
        whyItMatters: '',
        adaCompliance: ''
      },
      {
        id: 6,
        name: 'Logo Positioning',
        category: 'Branding',
        subcategory: 'Logo Positioning',
        points: 42,
        totalPossiblePoints: 111,
        level: 'domain',
        url: '',
        recommendations: [],
        createdAt: '2021-02-02T16:28:35.833',
        key: 'audit::logopositioning',
        messages: [],
        whyItMatters: '',
        adaCompliance: ''
      }
    ]
  }

  /* ERROR HANDLING METHODS */
  addError(error: string): void {
    this.error_emitted_event.emit(error);
  }

  /**
   * 
   * @param title 
   * @returns 
   */
  getHistoricalLayout(title: string): Record<string, unknown>{
    return { title: title,
            margin: {t:40, b:40, l:56, r:56},
            xaxis: {
                autorange: true,
                fixedrange: true,
                type: 'date'
            },
            yaxis: {
                autorange: true,
                fixedrange: true,
                range: [0, 100],
                type: 'linear'
            } 
    }
  }

  getLayoutForRiskChart(title: string): Record<string, unknown>{
    return {  title: title,
              margin: {t:40, b:40, l:56, r:56},
              barmode: 'group',
              xaxis: {
                fixedrange: true,
              },
              yaxis: {
                fixedrange: true,
              }
    }
  }
}
