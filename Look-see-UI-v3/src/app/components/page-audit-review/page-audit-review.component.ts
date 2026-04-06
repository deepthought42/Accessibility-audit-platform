import { Location } from '@angular/common';
import { AfterViewInit, Component, ElementRef, OnDestroy, OnInit, Renderer2, ViewChild, inject } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { ThemePalette } from '@angular/material/core';
import { MatDialog } from '@angular/material/dialog';
import { ProgressSpinnerMode } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';
import { faCircleCheck, faCompass, faImage, faTrashAlt } from '@fortawesome/free-regular-svg-icons';
import { faAngleDown, faAngleUp, faCaretRight, faCircle, faCircleRadiation, faDownload, faFileExcel, faFilePdf, faFrown, faInfoCircle, faMeh, faPalette, faPlus, faSearch, faSitemap, faSmileBeam, faSpinner, faTriangleExclamation, faUniversalAccess, faUser } from '@fortawesome/free-solid-svg-icons';
import { throwError } from 'rxjs/internal/observable/throwError';
import { catchError } from 'rxjs/internal/operators/catchError';
import { ObservationElementMapTwoWayBinding } from '../../models/ObservationElementMapTwoWayBinding';
import { Audit } from '../../models/audit';
import { AuditRecord } from '../../models/auditRecord';
import { AuditScore } from '../../models/audit_score';
import { AuditStats } from '../../models/audit_stats';
import { ColorPaletteObservation } from '../../models/color-palette-observation';
import { PageAudits } from '../../models/page_audits';
import { Priority } from '../../models/priority';
import { SimpleElement } from '../../models/simple_element';
import { SimplePage } from '../../models/simple_page';
import { UXIssueMessage } from '../../models/ux_issue_message';
import { FilterByCategoryPipe } from '../../pipes/category_filter.pipe';
import { FilterSeverityPipe } from '../../pipes/severity_filter.pipe';
import { AuditService } from '../../services/audit.service';
import { AuthorizationService } from '../../services/authorization.service';
import { ReportService } from '../../services/report.service';
import { SegmentIOService } from '../../services/segmentio.service';
import { ElementInfoDialog } from '../element-info-dialog/element-info-dialog';
import { StartSinglePageAuditConfirmation } from '../start-audit-confirm-dialog/start-audit-confirm-dialog';
import { StartSinglePageAuditLoginRequired } from '../start-audit-login-required-dialog/start-audit-login-required-dialog';

@Component({
  selector: 'app-page-audit-review',
  templateUrl: './page-audit-review.component.html',
  styleUrls: ['./page-audit-review.component.scss'],
  providers: [FilterByCategoryPipe, FilterSeverityPipe]
})
export class PageAuditReviewComponent implements OnInit, AfterViewInit, OnDestroy {
  errors:string[] = []

  faCaretRight = faCaretRight
  faAngleDown = faAngleDown
  faAngleUp = faAngleUp
  faSmileBeam = faSmileBeam
  faMeh = faMeh
  faFrown = faFrown
  faTrashAlt = faTrashAlt
  faCircle = faCircle
  faFilePdf = faFilePdf
  faFileExcel = faFileExcel
  faUser = faUser
  faPalette = faPalette
  faUniveralAccess = faUniversalAccess
  faCompass = faCompass
  faImage = faImage
  faSitemap = faSitemap
  faSearch = faSearch
  faSpinner = faSpinner
  faDownload = faDownload
  faPlus = faPlus
  faInfoCircle = faInfoCircle
  faTriangleExclamation = faTriangleExclamation
  faCircleRadiation = faCircleRadiation
  faCircleCheck = faCircleCheck

  isLoading = false
  domain_error = false
  error = ''
  is_audit_started = false

  subcategory = ''
  isSticky = true
  panelOpenState = false
  url = '';
  source = ''

  category_score = 98
  score_color = ""

  // issue and element lists
  issues: UXIssueMessage[] = []
  elements: SimpleElement[] = []
  element_issues: UXIssueMessage[] = []

  // array of audits
  //audits = this.getPageAudits();
  host = ''
  audits: Audit[] = []
  page: SimplePage = {} as SimplePage
  issue_element_map: ObservationElementMapTwoWayBinding = {} as ObservationElementMapTwoWayBinding
  issue_map = new Map()
  audit_record_id = 0
  audit_stat: AuditStats = {} as AuditStats
  audit_record: AuditRecord = {} as AuditRecord
  page_audits: PageAudits = {} as PageAudits
  observation_elements: ObservationElementMapTwoWayBinding = {} as ObservationElementMapTwoWayBinding
  simple_pages: SimplePage[] = []

  content_audit_score = 0
  info_architecture_audit_score = 0
  aesthetics_audit_score = 0
  interactivity_audit_score = 0
  category_scores: AuditScore = {} as AuditScore

  selected_category = "OVERALL"
  issue_selected = ""

  //intervals
  page_interval_id: any | undefined
  audit_stat_interval_id: any | undefined
  element_interval_id: any | undefined
  error_interval_id: any | undefined
  isReportDownloadDialogDisplayed: boolean
  showLoginRequiredMessage: boolean

  //tooltips
  email_privacy_description = "We won't share your email with anyone or send you any extra emails beyond providing you the UX audit results."

  //dropdown variables
  priorities: Priority[] = []

  color: ThemePalette = 'primary'
  accent_color: ThemePalette = 'accent'

