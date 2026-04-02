import { TestBed } from '@angular/core/testing';
import { JwtHelperService } from '@auth0/angular-jwt';
import { AuthService } from '@auth0/auth0-angular';
import { AuthorizationService } from './authorization.service';

describe('AuthorizationService', () => {
  let service: AuthorizationService;
  let jwtHelperSpy: jasmine.SpyObj<JwtHelperService>;

  beforeEach(() => {
    jwtHelperSpy = jasmine.createSpyObj('JwtHelperService', ['decodeToken']);

    TestBed.configureTestingModule({
      providers: [
        AuthorizationService,
        { provide: JwtHelperService, useValue: jwtHelperSpy },
        { provide: AuthService, useValue: {} }
      ]
    });

    service = TestBed.inject(AuthorizationService);
  });

  it('authorizes when allowed role is empty', () => {
    expect(service.isAuthorized('', 'token')).toBeTrue();
  });

  it('rejects when token cannot be decoded', () => {
    jwtHelperSpy.decodeToken.and.returnValue(null as any);

    expect(service.isAuthorized('admin', 'token')).toBeFalse();
  });

  it('authorizes when required permission is present', () => {
    jwtHelperSpy.decodeToken.and.returnValue({ permissions: ['admin'] });

    expect(service.isAuthorized('admin', 'token')).toBeTrue();
  });

  it('authorizes when user permissions list is empty', () => {
    jwtHelperSpy.decodeToken.and.returnValue({ permissions: [] });

    expect(service.isAuthorized('admin', 'token')).toBeTrue();
  });

  it('rejects when required permission is missing', () => {
    jwtHelperSpy.decodeToken.and.returnValue({ permissions: ['reader'] });

    expect(service.isAuthorized('admin', 'token')).toBeFalse();
  });
});
