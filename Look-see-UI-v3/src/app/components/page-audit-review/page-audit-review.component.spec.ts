import { CommonModule, Location } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { RouterTestingModule } from '@angular/router/testing';
import { AuthService } from '@auth0/auth0-angular';
import { of } from 'rxjs';
import { FilterByCategoryPipe } from '../../pipes/category_filter.pipe';
import { FilterSeverityPipe } from '../../pipes/severity_filter.pipe';
import { AuditService } from '../../services/audit.service';
import { AuthorizationService } from '../../services/authorization.service';
import { ReportService } from '../../services/report.service';
import { SegmentIOService } from '../../services/segmentio.service';
import { PageAuditReviewComponent } from './page-audit-review.component';

describe('AuditRecordsComponent', () => {
  let component: PageAuditReviewComponent;
  let fixture: ComponentFixture<PageAuditReviewComponent>;

  beforeEach(async () => {
    // Mock window.analytics
    (window as any).analytics = {
      track: jasmine.createSpy('track'),
      identify: jasmine.createSpy('identify')
    };

    const authSpy = jasmine.createSpyObj('AuthService', ['user$', 'isAuthenticated$', 'loginWithRedirect', 'logout', 'getAccessTokenSilently']);
    const auditSpy = jasmine.createSpyObj('AuditService', ['getAuditStats', 'getPage', 'getAuditElements', 'requestReport', 'deleteRecommendation']);
    const segmentSpy = jasmine.createSpyObj('SegmentIOService', ['trackExportReportAuthenticatedClick', 'trackExportReportNonAuthenticatedClick', 'sendRecommendationDeletedMessage']);
    const locationSpy = jasmine.createSpyObj('Location', ['back']);
    const dialogSpy = jasmine.createSpyObj('MatDialog', ['open']);
    const authServiceSpy = jasmine.createSpyObj('AuthorizationService', ['isAuthorized']);
    const reportSpy = jasmine.createSpyObj('ReportService', ['getExcelReport', 'getPDFReport']);

    // Setup default return values
    authSpy.user$ = of(null);
    authSpy.isAuthenticated$ = of(false);
    auditSpy.getAuditStats.and.returnValue(of({}));
    auditSpy.getPage.and.returnValue(of([]));
    auditSpy.getAuditElements.and.returnValue(of({}));
    reportSpy.getExcelReport.and.returnValue(of(new Blob()));
    reportSpy.getPDFReport.and.returnValue(of(new Blob()));

    await TestBed.configureTestingModule({
      imports: [
        CommonModule,
        HttpClientTestingModule,
        RouterTestingModule
      ],
      declarations: [
        PageAuditReviewComponent,
        FilterByCategoryPipe,
        FilterSeverityPipe
      ],
      schemas: [NO_ERRORS_SCHEMA],
      providers: [
        { provide: AuthService, useValue: authSpy },
        { provide: AuditService, useValue: auditSpy },
        { provide: SegmentIOService, useValue: segmentSpy },
        { provide: Location, useValue: locationSpy },
        { provide: MatDialog, useValue: dialogSpy },
        { provide: AuthorizationService, useValue: authServiceSpy },
        { provide: ReportService, useValue: reportSpy }
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(PageAuditReviewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
