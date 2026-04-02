import { CommonModule } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { AuthService } from '@auth0/auth0-angular';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { of } from 'rxjs';
import { AuditService } from '../../services/audit.service';
import { SegmentIOService } from '../../services/segmentio.service';
import { WebSocketService } from '../../services/websocket/web-socket.service';
import { AuditListComponent } from './audit-list.component';

describe('AuditListComponent', () => {
  let component: AuditListComponent;
  let fixture: ComponentFixture<AuditListComponent>;

  beforeEach(async () => {
    const authSpy = jasmine.createSpyObj('AuthService', ['user$', 'isAuthenticated$', 'loginWithRedirect', 'logout']);
    const auditSpy = jasmine.createSpyObj('AuditService', ['getAudits']);
    const segmentSpy = jasmine.createSpyObj('SegmentIOService', ['trackLoginClick']);
    const websocketSpy = jasmine.createSpyObj('WebSocketService', ['listenChannel', 'unsubscribeChannel']);

    // Setup default return values
    authSpy.user$ = of(null);
    authSpy.isAuthenticated$ = of(false);
    auditSpy.getAudits.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [
        CommonModule,
        HttpClientTestingModule,
        RouterTestingModule,
        FontAwesomeModule
      ],
      declarations: [AuditListComponent],
      schemas: [NO_ERRORS_SCHEMA],
      providers: [
        { provide: AuthService, useValue: authSpy },
        { provide: AuditService, useValue: auditSpy },
        { provide: SegmentIOService, useValue: segmentSpy },
        { provide: WebSocketService, useValue: websocketSpy }
      ]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(AuditListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
