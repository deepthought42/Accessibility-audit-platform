import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AuthService } from '@auth0/auth0-angular';
import { of } from 'rxjs';
import { IntegrationsPanelComponent } from './integrations-panel.component';

describe('IntegrationsPanelComponent', () => {
  let component: IntegrationsPanelComponent;
  let fixture: ComponentFixture<IntegrationsPanelComponent>;

  beforeEach(async () => {
    const authSpy = jasmine.createSpyObj('AuthService', ['user$', 'isAuthenticated$', 'loginWithRedirect', 'logout']);

    // Setup default return values
    authSpy.user$ = of(null);
    authSpy.isAuthenticated$ = of(false);

    await TestBed.configureTestingModule({
      declarations: [IntegrationsPanelComponent],
      providers: [
        { provide: AuthService, useValue: authSpy }
      ]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(IntegrationsPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
