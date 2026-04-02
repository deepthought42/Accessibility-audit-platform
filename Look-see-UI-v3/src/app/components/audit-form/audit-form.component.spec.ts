import { CommonModule } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { AuthService } from '@auth0/auth0-angular';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { of } from 'rxjs';
import { DomainService } from '../../models/domain/domain.service';
import { AuditorService } from '../../services/auditor.service';
import { SegmentIOService } from '../../services/segmentio.service';
import { AuditFormComponent } from './audit-form.component';

describe('AuditFormComponent', () => {
  let component: AuditFormComponent;
  let fixture: ComponentFixture<AuditFormComponent>;

  beforeEach(async () => {
    const authSpy = jasmine.createSpyObj('AuthService', ['user$', 'isAuthenticated$', 'loginWithRedirect', 'logout']);
    const domainSpy = jasmine.createSpyObj('DomainService', ['getDomains']);
    const auditorSpy = jasmine.createSpyObj('AuditorService', ['startAudit']);
    const segmentSpy = jasmine.createSpyObj('SegmentIOService', ['sendDomainUxAuditStartedMessage', 'sendUxAuditStartedMessage']);

    // Setup default return values
    authSpy.user$ = of(null);
    authSpy.isAuthenticated$ = of(false);
    domainSpy.getDomains.and.returnValue(of([]));
    auditorSpy.startAudit.and.returnValue(of({}));

    await TestBed.configureTestingModule({
      imports: [
        CommonModule,
        HttpClientTestingModule,
        RouterTestingModule,
        ReactiveFormsModule,
        FontAwesomeModule
      ],
      declarations: [AuditFormComponent],
      providers: [
        { provide: AuthService, useValue: authSpy },
        { provide: DomainService, useValue: domainSpy },
        { provide: AuditorService, useValue: auditorSpy },
        { provide: SegmentIOService, useValue: segmentSpy }
      ]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(AuditFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
