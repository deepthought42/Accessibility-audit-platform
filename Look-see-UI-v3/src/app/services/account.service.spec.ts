import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AccountService } from './account.service';
import { MessageService } from './message.service';
import { environment } from '../../environments/environment';

describe('AccountService', () => {
  let service: AccountService;
  let httpMock: HttpTestingController;
  let messageServiceSpy: jasmine.SpyObj<MessageService>;

  beforeEach(() => {
    messageServiceSpy = jasmine.createSpyObj('MessageService', ['add']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        AccountService,
        { provide: MessageService, useValue: messageServiceSpy }
      ]
    });

    service = TestBed.inject(AccountService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('gets account details from API', () => {
    const mockAccount: any = { id: 1, email: 'a@example.com' };

    service.getAccountDetails().subscribe((account) => {
      expect(account).toEqual(mockAccount);
    });

    const request = httpMock.expectOne(`${environment.api_url}/accounts`);
    expect(request.request.method).toBe('GET');
    request.flush(mockAccount);
  });

  it('handleError logs and returns fallback value', (done) => {
    spyOn(console, 'error');
    const handler = (service as any).handleError('fetchAccount', { id: 0 });

    handler(new Error('network')).subscribe((value: any) => {
      expect(console.error).toHaveBeenCalled();
      expect(messageServiceSpy.add).toHaveBeenCalledWith('AuditService: fetchAccount failed: network');
      expect(value).toEqual({ id: 0 });
      done();
    });
  });
});
