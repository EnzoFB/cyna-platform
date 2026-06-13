import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { LoggingService } from './logging.service';

describe('LoggingService', () => {
  let service: LoggingService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(LoggingService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should redact password in log message', () => {
    const raw = 'password: "secret123"';
    const redacted = (service as any).stripPII(raw);
    expect(redacted).not.toContain('secret123');
    expect(redacted).toContain('[REDACTED]');
  });

  it('should redact email in log message', () => {
    const raw = 'email: "john@example.com"';
    const redacted = (service as any).stripPII(raw);
    expect(redacted).not.toContain('john@example.com');
    expect(redacted).toContain('[REDACTED]');
  });

  it('should redact card number in log message', () => {
    const raw = 'cardNumber: "4242-4242-4242-4242"';
    const redacted = (service as any).stripPII(raw);
    expect(redacted).not.toContain('4242');
    expect(redacted).toContain('[REDACTED]');
  });

  it('should build a valid ClientLogEntry', () => {
    const entry = (service as any).buildEntry('ERROR', new Error('test error'));
    expect(entry.level).toBe('ERROR');
    expect(entry.message).toBe('test error');
    expect(entry.timestamp).toBeTruthy();
    expect(entry.correlationId).toBeTruthy();
  });

  it('should send logs immediately on error', fakeAsync(() => {
    service.logError(new Error('test error'));
    tick(1);

    const req = httpMock.expectOne('http://localhost:8080/api/v1/logs/client');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.length).toBe(1);
    req.flush({});
  }));
});