  mode: ProgressSpinnerMode = 'indeterminate'
  determinate_mode: ProgressSpinnerMode = 'determinate'

  value = 50
  //contentAuditProgress = this.audit_stat.contentAuditProgress || 0
  //aestheticAuditProgress = this.audit_stat.aestheticAuditProgress || 0
  //infoArchAuditProgress = this.audit_stat.infoArchitectureAuditProgress || 0
  //dataExtractionProgress = this.audit_stat.dataExtractionProgress || 0
  //overallAuditProgress = (this.contentAuditProgress + this.aestheticAuditProgress + this.infoArchAuditProgress + this.dataExtractionProgress)/4

  radius = 80
  diameter = 50
  overall_score_diameter = 45
  circumference = 2 * Math.PI * this.radius
  dashoffset = 0;

  /* REPORT REQUEST FORM PROPERTIES */
  auditReportForm = new FormGroup({
    email : new FormControl('', [
      Validators.required,
      Validators.email
    ])
  })

  email_submitted = false
  /* END REPORT REQUEST FORM PROPERTIES */

  //permissions
  is_audit_admin = false

  selected_element = ""

  // New UI state
  activeTab: string = 'visual'
  resultsReady: boolean = false
  visualIssues: UXIssueMessage[] = []
  nonVisualIssues: UXIssueMessage[] = []
  nonVisualIssueGroups: {type: string, label: string, issues: UXIssueMessage[]}[] = []
  aiSuggestions: Record<string, string> = {}
  aiLoadingKeys: Record<string, boolean> = {}

  //screenshot image element
  @ViewChild("full_page_screenshot")
  screenshot_img!: ElementRef

  @ViewChild("element_map")
  element_img_map!: ElementRef

  @ViewChild("issues_container")
  issues_container!: ElementRef

  //screenshot image element
  @ViewChild("page_frame")
  page_frame!: ElementRef

  @ViewChild("image_screenshot")
  image_screenshot!: ElementRef

  router = inject(Router);
  route = inject(ActivatedRoute);
  auth = inject(AuthService);
  audit_service = inject(AuditService);
  segmentio = inject(SegmentIOService);
  location = inject(Location);
  dialog = inject(MatDialog);
  renderer = inject(Renderer2);
  auth_service = inject(AuthorizationService);
  report_service = inject(ReportService);
  categoryPipe = inject(FilterByCategoryPipe);
  severityPipe = inject(FilterSeverityPipe);

  constructor() {
    this.priorities = [
      { name: 'High' },
      { name: 'Medium' },
      { name: 'Low' }
    ];
    this.isReportDownloadDialogDisplayed = false;
    this.showLoginRequiredMessage = false;
  }
  
  ngAfterViewInit(){
    this.issues = this.issue_element_map.issues || []
    this.category_scores = this.issue_element_map.scores || []

    if (this.issues.length) {
      this.splitIssues()
      this.resultsReady = true
    }

    this.selectCategory('OVERALL')
  }
 
  ngOnInit(): void {
    /* NOTE: Attempting removal. IF STILL HERE AFTER 2/1/2022 THEN DELETE
    if(this.auth.isAuthenticated$){
      this.auth.getAccessTokenSilently()
          .subscribe( (token) => {
            this.is_audit_admin = this.auth_service.isAuthorized( 'Audit Administrator', token)
          },
          (error) => {
            this.addError(error)
          })
    }
*/
    this.audit_record_id = this.route.snapshot.params['id'];

    //this.audit_record_id = parseInt(this.route.snapshot.paramMap.get("audit_record_id") || "0")
    //this.audit_record_id = parseInt(this.route.snapshot.queryParamMap.get("audit_record_id") || "0")
    this.is_audit_started = JSON.parse(sessionStorage.getItem("is_audit_started") || "0")
    
    if(!this.audit_record_id || this.audit_record_id <= 0){
      this.isLoading = true
      this.audit_record_id = parseInt(sessionStorage.getItem("audit_record_id") || "0")
      this.page = {
        url: sessionStorage.getItem("url") || ""
      } as SimplePage

      if(this.audit_record_id){
        window.analytics.track('View Quick-Audit Page');

        this.startIntervals()
      }
      else{
        console.log("Error. No audit record id found")
        window.analytics.track('Viewed UX Audit Form');
        this.is_audit_started = false
      }
    }
    else{
      
      sessionStorage.setItem("audit_record_id", `${this.audit_record_id}`)
      this.getAuditStats(this.audit_record_id)
      

      this.audit_service.getPage(this.audit_record_id)
                        .subscribe(pages =>  { 
                          if(pages){
                            this.page = pages[0];
                            sessionStorage.setItem("page", JSON.stringify(this.page))
                            sessionStorage.setItem("page_key", this.page.key)
                            this.getObservationElements(this.audit_record_id)
                          }
                        },
                        (error) => {
                          this.addError(JSON.stringify(error))
                        });
    }

  }

  ngOnDestroy() {
    if (this.page_interval_id) {
      clearInterval(this.page_interval_id);
    }
    if(this.audit_stat_interval_id){
      clearInterval(this.audit_stat_interval_id)
    }
  }

