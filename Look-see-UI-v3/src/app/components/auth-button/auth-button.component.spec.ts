import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AuthService } from '@auth0/auth0-angular';
import { of } from 'rxjs';
import { SegmentIOService } from '../../services/segmentio.service';
import { AuthButtonComponent } from './auth-button.component';

describe('AuthButtonComponent', () => {
  let component: AuthButtonComponent;
  let fixture: ComponentFixture<AuthButtonComponent>;

  beforeEach(async () => {
    const authSpy = jasmine.createSpyObj('AuthService', ['user$', 'isAuthenticated$', 'loginWithRedirect', 'logout']);
    const segmentSpy = jasmine.createSpyObj('SegmentIOService', ['trackLoginClick']);

    // Setup default return values
    authSpy.user$ = of(null);
    authSpy.isAuthenticated$ = of(false);

    await TestBed.configureTestingModule({
      declarations: [AuthButtonComponent],
      providers: [
        { provide: AuthService, useValue: authSpy },
        { provide: SegmentIOService, useValue: segmentSpy }
      ]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(AuthButtonComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
