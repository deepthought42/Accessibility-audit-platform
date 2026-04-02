import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { AuthService } from '@auth0/auth0-angular';
import { MessageService } from './message.service';
import { SegmentIOService } from './segmentio.service';
import { Auth0Service } from './auth0.service';

describe('Auth0Service', () => {
  let service: Auth0Service;
  let authSpy: jasmine.SpyObj<AuthService>;
  let messageServiceSpy: jasmine.SpyObj<MessageService>;
  let segmentSpy: jasmine.SpyObj<SegmentIOService>;

  beforeEach(() => {
    authSpy = jasmine.createSpyObj('AuthService', ['loginWithRedirect', 'loginWithPopup']);
    messageServiceSpy = jasmine.createSpyObj('MessageService', ['add']);
    segmentSpy = jasmine.createSpyObj('SegmentIOService', ['trackLoginClick']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        Auth0Service,
        { provide: AuthService, useValue: authSpy },
        { provide: MessageService, useValue: messageServiceSpy },
        { provide: SegmentIOService, useValue: segmentSpy }
      ]
    });

    service = TestBed.inject(Auth0Service);
  });

  it('tracks and calls loginWithRedirect', () => {
    service.loginWithRedirect();

    expect(segmentSpy.trackLoginClick).toHaveBeenCalled();
    expect(authSpy.loginWithRedirect).toHaveBeenCalled();
  });

  it('tracks and calls loginWithPopup', () => {
    service.loginWithPopup();

    expect(segmentSpy.trackLoginClick).toHaveBeenCalled();
    expect(authSpy.loginWithPopup).toHaveBeenCalled();
  });

  it('handleError logs and returns fallback value', (done) => {
    spyOn(console, 'error');
    const handler = (service as any).handleError('login', 'fallback');

    handler(new Error('boom')).subscribe((value: string) => {
      expect(console.error).toHaveBeenCalled();
      expect(messageServiceSpy.add).toHaveBeenCalledWith('AuditService: login failed: boom');
      expect(value).toBe('fallback');
      done();
    });
  });
});
