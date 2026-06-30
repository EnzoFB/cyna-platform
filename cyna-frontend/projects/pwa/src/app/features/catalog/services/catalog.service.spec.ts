import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { CatalogService } from './catalog.service';
import { environment } from '../../../../environments/environment';

const API = `${environment.apiUrl}`;

const productDto = (overrides: Record<string, unknown> = {}) => ({
  id: 'prod-1',
  translations: {
    fr: { name: 'SOC Standard', serviceDescription: 'desc', technicalDescription: 'tech', highlightPoints: ['point 1'] },
  },
  categoryId: 'cat-1',
  categoryName: 'SOC',
  priorityLevel: 1,
  monthlyPrice: 100,
  annualPrice: 1000,
  discountedMonthlyPrice: null,
  discountedAnnualPrice: null,
  promotionDiscountPercent: null,
  currency: 'EUR',
  primaryImageBase64: null,
  isPublished: true,
  isAvailable: true,
  ...overrides,
});

const categoryDto = () => ({
  id: 'cat-1',
  name: 'SOC',
  translations: { fr: { fullName: 'Security Operations Center', description: 'desc' } },
  imageBase64: null,
  active: true,
  createdAt: '2024-01-01',
  updatedAt: '2024-01-01',
});

describe('CatalogService', () => {
  let service: CatalogService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service  = TestBed.inject(CatalogService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  describe('getProductPage', () => {
    it('sends page, size and published=true by default', () => {
      service.getProductPage({ page: 0, size: 12 }).subscribe();

      const req = httpMock.expectOne(r => r.url.includes('/products'));
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('12');
      expect(req.request.params.get('published')).toBe('true');

      req.flush({ success: true, data: { items: [], totalItems: 0, page: 0, size: 12, totalPages: 0 }, timestamp: '' });
    });

    it('passes categoryId and search params when provided', () => {
      service.getProductPage({ page: 0, size: 10, categoryId: 'cat-1', search: 'soc' }).subscribe();

      const req = httpMock.expectOne(r => r.url.includes('/products'));
      expect(req.request.params.get('categoryId')).toBe('cat-1');
      expect(req.request.params.get('search')).toBe('soc');
      req.flush({ success: true, data: { items: [], totalItems: 0, page: 0, size: 10, totalPages: 0 }, timestamp: '' });
    });

    it('maps product DTO to domain Product correctly', () => {
      let result: unknown;
      service.getProductPage({ page: 0, size: 1 }).subscribe(p => (result = p));

      const req = httpMock.expectOne(r => r.url.includes('/products'));
      req.flush({ success: true, data: { items: [productDto()], totalItems: 1, page: 0, size: 1, totalPages: 1 }, timestamp: '' });

      const item = (result as { items: unknown[] }).items[0] as Record<string, unknown>;
      expect(item['id']).toBe('prod-1');
      expect((item['translations'] as Record<string, { name: string }>)['fr'].name).toBe('SOC Standard');
      expect(item['originalMonthlyPrice']).toBeNull();
    });

    it('applies promotion prices and preserves original price', () => {
      let result: unknown;
      service.getProductPage({ page: 0, size: 1 }).subscribe(p => (result = p));

      const req = httpMock.expectOne(r => r.url.includes('/products'));
      req.flush({
        success: true,
        data: {
          items: [productDto({ discountedMonthlyPrice: 80, promotionDiscountPercent: 20 })],
          totalItems: 1, page: 0, size: 1, totalPages: 1,
        },
        timestamp: '',
      });

      const item = (result as { items: unknown[] }).items[0] as Record<string, unknown>;
      expect(item['monthlyPrice']).toBe(80);
      expect(item['originalMonthlyPrice']).toBe(100);
      expect(item['promotionDiscountPercent']).toBe(20);
    });
  });

  describe('getCategories', () => {
    it('GET /categories and maps to Category domain objects', () => {
      let categories: unknown[] | undefined;
      service.getCategories().subscribe(c => (categories = c));

      const req = httpMock.expectOne(`${API}/categories?activeOnly=true`);
      expect(req.request.method).toBe('GET');
      req.flush({ success: true, data: [categoryDto()], timestamp: '' });

      expect(categories?.length).toBe(1);
      const cat = (categories as Record<string, unknown>[])[0];
      expect(cat['id']).toBe('cat-1');
      expect((cat['translations'] as Record<string, { fullName: string }>)['fr'].fullName)
        .toBe('Security Operations Center');
    });
  });

  describe('getProductById', () => {
    it('GET /products/:id and maps to ProductDetail with images', () => {
      let detail: unknown;
      service.getProductById('prod-1').subscribe(d => (detail = d));

      const req = httpMock.expectOne(`${API}/products/prod-1`);
      expect(req.request.method).toBe('GET');
      req.flush({
        success: true,
        data: { ...productDto(), freeTrialDays: 14, images: [{ id: 'img-1', base64: 'abc' }] },
        timestamp: '',
      });

      const d = detail as Record<string, unknown>;
      expect(d['id']).toBe('prod-1');
      expect(d['freeTrialDays']).toBe(14);
      expect((d['images'] as unknown[]).length).toBe(1);
    });

    it('returns null when API returns null data', () => {
      let detail: unknown = 'not-null';
      service.getProductById('missing').subscribe(d => (detail = d));

      httpMock.expectOne(`${API}/products/missing`)
        .flush({ success: true, data: null, timestamp: '' });

      expect(detail).toBeNull();
    });
  });
});