  /**
   * Retrieve {@link AuditStats} record from API
   */
  getAuditStats(audit_record_id: number) {
    this.audit_service.getAuditStats(audit_record_id)
                        .subscribe(audit_stat =>  {
                          this.audit_stat = audit_stat;
                          //this.contentAuditProgress = this.audit_stat.contentAuditProgress * 100
                          //this.aestheticAuditProgress = this.audit_stat.aestheticAuditProgress * 100
                          //this.infoArchAuditProgress = audit_stat.infoArchitectureAuditProgress * 100
                          //this.overallAuditProgress = (this.contentAuditProgress + this.aestheticAuditProgress + this.infoArchAuditProgress)/3;
                        },
                        () => {
                          this.addError("Something went wrong while retrieving audit data")
                        });
  }

  showReportDownloadDialog() {
    this.isReportDownloadDialogDisplayed = true
    this.segmentio.trackExportReportAuthenticatedClick(sessionStorage.getItem("url") || '', sessionStorage.getItem("page_key") || '' )

    this.report_service.getExcelReport(sessionStorage.getItem("page_key") || '')
                          .subscribe(result => {
                            //const blob = new Blob([result], {type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'});

                            const downloadURL = window.URL.createObjectURL(result);
                            const link = document.createElement('a');
                            link.href = downloadURL;
                            link.download = "audit_results.xlsx";
                            link.click();
                            console.log('The report dialog was closed');
                          },
                          () => {
                            this.addError("There was an error while generating the excel report")
                          });

    this.report_service.getPDFReport(sessionStorage.getItem("page_key") || '')
                        .subscribe(result => {
                          // const blob = new Blob([result], {type: 'application/pdf'});
                          const downloadURL = window.URL.createObjectURL(result);
                          const link = document.createElement('a');
                          link.href = downloadURL;
                          link.download = "audit_report.pdf";
                          link.click();
                          console.log('The pdf report dialog was closed');
                        },
                        () => {
                          this.addError("There was an error while generating the pdf report")
                        });
  } 

  showLoginRequiredDialog() {
    console.log("show login required message")
    this.showLoginRequiredMessage = true
    this.segmentio.trackExportReportNonAuthenticatedClick(sessionStorage.getItem("url") || '', sessionStorage.getItem("page_key") || '')
  }

  formatPageUrlAsLink(url: string) {
    return "http://"+url
  }

  startIntervals() {
    //get audit stats for audit record
    this.audit_stat_interval_id =  setInterval(()=>{  
      this.audit_service.getAuditStats(this.audit_record_id)
        .subscribe((audit_stat) =>  {
          this.audit_stat = audit_stat;
          //this.contentAuditProgress = this.audit_stat.contentAuditProgress * 100
          //this.aestheticAuditProgress = this.audit_stat.aestheticAuditProgress * 100
          //this.infoArchAuditProgress = audit_stat.infoArchitectureAuditProgress * 100
          //this.dataExtractionProgress = this.audit_stat.dataExtractionProgress * 100
          //this.overallAuditProgress = (this.contentAuditProgress + this.aestheticAuditProgress + this.infoArchAuditProgress + this.dataExtractionProgress)/4;

          /*
          if(audit_stat.aestheticAuditProgress >= 1 && audit_stat.contentAuditProgress >= 1 && audit_stat.infoArchitectureAuditProgress >= 1 && audit_stat.dataExtractionProgress >= 1){
            this.isLoading = false
            this.is_audit_started = false
            this.startPageRequestInterval(this.audit_record_id)
            this.isAuditStarted({ audit_record_id: this.audit_record_id, is_audit_started: false, url: this.url})
            clearInterval(this.audit_stat_interval_id)
          }
          */
        },
        () => {
          this.addError("Something went wrong while requesting audit data")
        });
    }, 10000)
  }

  /**
   * Returns a "is considered delightful", "is almost there", or "needs improvement"
   *    based on the provided score
   * @param score 
   * @returns 
   */
  getExperienceScoreNeeds(score: number){
    console.log("score :: "+score)
     //exceptional
     if(score >= 95.0){
      return "is considered exceptional"
    }
    //delightful
    else if(score >= 80.0){
      return "is considered delightful"
    }
    //almost there
    else if(score >= 60.0){
      return "is almost there"
    }
    //needs improvmenet
    else {
      return "needs improvement"
    }
  }

  getNumberOfNonDelightfulCategoryScores() {
    let category_count = 0;
    if(this.category_scores.contentScore < 80.0){
      category_count+=1
    }
    if(this.category_scores.informationArchitectureScore < 80.0){
      category_count+=1
    }
    console.log("aesthetic audits score : " +this.aesthetics_audit_score)
    if(this.category_scores.aestheticsScore < 80.0){
      category_count+=1
    }

    let return_str = ""
    if(category_count > 1){
      return_str = "are "+category_count + " categories"
    }
    else if(category_count == 1){
      return_str = "is "+category_count + " category"
    }
    else {
      return_str = "aren't any categories"
    }
    return return_str
  }

  startPageRequestInterval(audit_record_id: number){
    //get page for audit record
    this.audit_service.getPage(audit_record_id)
        .subscribe((pages) =>  {
          if(pages){
            this.page = pages[0];
            sessionStorage.setItem("page", JSON.stringify(this.page))
            sessionStorage.setItem("page_key", this.page.key)
            this.getObservationElements(this.audit_record_id)
          }
        },
        () => {
          this.addError("Error retrieving page data")
        });
  }

