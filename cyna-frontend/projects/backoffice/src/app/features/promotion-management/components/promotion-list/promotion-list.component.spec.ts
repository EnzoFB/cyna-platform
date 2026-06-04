import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideTranslateService, TranslateLoader } from '@ngx-translate/core';
import { Observable, of } from 'rxjs';
import { PromotionListComponent } from './promotion-list.component';
import { PromotionService, AdminPromotion } from '../../../../core/services/promotion.service';
import { CategoryService } from '../../../../core/services/category.service';
import { ProductService } from '../../../../core/services/product.service';

class FakeTranslateLoader implements TranslateLoader {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  getTranslation(): Observable<any> {
    return of({});
  }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

function makePromotion(overrides: Partial<AdminPromotion> = {}): AdminPromotion {
  return {
    id: 'promo-' + Math.random().toString(36).slice(2),
    productId: 'prod-1',
    productName: 'EDR Pro',
    productCategoryName: 'EDR',
    baseMonthlyPrice: 100,
    baseAnnualPrice: 1000,
    discountedMonthlyPrice: 80,
    discountedAnnualPrice: 800,
    currency: 'EUR',
    discountPercent: 20,
    translations: {
      fr: { marketingText: 'Texte FR' },
      en: { marketingText: 'Texte EN' },
    },
    startAt: new Date(Date.now() - 3_600_000).toISOString(),
    endAt:   new Date(Date.now() + 86_400_000).toISOString(),
    enabled: true,
    showInCarousel: false,
    carouselOrder: null,
    activeNow: true,
    productAvailable: true,
    productPublished: true,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    ...overrides,
  };
}

const emptyResponse = <T>(data: T) => of({ success: true, data, timestamp: '' });

// ── Suite ─────────────────────────────────────────────────────────────────────

describe('PromotionListComponent', () => {
  let component: PromotionListComponent;
  let fixture: ComponentFixture<PromotionListComponent>;
  let promotionSpy: jasmine.SpyObj<PromotionService>;
  let categorySpy:  jasmine.SpyObj<CategoryService>;
  let productSpy:   jasmine.SpyObj<ProductService>;

  beforeEach(async () => {
    promotionSpy = jasmine.createSpyObj('PromotionService', [
      'getPromotions', 'getCarouselSettings', 'createPromotion',
      'updatePromotion', 'deletePromotion', 'reorderCarousel', 'updateCarouselSettings',
      'addToCarousel', 'removeFromCarousel',
    ]);
    categorySpy = jasmine.createSpyObj('CategoryService', ['getCategories']);
    productSpy  = jasmine.createSpyObj('ProductService',  ['getProducts']);

    promotionSpy.getPromotions.and.returnValue(emptyResponse([]));
    promotionSpy.getCarouselSettings.and.returnValue(
      emptyResponse({ translations: {}, maxSlides: 5 })
    );
    categorySpy.getCategories.and.returnValue(emptyResponse([]));

    await TestBed.configureTestingModule({
      imports: [PromotionListComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: PromotionService, useValue: promotionSpy },
        { provide: CategoryService,  useValue: categorySpy  },
        { provide: ProductService,   useValue: productSpy   },
        provideTranslateService({
          defaultLanguage: 'fr',
          loader: { provide: TranslateLoader, useClass: FakeTranslateLoader },
        }),
      ],
    }).compileComponents();

    fixture   = TestBed.createComponent(PromotionListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ── carouselSlides ────────────────────────────────────────────────────────

  describe('carouselSlides', () => {
    it('contains only promotions with showInCarousel = true', () => {
      const inCarousel  = makePromotion({ showInCarousel: true,  carouselOrder: 1 });
      const outCarousel = makePromotion({ showInCarousel: false });
      component['promotions'].set([outCarousel, inCarousel]);

      expect(component['carouselSlides']()).toEqual([inCarousel]);
    });

    it('is sorted by carouselOrder ascending', () => {
      const p1 = makePromotion({ showInCarousel: true, carouselOrder: 3 });
      const p2 = makePromotion({ showInCarousel: true, carouselOrder: 1 });
      const p3 = makePromotion({ showInCarousel: true, carouselOrder: 2 });
      component['promotions'].set([p1, p2, p3]);

      const slides = component['carouselSlides']();
      expect(slides.map(s => s.carouselOrder)).toEqual([1, 2, 3]);
    });

    it('is empty when no promotion is in the carousel', () => {
      component['promotions'].set([makePromotion(), makePromotion()]);
      expect(component['carouselSlides']()).toEqual([]);
    });
  });

  // ── carouselIsFull / carouselMaxSlides ────────────────────────────────────

  describe('carouselIsFull', () => {
    it('is false when carousel has fewer slides than maxSlides', () => {
      component['carouselSettings'].set({ translations: {}, maxSlides: 3 });
      component['promotions'].set([
        makePromotion({ showInCarousel: true, carouselOrder: 1 }),
        makePromotion({ showInCarousel: true, carouselOrder: 2 }),
      ]);
      expect(component['carouselIsFull']()).toBeFalse();
    });

    it('is true when carousel reaches maxSlides', () => {
      component['carouselSettings'].set({ translations: {}, maxSlides: 2 });
      component['promotions'].set([
        makePromotion({ showInCarousel: true, carouselOrder: 1 }),
        makePromotion({ showInCarousel: true, carouselOrder: 2 }),
      ]);
      expect(component['carouselIsFull']()).toBeTrue();
    });

    it('reflects the configured maxSlides value', () => {
      component['carouselSettings'].set({ translations: {}, maxSlides: 7 });
      expect(component['carouselMaxSlides']()).toBe(7);
    });
  });

  // ── promotionsNotInCarousel ───────────────────────────────────────────────

  describe('promotionsNotInCarousel', () => {
    it('excludes promotions already in the carousel', () => {
      const inCarousel  = makePromotion({ showInCarousel: true, carouselOrder: 1 });
      const outCarousel = makePromotion({ showInCarousel: false });
      component['promotions'].set([inCarousel, outCarousel]);

      expect(component['promotionsNotInCarousel']()).toEqual([outCarousel]);
    });
  });

  // ── carouselAddFiltered ───────────────────────────────────────────────────

  describe('carouselAddFiltered', () => {
    beforeEach(() => {
      component['promotions'].set([
        makePromotion({ showInCarousel: false, productName: 'EDR Pro' }),
        makePromotion({ showInCarousel: false, productName: 'SOC Standard', productCategoryName: 'SOC' }),
        makePromotion({ showInCarousel: true,  carouselOrder: 1, productName: 'XDR' }),
      ]);
    });

    it('returns all non-carousel promotions when query is empty', () => {
      component['carouselAddQuery'].set('');
      expect(component['carouselAddFiltered']().length).toBe(2);
    });

    it('filters by product name (case-insensitive)', () => {
      component['carouselAddQuery'].set('edr');
      expect(component['carouselAddFiltered']().length).toBe(1);
      expect(component['carouselAddFiltered']()[0].productName).toBe('EDR Pro');
    });

    it('filters by category name', () => {
      component['carouselAddQuery'].set('soc');
      expect(component['carouselAddFiltered']().length).toBe(1);
      expect(component['carouselAddFiltered']()[0].productCategoryName).toBe('SOC');
    });
  });

  // ── toggleCarouselAdd ─────────────────────────────────────────────────────

  describe('toggleCarouselAdd', () => {
    it('opens the dropdown when closed', () => {
      component['carouselAddOpen'].set(false);
      component['toggleCarouselAdd']();
      expect(component['carouselAddOpen']()).toBeTrue();
    });

    it('closes the dropdown and clears the query when open', () => {
      component['carouselAddOpen'].set(true);
      component['carouselAddQuery'].set('edr');
      component['toggleCarouselAdd']();
      expect(component['carouselAddOpen']()).toBeFalse();
      expect(component['carouselAddQuery']()).toBe('');
    });
  });

  // ── carouselSlideStatus ───────────────────────────────────────────────────

  describe('carouselSlideStatus', () => {
    it('returns "active" when promotion is activeNow and product is available+published', () => {
      const p = makePromotion({ activeNow: true, productAvailable: true, productPublished: true });
      expect(component['carouselSlideStatus'](p)).toBe('active');
    });

    it('returns "masked" when product is unavailable', () => {
      const p = makePromotion({ activeNow: true, productAvailable: false, productPublished: true });
      expect(component['carouselSlideStatus'](p)).toBe('masked');
    });

    it('returns "masked" when product is not published', () => {
      const p = makePromotion({ activeNow: false, productAvailable: true, productPublished: false });
      expect(component['carouselSlideStatus'](p)).toBe('masked');
    });

    it('returns "scheduled" when enabled but not yet active', () => {
      const p = makePromotion({ activeNow: false, enabled: true, productAvailable: true, productPublished: true });
      expect(component['carouselSlideStatus'](p)).toBe('scheduled');
    });

    it('returns "disabled" when not enabled and product is ok', () => {
      const p = makePromotion({ activeNow: false, enabled: false, productAvailable: true, productPublished: true });
      expect(component['carouselSlideStatus'](p)).toBe('disabled');
    });
  });

  describe('carouselSlideIsShown', () => {
    it('returns true only when status is active', () => {
      const active   = makePromotion({ activeNow: true,  productAvailable: true, productPublished: true });
      const masked   = makePromotion({ activeNow: false, productAvailable: false });
      expect(component['carouselSlideIsShown'](active)).toBeTrue();
      expect(component['carouselSlideIsShown'](masked)).toBeFalse();
    });
  });

  // ── displayedPromotions (sort) ────────────────────────────────────────────

  describe('displayedPromotions', () => {
    const older = makePromotion({ startAt: new Date(Date.now() - 200_000).toISOString(), activeNow: false });
    const newer = makePromotion({ startAt: new Date(Date.now() - 100_000).toISOString(), activeNow: false });
    const active = makePromotion({ activeNow: true });

    beforeEach(() => {
      component['promotions'].set([older, newer, active]);
    });

    it('defaults to active-first then startAt descending', () => {
      component['sortColumn'].set(null);
      const result = component['displayedPromotions']();
      expect(result[0].activeNow).toBeTrue();
      expect(result[1].startAt > result[2].startAt).toBeTrue();
    });

    it('sorts by productName ascending when column is productName', () => {
      const a = makePromotion({ productName: 'AAA' });
      const b = makePromotion({ productName: 'ZZZ' });
      component['promotions'].set([b, a]);
      component['sortColumn'].set('productName');
      component['sortDirection'].set('asc');
      const result = component['displayedPromotions']();
      expect(result[0].productName).toBe('AAA');
    });

    it('sorts by discountPercent descending', () => {
      const low  = makePromotion({ discountPercent: 10 });
      const high = makePromotion({ discountPercent: 50 });
      component['promotions'].set([low, high]);
      component['sortColumn'].set('discountPercent');
      component['sortDirection'].set('desc');
      expect(component['displayedPromotions']()[0].discountPercent).toBe(50);
    });
  });

  // ── toggleSort ────────────────────────────────────────────────────────────

  describe('toggleSort', () => {
    it('sets column and asc on first click', () => {
      component['toggleSort']('productName');
      expect(component['sortColumn']()).toBe('productName');
      expect(component['sortDirection']()).toBe('asc');
    });

    it('switches to desc on second click on same column', () => {
      component['sortColumn'].set('productName');
      component['sortDirection'].set('asc');
      component['toggleSort']('productName');
      expect(component['sortDirection']()).toBe('desc');
    });

    it('resets sort on third click on same column', () => {
      component['sortColumn'].set('productName');
      component['sortDirection'].set('desc');
      component['toggleSort']('productName');
      expect(component['sortColumn']()).toBeNull();
      expect(component['sortDirection']()).toBe('asc');
    });

    it('isSortActive returns true only for active column', () => {
      component['sortColumn'].set('discountPercent');
      expect(component['isSortActive']('discountPercent')).toBeTrue();
      expect(component['isSortActive']('productName')).toBeFalse();
    });
  });

  // ── canSavePromotion ──────────────────────────────────────────────────────

  describe('canSavePromotion', () => {
    it('is false when saving is in progress', () => {
      component['saving'].set(true);
      expect(component['canSavePromotion']()).toBeFalse();
    });

    it('is false when productId is empty', () => {
      component['form'].update(f => ({ ...f, productId: '' }));
      expect(component['canSavePromotion']()).toBeFalse();
    });

    it('is false when discountPercent is out of range', () => {
      component['form'].update(f => ({ ...f, productId: 'p1', discountPercent: 0 }));
      expect(component['canSavePromotion']()).toBeFalse();
    });

    it('is false when marketingTextFr is blank', () => {
      component['form'].update(f => ({
        ...f, productId: 'p1', discountPercent: 20,
        marketingTextFr: '   ', marketingTextEn: 'EN text',
        startAtLocal: '2026-01-01T00:00', endAtLocal: '2026-12-31T00:00',
      }));
      expect(component['canSavePromotion']()).toBeFalse();
    });

    it('is true when all required fields are filled', () => {
      component['form'].set({
        productId: 'prod-1', discountPercent: 20,
        marketingTextFr: 'FR text', marketingTextEn: 'EN text',
        startAtLocal: '2026-01-01T00:00', endAtLocal: '2026-12-31T00:00',
        enabled: true,
      });
      expect(component['canSavePromotion']()).toBeTrue();
    });
  });

  // ── canSaveSettings ───────────────────────────────────────────────────────

  describe('canSaveSettings', () => {
    it('is false when both texts are empty', () => {
      component['carouselSettingsDraft'].set({ fixedTextFr: '', fixedTextEn: '', maxSlides: 5 });
      expect(component['canSaveSettings']()).toBeFalse();
    });

    it('is false when one text is missing', () => {
      component['carouselSettingsDraft'].set({ fixedTextFr: 'FR', fixedTextEn: '', maxSlides: 5 });
      expect(component['canSaveSettings']()).toBeFalse();
    });

    it('is true when both texts are filled', () => {
      component['carouselSettingsDraft'].set({ fixedTextFr: 'FR text', fixedTextEn: 'EN text', maxSlides: 5 });
      expect(component['canSaveSettings']()).toBeTrue();
    });

    it('is false while saving is in progress', () => {
      component['carouselSettingsDraft'].set({ fixedTextFr: 'FR', fixedTextEn: 'EN', maxSlides: 5 });
      component['carouselSettingsSaving'].set(true);
      expect(component['canSaveSettings']()).toBeFalse();
    });
  });

  // ── Incomplete locale badges ──────────────────────────────────────────────

  describe('locale incomplete badges', () => {
    it('isModalFrIncomplete is true when FR marketing text is blank', () => {
      component['form'].update(f => ({ ...f, marketingTextFr: '' }));
      expect(component['isModalFrIncomplete']()).toBeTrue();
    });

    it('isModalEnIncomplete is false when EN marketing text is filled', () => {
      component['form'].update(f => ({ ...f, marketingTextEn: 'EN text' }));
      expect(component['isModalEnIncomplete']()).toBeFalse();
    });

    it('isSettingsFrIncomplete is true when FR fixed text is blank', () => {
      component['carouselSettingsDraft'].update(d => ({ ...d, fixedTextFr: '' }));
      expect(component['isSettingsFrIncomplete']()).toBeTrue();
    });

    it('isSettingsEnIncomplete is false when EN fixed text is set', () => {
      component['carouselSettingsDraft'].update(d => ({ ...d, fixedTextEn: 'EN text' }));
      expect(component['isSettingsEnIncomplete']()).toBeFalse();
    });
  });

  // ── Modal state ───────────────────────────────────────────────────────────

  describe('modal state', () => {
    it('openCreateModal opens the modal in create mode', () => {
      component['openCreateModal']();
      expect(component['modalOpen']()).toBeTrue();
      expect(component['editingPromotion']()).toBeNull();
    });

    it('openEditModal opens the modal and pre-fills the form', () => {
      const promo = makePromotion({
        discountPercent: 30,
        translations: { fr: { marketingText: 'FR' }, en: { marketingText: 'EN' } },
        startAt: new Date().toISOString(),
        endAt:   new Date(Date.now() + 86_400_000).toISOString(),
        enabled: true,
      });
      component['openEditModal'](promo);

      expect(component['modalOpen']()).toBeTrue();
      expect(component['editingPromotion']()).toBe(promo);
      expect(component['form']().discountPercent).toBe(30);
      expect(component['form']().marketingTextFr).toBe('FR');
      expect(component['form']().marketingTextEn).toBe('EN');
    });

    it('closeModal resets modal state', () => {
      component['openCreateModal']();
      component['closeModal']();
      expect(component['modalOpen']()).toBeFalse();
      expect(component['editingPromotion']()).toBeNull();
      expect(component['formError']()).toBeNull();
    });
  });

  // ── updateCarouselSettingsDraft ───────────────────────────────────────────

  describe('updateCarouselSettingsDraft', () => {
    it('updates a single field without touching the rest', () => {
      component['carouselSettingsDraft'].set({ fixedTextFr: 'original', fixedTextEn: 'EN', maxSlides: 5 });
      component['updateCarouselSettingsDraft']('fixedTextFr', 'updated');
      expect(component['carouselSettingsDraft']().fixedTextFr).toBe('updated');
      expect(component['carouselSettingsDraft']().fixedTextEn).toBe('EN');
      expect(component['carouselSettingsDraft']().maxSlides).toBe(5);
    });

    it('can update maxSlides', () => {
      component['updateCarouselSettingsDraft']('maxSlides', 3);
      expect(component['carouselSettingsDraft']().maxSlides).toBe(3);
    });
  });

  // ── saveCarouselSettings ──────────────────────────────────────────────────

  describe('saveCarouselSettings', () => {
    it('calls promotionService.updateCarouselSettings with correct payload', fakeAsync(() => {
      promotionSpy.updateCarouselSettings.and.returnValue(of(undefined as void));
      component['carouselSettingsDraft'].set({
        fixedTextFr: 'Texte FR', fixedTextEn: 'EN text', maxSlides: 4
      });
      component['saveCarouselSettings']();
      tick();

      expect(promotionSpy.updateCarouselSettings).toHaveBeenCalledWith({
        translations: {
          fr: { fixedText: 'Texte FR' },
          en: { fixedText: 'EN text' },
        },
        maxSlides: 4,
      });
    }));

    it('updates carouselSettings signal on success', fakeAsync(() => {
      promotionSpy.updateCarouselSettings.and.returnValue(of(undefined as void));
      promotionSpy.getPromotions.and.returnValue(emptyResponse([]));
      promotionSpy.getCarouselSettings.and.returnValue(emptyResponse({ translations: {}, maxSlides: 5 }));
      categorySpy.getCategories.and.returnValue(emptyResponse([]));

      component['carouselSettingsDraft'].set({ fixedTextFr: 'New FR', fixedTextEn: 'New EN', maxSlides: 3 });
      component['saveCarouselSettings']();
      tick();

      expect(component['carouselSettings']().maxSlides).toBe(3);
      expect(component['carouselSettingsSaving']()).toBeFalse();
    }));
  });

  // ── addToCarousel guard ───────────────────────────────────────────────────

  describe('addToCarousel', () => {
    it('does not call the service when carousel is full', () => {
      component['carouselSettings'].set({ translations: {}, maxSlides: 1 });
      component['promotions'].set([
        makePromotion({ showInCarousel: true, carouselOrder: 1 }),
      ]);
      const promo = makePromotion({ showInCarousel: false });
      component['addToCarousel'](promo);

      expect(promotionSpy.updatePromotion).not.toHaveBeenCalled();
    });

    it('calls addToCarousel service with promotion id', fakeAsync(() => {
      promotionSpy.addToCarousel.and.returnValue(of(undefined as void));
      promotionSpy.getPromotions.and.returnValue(emptyResponse([]));
      promotionSpy.getCarouselSettings.and.returnValue(emptyResponse({ translations: {}, maxSlides: 5 }));
      categorySpy.getCategories.and.returnValue(emptyResponse([]));

      component['carouselSettings'].set({ translations: {}, maxSlides: 5 });
      component['promotions'].set([]);

      const promo = makePromotion({ showInCarousel: false });
      component['addToCarousel'](promo);
      tick();

      expect(promotionSpy.addToCarousel).toHaveBeenCalledWith(promo.id);
    }));
  });
});
