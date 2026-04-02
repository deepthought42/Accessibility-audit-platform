import { CommonModule } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AuthService } from '@auth0/auth0-angular';
import { of } from 'rxjs';
import { DomainService } from '../../models/domain/domain.service';
import { SegmentIOService } from '../../services/segmentio.service';
import { AuthButtonComponent } from '../auth-button/auth-button.component';
import { DomainsComponent } from './domains.component';

describe('DomainsComponent', () => {
  let component: DomainsComponent;
  let fixture: ComponentFixture<DomainsComponent>;
  // let mockAuthService: jasmine.SpyObj<AuthService>;
  // let mockDomainService: jasmine.SpyObj<DomainService>;

  beforeEach(async () => {
    const authSpy = jasmine.createSpyObj('AuthService', ['user$', 'isAuthenticated$', 'loginWithRedirect', 'logout']);
    const domainSpy = jasmine.createSpyObj('DomainService', ['getDomains']);
    const segmentSpy = jasmine.createSpyObj('SegmentIOService', ['trackLoginClick']);

    // Setup default return values
    authSpy.user$ = of(null);
    authSpy.isAuthenticated$ = of(false);
    domainSpy.getDomains.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [
        CommonModule,
        HttpClientTestingModule
      ],
      declarations: [
        DomainsComponent,
        AuthButtonComponent
      ],
      providers: [
        { provide: AuthService, useValue: authSpy },
        { provide: DomainService, useValue: domainSpy },
        { provide: SegmentIOService, useValue: segmentSpy }
      ]
    })
    .compileComponents();

    // mockAuthService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    // mockDomainService = TestBed.inject(DomainService) as jasmine.SpyObj<DomainService>;
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DomainsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
