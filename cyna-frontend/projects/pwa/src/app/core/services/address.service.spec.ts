import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AddressService } from './address.service';
import { AddressPayload } from '../models/address.model';
import { environment } from '../../../environments/environment';

describe('AddressService', () => {
  let service: AddressService;
  let httpMock: HttpTestingController;

  const base = `${environment.apiUrl}/account/addresses`;

  const payload: AddressPayload = {
    firstName: 'Alice',
    lastName: 'Dupont',
    label: 'Bureau',
    address: '1 rue de la Paix',
    zipCode: '75001',
    city: 'Paris',
    region: 'Île-de-France',
    countryCode: 'FR',
    phone: '+33612345678',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service  = TestBed.inject(AddressService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getAll GET /account/addresses', () => {
    service.getAll().subscribe();
    const req = httpMock.expectOne(base);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: [], timestamp: '' });
  });

  it('create POST /account/addresses with payload', () => {
    let id: string | undefined;
    service.create(payload).subscribe(r => (id = r.data));

    const req = httpMock.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush({ success: true, data: 'addr-1', timestamp: '' });

    expect(id).toBe('addr-1');
  });

  it('update PUT /account/addresses/:id with payload', () => {
    service.update('addr-1', payload).subscribe();

    const req = httpMock.expectOne(`${base}/addr-1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(payload);
    req.flush({ success: true, data: null, timestamp: '' });
  });

  it('delete DELETE /account/addresses/:id', () => {
    service.delete('addr-1').subscribe();

    const req = httpMock.expectOne(`${base}/addr-1`);
    expect(req.request.method).toBe('DELETE');
    req.flush({ success: true, data: null, timestamp: '' });
  });

  it('setDefault PATCH /account/addresses/:id/default with empty body', () => {
    service.setDefault('addr-1').subscribe();

    const req = httpMock.expectOne(`${base}/addr-1/default`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({});
    req.flush({ success: true, data: null, timestamp: '' });
  });
});
