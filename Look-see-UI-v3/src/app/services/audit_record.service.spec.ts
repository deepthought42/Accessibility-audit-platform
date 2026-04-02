import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuditRecordService } from './audit_record.service';
import { MessageService } from './message.service';
import { environment } from '../../environments/environment';

describe('AuditRecordService', () => {
  let service: AuditRecordService;
  let httpMock: HttpTestingController;
  let messageServiceSpy: jasmine.SpyObj<MessageService>;

  beforeEach(() => {
    messageServiceSpy = jasmine.createSpyObj('MessageService', ['add']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        AuditRecordService,
        { provide: MessageService, useValue: messageServiceSpy }
      ]
    });

    service = TestBed.inject(AuditRecordService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('gets audit records from API', () => {
    const mockRecords: any[] = [{ id: 1 }, { id: 2 }];

    service.getAuditRecords().subscribe((records) => {
      expect(records).toEqual(mockRecords);
    });

    const request = httpMock.expectOne(`${environment.api_url}/auditrecords`);
    expect(request.request.method).toBe('GET');
    request.flush(mockRecords);
  });

  it('handleError logs and returns fallback value', (done) => {
    spyOn(console, 'error');
    const handler = (service as any).handleError('getAuditRecords', []);

    handler(new Error('boom')).subscribe((value: any[]) => {
      expect(console.error).toHaveBeenCalled();
      expect(messageServiceSpy.add).toHaveBeenCalledWith('AuditRecordService: getAuditRecords failed: boom');
      expect(value).toEqual([]);
      done();
    });
  });
});
