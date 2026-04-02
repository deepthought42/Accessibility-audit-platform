import { CommonModule } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { AuthService } from '@auth0/auth0-angular';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { of } from 'rxjs';
import { DomainService } from '../../models/domain/domain.service';
import { SegmentIOService } from '../../services/segmentio.service';
import { AuthButtonComponent } from '../auth-button/auth-button.component';
import { NavBarComponent } from './nav-bar.component';

describe('NavBarComponent', () => {
  let component: NavBarComponent;
  let fixture: ComponentFixture<NavBarComponent>;

  beforeEach(async () => {
    const authSpy = jasmine.createSpyObj('AuthService', ['user$', 'isAuthenticated$', 'loginWithRedirect', 'logout']);
    const domainSpy = jasmine.createSpyObj('DomainService', ['getDomainExcelReport']);
    const segmentSpy = jasmine.createSpyObj('SegmentIOService', ['trackExportDomainReportClick']);

    // Setup default return values
    authSpy.user$ = of(null);
    authSpy.isAuthenticated$ = of(false);
    domainSpy.getDomainExcelReport.and.returnValue(of(new Blob()));

    await TestBed.configureTestingModule({
      imports: [
        CommonModule,
        HttpClientTestingModule,
        RouterTestingModule,
        FontAwesomeModule
      ],
      declarations: [
        NavBarComponent,
        AuthButtonComponent
      ],
      providers: [
        { provide: AuthService, useValue: authSpy },
        { provide: DomainService, useValue: domainSpy },
        { provide: SegmentIOService, useValue: segmentSpy }
      ]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(NavBarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