  startErrorTimeout(){
    //get page for audit record
    setTimeout(()=>{ 
      this.error = ""
    }, 30000)
  }

  
  highlightIssueNumber(issue_key: string){
    console.log("issue key ::  "+ issue_key)
    //var myElement = angular.element( '#'+issue_key  );
  }

  collectUxIssues(audits: Audit[]): UXIssueMessage[] {
    const ux_issues = [] as UXIssueMessage[]
    for (const audit of audits) {
      if(audit.messages){
        for (const issue of audit.messages) {
          ux_issues.push(issue)
        }
      }
    }
    return ux_issues
  }

  elementHasIssues(element_key: string): boolean {
    
    const element_issues = this.getElementIssues(element_key)
    let issue_count = 0
    for(let i = 0; i< element_issues?.length; i++){
      if(element_issues[i].priority != 'NONE'){
        issue_count+=1
      }
    }

    return issue_count > 0
  }
  
  getScoreColorClass(score:number): string {
    if(score >= 80){
      return "delightful"
    }
    else if(score >= 60 && score < 80){
      return "meh"
    }
    else if(score < 60){
      return "sadface"
    }
    return ""
  }

  getScoreColor(score:number): string {
    if(score >= 80){
      return "#23D8A4"
    }
    else if(score >= 60 && score < 80){
      return "#F9BF07"
    }
    else if(score < 60){
      return "#FF0050"
    }
    return ""
  }

  /* audit category panel */
  selectCategory(category: string) {
    this.selected_category = category.toUpperCase()

    if(this.selected_category === 'CONTENT'){
      this.category_score = this.category_scores.contentScore
    }
    else if(this.selected_category === 'INFORMATION_ARCHITECTURE'){
      this.category_score = this.category_scores.informationArchitectureScore
    }
    else if(this.selected_category === 'AESTHETICS'){
      this.category_score = this.category_scores.aestheticsScore
    }
    else if(this.selected_category === 'INTERACTIVITY'){
      this.category_score = this.category_scores.interactivityScore
    }
    else if(this.selected_category === 'OVERALL'){
      this.category_score = this.category_scores.overallScore
    }

    this.score_color = this.getScoreColor(this.category_score)
    const progress = this.category_score / 100
    this.dashoffset = this.circumference * (1 - progress)
  }

  //navigate back to audit category page
  goBack(): void {
    this.location.back();
  }

  setPriority(issue: UXIssueMessage, priority: Priority) {
    issue.priority = priority.name
  }

  titleCase(value: string): string {
    
    value = value.replace(
      /([-_][a-zA-Z])/g,
      (group) => group.toLowerCase()
                      .replace('-', ' ')
                      .replace('_', ' '))

    return value.replace(
      /\w\S*/g,
      function(txt) {
        return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();
      }
    );
  }

  convertToHex(color:string) {
    color = color.replace("rgb(","");
    color = color.replace(")", "");
    const rgb_arr = color.split(",");
    const b = rgb_arr.map(function(x){             //For each array element
      x = parseInt(x).toString(16);      //Convert to a base16 string
      return (x.length==1) ? "0"+x : x;  //Add zero if we get only one character
    })
  
    return '#'+b.join("");
  }

  openElementInfoDialog(element: SimpleElement): void {
    const dialogRef = this.dialog.open(ElementInfoDialog, {
      width: '800px',
      height: '800px',
      data: {
        element: element
      }
    });

    dialogRef.afterClosed().subscribe(() => {
      console.log('The page recommendation dialog was closed');
    },
    (error) => {
      this.addError(error)
    });
  }

  startSinglePageAudit(): void {
    let dialogRef = undefined
    if(this.auth.isAuthenticated$){
      dialogRef = this.dialog.open(StartSinglePageAuditConfirmation, {
        width: '600px',
        height: '500px',
        data: {
          url: this.page.url
        }
      });
    }
    else{
      dialogRef = this.dialog.open(StartSinglePageAuditLoginRequired, {
        width: '600px',
        height: '500px',
        data: {
          url: this.page.url
        }
      });
    }

    const subscribeDialog = dialogRef.componentInstance.clear_audit_event.subscribe((data: boolean) => {
        console.log('dialog data', data);
        this.is_audit_started = false
        this.clearAuditStatus()
    },
    (error) => {
      this.addError(error)
    });

    dialogRef.afterClosed().subscribe(() => {
      subscribeDialog.unsubscribe()
      console.log('The start audit dialog was closed');
    },
    (error) => {
      this.addError(error)
    });
  }

