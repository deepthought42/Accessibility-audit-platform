import { CommonModule } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterTestingModule } from '@angular/router/testing';
import { AuthService } from '@auth0/auth0-angular';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { of } from 'rxjs';
import { AuditService } from '../../services/audit.service';
import { SegmentIOService } from '../../services/segmentio.service';
import { WebSocketService } from '../../services/websocket/web-socket.service';
import { ScoreBadgeComponent } from '../shared/score-badge/score-badge.component';
import { ScoreGaugeComponent } from '../shared/score-gauge/score-gauge.component';
import { AuditDashboardComponent } from './audit-dashboard.component';

describe('AuditDashboardComponent', () => {
  let component: AuditDashboardComponent;
  let fixture: ComponentFixture<AuditDashboardComponent>;

  beforeEach(async () => {
    const authSpy = jasmine.createSpyObj('AuthService', ['user$', 'isAuthenticated$', 'loginWithRedirect', 'logout']);
    const auditSpy = jasmine.createSpyObj('AuditService', ['getAuditStats', 'getPageAuditsForDomainAudit']);
    const segmentSpy = jasmine.createSpyObj('SegmentIOService', ['trackLoginClick']);
    const websocketSpy = jasmine.createSpyObj('WebSocketService', ['listenChannel', 'unsubscribeChannel']);

    // Setup default return values
    authSpy.user$ = of(null);
    authSpy.isAuthenticated$ = of(false);
    auditSpy.getAuditStats.and.returnValue(of({}));
    auditSpy.getPageAuditsForDomainAudit.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [
        CommonModule,
        HttpClientTestingModule,
        RouterTestingModule,
        FontAwesomeModule,
        MatProgressSpinnerModule
      ],
      declarations: [AuditDashboardComponent, ScoreBadgeComponent, ScoreGaugeComponent],
      providers: [
        { provide: AuthService, useValue: authSpy },
        { provide: AuditService, useValue: auditSpy },
        { provide: SegmentIOService, useValue: segmentSpy },
        { provide: WebSocketService, useValue: websocketSpy }
      ]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(AuditDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