  isValidURL(domain_url: string): boolean {
    const urlregex = /^((https?|ftp):\/\/)?([a-zA-Z0-9.-]+(:[a-zA-Z0-9.&%$-]+)*@)*((25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9][0-9]?)(\.(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])){3}|([a-zA-Z0-9-]+\.)*[a-zA-Z0-9-]+\.(com|app|edu|gov|int|mil|net|org|biz|arpa|info|name|pro|aero|coop|museum|website|[a-zA-Z]{2}))(:[0-9]+)*(\/($|[a-zA-Z0-9.,?'\\+&%$#=~_-]+))*$/;
    return urlregex.test(domain_url);
  }

  getElementScreenshot(issue: UXIssueMessage ){
    const element_key = this.issue_element_map.issueElementMap[issue.key]

    console.log("looking up element ... "+element_key)
    //find element with key
    
    for (const element of this.elements) {
      if(element.key === element_key){
        console.log("element found ... " + element.key)
        return element.screenshotUrl ? element.screenshotUrl : '../assets/placeholder-300x202.jpg'
      }
    }
    //var element = this.elements.find((element) => { element.key == element_key} )
    return '../assets/placeholder-300x202.jpg'
  }

  getObservationElements(audit_record_id: number): void {
    
    this.audit_service.getAuditElements(audit_record_id)
        .pipe(
          catchError(err => {
            this.error = "Unable to get UX issues. Please try again in a few minutes";

            this.startErrorTimeout()
           // this.isLoading = false
            return throwError(err);
          })
        )
        .subscribe(issue_element_map =>  { 
          this.issue_element_map = issue_element_map || {} as ObservationElementMapTwoWayBinding; 
          sessionStorage.setItem('issue_element_map', JSON.stringify(this.issue_element_map))
          this.elements = this.issue_element_map.elements || []
          this.issues = this.issue_element_map.issues || []
                    
          this.category_scores = this.issue_element_map.scores || []
          this.category_scores.overallScore = (this.issue_element_map.scores.aestheticsScore + this.issue_element_map.scores.contentScore + this.issue_element_map.scores.informationArchitectureScore)/3
          this.splitIssues()
          this.resultsReady = true
          this.isLoading = false
          this.is_audit_started = false
        },
        (error) => {
          this.addError(error)
        });
  }

  getCategoryDescription(category: string): string {
    let description = ""
    if(category.toLowerCase() === 'content'){
      description = "The readability, grammatical errors, organization of the text, quality and relevance of images, consistency of icons and visual hierarchy"
    }
    else if(category.toLowerCase() === 'information_architecture'){
      description = "The effectiveness and functionality of the structure and different interactive elements on your website such as menu, links and buttons, as well as the overall structure of the website."
    }
    else if(category.toLowerCase() === 'aesthetics'){
      description = "The color palette, contrast of text and non-text items. The position and placement of your logo. The consistency of type used across your website."
    }
    else if(category.toLowerCase() === 'interactivity'){
      description = "The complexity of user journeys and forms"
    }
    else if(category.toLowerCase() === 'overall'){
      description = "Cumulative score of all UX audits performed"
    }
    
    return description
  }

  /** Sets the issue selected and scrolls to the element */
  openIssueDetails(key: string) {
    if(this.issue_selected===key){
      this.issue_selected = '';
    }
    else{
      this.issue_selected = key;
    }

    this.selected_element = '';
    const element_key = this.issue_element_map.issueElementMap[key]
    //scroll to element
    document.getElementById(element_key)?.scrollIntoView(true)
    //set timeout to remove class from element after 10 seconds

    //get element for issue with key
    this.selected_element = element_key  
  }

  calculatePageAuditScore(page_audits: Audit[], audit_type: string): number {
    let score = 0
    let total = 0

    for (const audit of page_audits) {
      if(audit.category === audit_type){
        if(audit.totalPossiblePoints === 0){
          score += 1
        }
        else{
          score += (audit.points/audit.totalPossiblePoints)
        }
        total += 1
      }
    }

    score = (score / total) * 100;
    return score | 0;
  }
  
  getAuditCategory(labels: string[]): void {
    for (const label of labels) {
      if(label === "") {
        // Handle empty label if needed
      }
    }
  }

  getElementIssues(element_key: string): UXIssueMessage[] {
    const issue_keys = this.issue_element_map.elementIssues[element_key] || [] as string[]

    const ux_issues = [] as UXIssueMessage[]
    //console.log("issue key length :: "+issue_keys)
    for (const issue_key of issue_keys) {
      for (const issue of this.issue_element_map.issues) {
        if(issue.key === issue_key){
          ux_issues.push(issue)
          //console.log("adding ux issue to element issues list")
          break;
        }
      }
    }
    this.element_issues = ux_issues
    return ux_issues
  }

  getElementLeftCoordinate(element: SimpleElement) {
    const scale_width = this.image_screenshot.nativeElement.width/this.page.width
    const x_offset = element.xlocation * scale_width
    const outline_style = Math.ceil(x_offset)+"px"

    return outline_style
  }

  getElementTopCoordinate(element: SimpleElement) {
    const scale_width = this.image_screenshot.nativeElement.width/this.page.width
    const y_offset = element.ylocation * scale_width
    const outline_style = Math.floor(y_offset)+"px"

    return outline_style
  }
  
  getElementScaledWidth(element: SimpleElement) {
    const scale_width = this.image_screenshot.nativeElement.width/this.page.width
    const width = element.width * scale_width
    const outline_style = Math.ceil(width)+"px"

    return outline_style
  }

  getElementScaledHeight(element: SimpleElement) {
    const scale_width = this.image_screenshot.nativeElement.width/this.page.width
    const height = element.height * scale_width
    const outline_style = Math.floor(height)+"px"

    return outline_style
  }

  selectIssue(issue_key: string): void {
    console.log("selecting issue ... "+issue_key)

    //close any open issues
    this.issue_selected = '';

    //scroll panel to top of panel
    document.getElementById(issue_key)?.scrollIntoView({ behavior: 'smooth', block: "start" });

    //open issue panel with matching issue_key
    this.issue_selected = issue_key
  }

  /**
   * 
   * @param style 
   * @param issues 
   * @param issue_map 
   */
  generateElementsHtml(page: SimplePage, ux_issues: UXIssueMessage[], issue_map: Map<string, number>, element: SimpleElement): string {
    let html = ""
    let ylocation = "0px"
    let xlocation = "0px"
    if(element){
      const scale_height = this.image_screenshot.nativeElement.height/page.height
      const scale_width = this.image_screenshot.nativeElement.width/page.width
      xlocation = (Math.floor(element.xlocation * scale_width)-20) +"px"
      ylocation = (Math.floor(element.ylocation * scale_height)-30) +"px"
    }
    else {
      xlocation = "0px"
      ylocation = "0px"
    }

    for (const issue of ux_issues) {
      if(issue.priority.toLowerCase() === 'none'){
        continue
      }

      const absolute_container: HTMLElement = this.renderer.createElement('div')
      absolute_container.setAttribute("style", "position:absolute;width: 36px; height:36px;top:"+ ylocation +";left:"+ xlocation)
      absolute_container.setAttribute("id", issue.key)
      this.renderer.appendChild(this.screenshot_img.nativeElement, absolute_container)
      
      const badge: HTMLElement = this.renderer.createElement('img')
      const img_url = this.getIconSrc(issue.priority)        

      badge.setAttribute('src', img_url)
      badge.setAttribute('style', 'width:100%;height:100%')
      badge.setAttribute('aria-label', issue.description)
      badge.setAttribute('data-balloon-pos', "up")
      absolute_container.appendChild(badge)

      const badge_label: HTMLElement = this.renderer.createElement('span')
      let top = ''
      let left = ''
      
      const issue_number = issue_map.get(issue.key) || 0
      if( 10 <= issue_number ){
        left = "left:25%;"
        if(issue.priority === 'MEDIUM'){
          top = 'top:18%;'
        }
        else{
          top = "top:10%;"
        }        
      }
      else{
        left = "left:35%;"
        if(issue.priority === 'MEDIUM'){
          top = 'top:15%;'
        }
        else{
          top = "top:10%;"
        }        
      }
      badge_label.setAttribute("style", "position:absolute;" + left + top + "color:black;font-size:16px;font-weight:400; margin-top:0px")
      badge_label.innerHTML = ""+issue_map.get(issue.key)
      absolute_container.appendChild(badge_label)

      html += absolute_container.outerHTML
    }
    return html
  }

  generateIssueIcons(page: SimplePage, ux_issues: UXIssueMessage[], issue_map: Map<string, number>, element: SimpleElement): string {    
    let html = ''
    html += this.generateElementsHtml(page, ux_issues, issue_map, element)
    return html
  }

  
  getElementString(element: SimpleElement) {
    const margin_right = this.screenshot_img.nativeElement.offsetLeft
    const margin_top = this.screenshot_img.nativeElement.offsetTop

    let scale_height = this.screenshot_img.nativeElement.parentElement.height/this.page.height
    const scale_width = this.screenshot_img.nativeElement.parentElement.width/this.page.width
    
    if(scale_height === 0){
      scale_height = scale_width
    }

    const element_width = element.width * scale_width
    const element_height = element.height * scale_height

    const x_offset = element.xlocation * scale_width + margin_right
    const y_offset = element.ylocation * scale_height + margin_top
    console.log("y offset : "+y_offset+"px")
    console.log("x offset : "+x_offset+"px")
    console.log("width : "+element_width+"px")
    console.log("height : "+element_height+"px")

    const outline_style = "position:absolute, top: "+ y_offset +"px"

    return outline_style
  }

  showElementIssues(element_key: string) {
    console.log("you clicked element :: "+element_key)
  }

  requestReport(): void {
    console.log("Audit report form :: " + JSON.stringify(this.auditReportForm.value))
    this.audit_record_id = parseInt(sessionStorage.getItem('audit_record_id') || "-1")
    this.audit_service
        .requestReport(this.auditReportForm, this.audit_record_id)
        .subscribe(() => {
          this.email_submitted = true
        },
        (error) => {
          this.addError(error)
        })
  }

  getLabel(key: string) {
    return this.issue_map.get(key)
  }

  getScore(audit:Audit){
    return (audit.points/audit.totalPossiblePoints || 0 )*100;
  }

  isColorPalette(issue:ColorPaletteObservation){
    return issue.type === "COLOR_PALETTE"
  }

  isColorContrast(issue:UXIssueMessage){
    return issue.type === "COLOR_CONTRAST"
  }

  isElementObservation(issue: UXIssueMessage): boolean {
    return issue.type === 'element_observation';
  }

  isWrittenContent(audit:Audit){
    return audit.category === "WRITTEN_CONTENT"
  }

  isTypography(audit:Audit){
    return audit.category === "TYPOGRAPHY"
  }

  isVisuals(audit:Audit){
    return audit.category === "VISUALS"
  }

  isBranding(audit:Audit){
    return audit.category === "VISUALS"
  }

  deleteRecommendation(key: string, recommendation: string): void {
    const delete_confirmed = confirm("Are you sure you want to delete this recommendation. This action is permanent and cannot be undone");

    if(delete_confirmed){
      console.log("recommendation to add :: "+recommendation)
      this.audit_service.deleteRecommendation(key, recommendation)
                    .pipe(
                      catchError(err => {
                        this.error = "I'm sorry I can't do that. Please try again in a few minutes";

                        this.startErrorTimeout()
                        this.isLoading = false
                        return throwError(err);
                      }))
                      .subscribe(data => {
                        //remove recommendation from front end
                        this.getObservationElements(this.audit_record_id)

                        this.segmentio.sendRecommendationDeletedMessage(key, recommendation)
                        console.log("Data returned :: " + JSON.stringify(data));
                      },
                      (error) => {
                        this.addError(error)
                      })
    }
  }

  isMultipleOfEight(value: string): boolean {
    return parseInt(value.replace('px', '')) % 8 == 0;
  }

  //TRACKING METHODS
  getBackgroundColor(priority:string): string {
    if(priority.toLowerCase() === 'high'){
      return "red"
    }
    else if(priority.toLowerCase() === 'medium'){
      return "orange"
    }
    else {
      return "blue"
    }
  }

  getIconSrc(priority:string): string {
    if(priority.toLowerCase() === 'high'){
      return "https://storage.googleapis.com/look-see-inc-assets/icons/High-Alert-128px.png"
    }
    else if(priority.toLowerCase() === 'medium'){
      return "https://storage.googleapis.com/look-see-inc-assets/icons/Medium-Alert-128px.png"
    }
    else if(priority.toLowerCase() === 'low'){
      return "https://storage.googleapis.com/look-see-inc-assets/icons/Low-Alert-128px.png"
    }
    else {
      return "https://storage.googleapis.com/look-see-inc-assets/icons/circle_green_checkmark.png"
    }
  }

  getRiskTooltip(priority:string): string{
    if(priority.toLowerCase() === 'high'){
      return "High risk has a large impact"
    }
    else if(priority.toLowerCase() === 'medium'){
      return "Mid level risk has a moderate impact"
    }
    else {
      return "Low risk has a minimal impact"
    }
  }

  printObject(val: unknown): string {
    return JSON.stringify(val);
  }


  openReport(): void {
    this.isReportDownloadDialogDisplayed = false
  }

  isAuditStarted(audit_started: {audit_record_id: number, is_audit_started: boolean, url: string}) {
    console.log("triggered check for if audit is started")
    this.audit_record_id = audit_started.audit_record_id
    this.is_audit_started = audit_started.is_audit_started
    this.isLoading = this.is_audit_started
    this.audit_stat = {} as AuditStats
    sessionStorage.setItem("is_audit_started", this.is_audit_started.toString())
    sessionStorage.setItem("audit_record_id", this.audit_record_id.toString())
    if(this.is_audit_started){
      this.page.url = audit_started.url
    }

    if(this.is_audit_started){
      this.startIntervals()
    }
  }

  clearAuditStatus() {
    this.is_audit_started = false
    this.audit_record_id = 0
    this.isLoading = false
    this.email_submitted = false

    sessionStorage.removeItem("is_audit_started")
    sessionStorage.removeItem("audit_record_id")
    sessionStorage.removeItem("url")
  }


  /* ERROR HANDLING METHODS */
  addError(error: string): void {
    //check if error is already present
    if(this.errors.indexOf(error) < 0){
      this.errors.push(error)
      
      setTimeout(()=>{ 
        this.errors.shift()
      }, 15000)
    }
  }

  getExampleScreenshotUrl(issue: UXIssueMessage ) : string{
    if(issue?.goodExample && issue.goodExample.screenshotUrl.length > 0){
      return issue.goodExample.screenshotUrl;
    }

    return "assets/placeholder-300x202.jpg"
  }

  /* ===== NEW METHODS FOR REDESIGNED UI ===== */

  /**
   * Split issues into visual (tied to rendered elements with coordinates)
   * and non-visual (metadata, SEO, structural issues without element placement).
   */
  splitIssues(): void {
    this.visualIssues = []
    this.nonVisualIssues = []

    for (const issue of this.issues) {
      if (issue.priority === 'NONE') continue;

      const elementKey = this.issue_element_map.issueElementMap
        ? this.issue_element_map.issueElementMap[issue.key]
        : null;

      let isVisual = false;
      if (elementKey && this.elements) {
        const element = this.elements.find(el => el.key === elementKey);
        if (element && (element.xlocation > 0 || element.ylocation > 0)) {
          isVisual = true;
        }
      }

      if (isVisual) {
        this.visualIssues.push(issue);
      } else {
        this.nonVisualIssues.push(issue);
      }
    }

    this.groupNonVisualIssues();
  }

  /**
   * Group non-visual issues by their category field.
   */
  groupNonVisualIssues(): void {
    const groupMap: Record<string, UXIssueMessage[]> = {};

    for (const issue of this.nonVisualIssues) {
      const cat = issue.category || 'OTHER';
      if (!groupMap[cat]) {
        groupMap[cat] = [];
      }
      groupMap[cat].push(issue);
    }

    this.nonVisualIssueGroups = Object.keys(groupMap)
      .filter(key => groupMap[key].length > 0)
      .map(key => ({
        type: key,
        label: this.titleCase(key),
        issues: groupMap[key]
      }));
  }

  /**
   * Filter issues by the selected category. Returns all if OVERALL is selected.
   */
  getFilteredIssues(issueList: UXIssueMessage[]): UXIssueMessage[] {
    if (!issueList) return [];

    let filtered = issueList.filter(i => i.priority !== 'NONE');

    if (this.selected_category !== 'OVERALL') {
      filtered = this.categoryPipe.transform(filtered, this.selected_category);
    }

    return filtered;
  }

  /**
   * Select an element on the screenshot and open its first issue in the panel.
   */
  selectElementIssues(elementKey: string): void {
    this.selected_element = elementKey;
    this.activeTab = 'visual';

    const elementIssues = this.getElementIssues(elementKey);
    if (elementIssues.length > 0) {
      const firstVisibleIssue = elementIssues.find(i => i.priority !== 'NONE');
      if (firstVisibleIssue) {
        this.issue_selected = firstVisibleIssue.key;
        // Scroll to the issue in the panel
        setTimeout(() => {
          document.getElementById(firstVisibleIssue.key)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }, 100);
      }
    }
  }

  /**
   * Count visible issues for an element, respecting category filter.
   */
  getVisibleElementIssueCount(elementKey: string): number {
    const elementIssues = this.getElementIssues(elementKey);
    return this.getFilteredIssues(elementIssues).length;
  }

  /**
   * Get the highest priority among issues for an element.
   */
  getHighestPriority(elementKey: string): string {
    const elementIssues = this.getElementIssues(elementKey);
    const priorities = elementIssues
      .filter(i => i.priority !== 'NONE')
      .map(i => i.priority);

    if (priorities.includes('HIGH')) return 'HIGH';
    if (priorities.includes('MEDIUM')) return 'MEDIUM';
    if (priorities.includes('LOW')) return 'LOW';
    return 'LOW';
  }

  /**
   * Stubbed AI assist — generates a fix suggestion after a simulated delay.
   */
  requestAIAssist(issue: UXIssueMessage): void {
    if (this.aiSuggestions[issue.key] || this.aiLoadingKeys[issue.key]) return;

    this.aiLoadingKeys[issue.key] = true;

    setTimeout(() => {
      this.aiLoadingKeys[issue.key] = false;

      if (issue.type === 'COLOR_CONTRAST') {
        const contrast = issue.contrast ? issue.contrast.toFixed(2) : 'unknown';
        const fg = issue.foregroundColor ? this.convertToHex(issue.foregroundColor) : 'the text color';
        const bg = issue.backgroundColor ? this.convertToHex(issue.backgroundColor) : 'the background color';
        this.aiSuggestions[issue.key] =
          `The current contrast ratio is ${contrast}:1, which does not meet WCAG AA requirements (minimum 4.5:1 for normal text, 3:1 for large text).\n\n` +
          `Suggested fix: Update ${fg} (foreground) or ${bg} (background) to achieve at least a 4.5:1 contrast ratio.\n\n` +
          `For example, try darkening the text color or lightening the background to increase separation. You can verify your updated colors at a contrast checker tool.`;
      } else {
        const wcag = issue.wcagCompliance || 'general accessibility best practices';
        this.aiSuggestions[issue.key] =
          `To fix "${issue.title}":\n\n` +
          `${issue.recommendation}\n\n` +
          `This addresses WCAG compliance: ${wcag}.\n\n` +
          `Additional context: ${issue.description || 'Review the element and apply the recommended changes to improve accessibility.'}`;
      }
    }, 2000);
  }

  /**
   * Export PDF report — downloads the audit as a PDF file.
   */
  exportPDFReport(): void {
    this.isReportDownloadDialogDisplayed = true;
    const pageKey = sessionStorage.getItem("page_key") || '';
    const url = sessionStorage.getItem("url") || '';

    this.segmentio.trackExportReportAuthenticatedClick(url, pageKey);

    this.report_service.getPDFReport(pageKey).subscribe(
      result => {
        const downloadURL = window.URL.createObjectURL(result);
        const link = document.createElement('a');
        link.href = downloadURL;
        link.download = "accessibility_audit_report.pdf";
        link.click();
        this.isReportDownloadDialogDisplayed = false;
      },
      () => {
        this.addError("There was an error generating the PDF report. Please try again.");
        this.isReportDownloadDialogDisplayed = false;
      }
    );
  }

  /**
   * Return an SVG path for a non-visual issue group icon based on category.
   */
  getNonVisualGroupIcon(type: string): string {
    switch (type.toUpperCase()) {
      case 'CONTENT':
      case 'WRITTEN_CONTENT':
        return 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z';
      case 'INFORMATION_ARCHITECTURE':
      case 'SEO':
      case 'METADATA':
        return 'M4 5a1 1 0 011-1h14a1 1 0 011 1v2a1 1 0 01-1 1H5a1 1 0 01-1-1V5zM4 13a1 1 0 011-1h6a1 1 0 011 1v6a1 1 0 01-1 1H5a1 1 0 01-1-1v-6zM16 13a1 1 0 011-1h2a1 1 0 011 1v6a1 1 0 01-1 1h-2a1 1 0 01-1-1v-6z';
      case 'AESTHETICS':
      case 'COLOR_MANAGEMENT':
      case 'TYPOGRAPHY':
        return 'M7 21a4 4 0 01-4-4V5a2 2 0 012-2h4a2 2 0 012 2v12a4 4 0 01-4 4zm0 0h12a2 2 0 002-2v-4a2 2 0 00-2-2h-2.343M11 7.343l1.657-1.657a2 2 0 012.828 0l2.829 2.829a2 2 0 010 2.828l-8.486 8.485M7 17h.01';
      default:
        return 'M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z';
    }
  }

  /**
   * Called from the onboarding component when user clicks "View Results".
   */
  showResults(): void {
    this.resultsReady = true;
  }
}