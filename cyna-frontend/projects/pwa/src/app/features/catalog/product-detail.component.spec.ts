import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { ProductDetailComponent } from './product-detail.component';
import { ProductDetail } from './models/product.model';

describe('ProductDetailComponent', () => {
  let component: ProductDetailComponent;
  let fixture: ComponentFixture<ProductDetailComponent>;

  const imageA = { id: 'img-a', base64: 'iVBORtest==' };  // PNG
  const imageB = { id: 'img-b', base64: '/9j/jpeg==' };    // JPEG
  const imageC = { id: 'img-c', base64: 'PHN2svg==' };     // SVG

  const productWithImages = (images: { id: string; base64: string }[]): ProductDetail => ({
    id: 'prod-1',
    translations: {
      fr: { name: 'EDR Pro', serviceDescription: 'Desc', technicalDescription: 'Tech', highlightPoints: [] }
    },
    categoryId: 'cat-1',
    categoryName: 'EDR',
    priorityLevel: 1,
    monthlyPrice: 99,
    annualPrice: 990,
    currency: 'EUR',
    primaryImageBase64: null,
    isPublished: true,
    isAvailable: true,
    freeTrialDays: 0,
    images
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProductDetailComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTranslateService()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProductDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ── Carousel navigation ─────────────────────────────────────────────────────

  describe('hasCarousel', () => {
    it('returns false when product has 0 or 1 image', () => {
      component['product'].set(productWithImages([]));
      expect(component['hasCarousel']()).toBeFalse();

      component['product'].set(productWithImages([imageA]));
      expect(component['hasCarousel']()).toBeFalse();
    });

    it('returns true when product has more than 1 image', () => {
      component['product'].set(productWithImages([imageA, imageB]));
      expect(component['hasCarousel']()).toBeTrue();
    });
  });

  describe('nextImage / previousImage', () => {
    beforeEach(() => {
      component['product'].set(productWithImages([imageA, imageB, imageC]));
      component['currentImageIndex'].set(0);
    });

    it('nextImage advances the index', () => {
      component['nextImage']();
      expect(component['currentImageIndex']()).toBe(1);
    });

    it('nextImage wraps around to 0 from the last image', () => {
      component['currentImageIndex'].set(2);
      component['nextImage']();
      expect(component['currentImageIndex']()).toBe(0);
    });

    it('previousImage goes back one step', () => {
      component['currentImageIndex'].set(1);
      component['previousImage']();
      expect(component['currentImageIndex']()).toBe(0);
    });

    it('previousImage wraps around to last from index 0', () => {
      component['currentImageIndex'].set(0);
      component['previousImage']();
      expect(component['currentImageIndex']()).toBe(2);
    });

    it('nextImage does nothing when product has only 1 image', () => {
      component['product'].set(productWithImages([imageA]));
      component['currentImageIndex'].set(0);
      component['nextImage']();
      expect(component['currentImageIndex']()).toBe(0);
    });
  });

  describe('goToImage', () => {
    beforeEach(() => {
      component['product'].set(productWithImages([imageA, imageB, imageC]));
      component['currentImageIndex'].set(0);
    });

    it('jumps to the specified index', () => {
      component['goToImage'](2);
      expect(component['currentImageIndex']()).toBe(2);
    });

    it('does nothing when index is already current', () => {
      component['currentImageIndex'].set(1);
      component['goToImage'](1);
      expect(component['currentImageIndex']()).toBe(1);
    });

    it('does nothing when index is out of range', () => {
      component['goToImage'](-1);
      expect(component['currentImageIndex']()).toBe(0);

      component['goToImage'](99);
      expect(component['currentImageIndex']()).toBe(0);
    });
  });

  // ── MIME type detection via displayedImageSrc ────────────────────────────────

  describe('displayedImageSrc', () => {
    it('detects PNG from iVBOR prefix', () => {
      component['product'].set(productWithImages([{ id: 'p', base64: 'iVBORtest==' }]));
      component['currentImageIndex'].set(0);
      expect(component['displayedImageSrc']()).toContain('data:image/png;base64,');
    });

    it('detects SVG from PHN2 prefix', () => {
      component['product'].set(productWithImages([{ id: 's', base64: 'PHN2svg==' }]));
      expect(component['displayedImageSrc']()).toContain('data:image/svg+xml;base64,');
    });

    it('detects SVG from PD94 prefix', () => {
      component['product'].set(productWithImages([{ id: 's2', base64: 'PD94svg==' }]));
      expect(component['displayedImageSrc']()).toContain('data:image/svg+xml;base64,');
    });

    it('defaults to JPEG for unknown prefix', () => {
      component['product'].set(productWithImages([{ id: 'j', base64: '/9j/jpeg==' }]));
      expect(component['displayedImageSrc']()).toContain('data:image/jpeg;base64,');
    });

    it('returns null when product has no images', () => {
      component['product'].set(productWithImages([]));
      expect(component['displayedImageSrc']()).toBeNull();
    });

    it('returns null when no product is loaded', () => {
      component['product'].set(null);
      expect(component['displayedImageSrc']()).toBeNull();
    });
  });

  // ── Lightbox ─────────────────────────────────────────────────────────────────

  describe('lightbox', () => {
    it('is closed by default', () => {
      expect(component['lightboxOpen']()).toBeFalse();
    });

    it('openLightbox sets lightboxOpen to true when image exists', () => {
      component['product'].set(productWithImages([imageA]));
      component['openLightbox']();
      expect(component['lightboxOpen']()).toBeTrue();
    });

    it('openLightbox does nothing when there is no image', () => {
      component['product'].set(productWithImages([]));
      component['openLightbox']();
      expect(component['lightboxOpen']()).toBeFalse();
    });

    it('closeLightbox sets lightboxOpen to false', () => {
      component['product'].set(productWithImages([imageA]));
      component['openLightbox']();
      component['closeLightbox']();
      expect(component['lightboxOpen']()).toBeFalse();
    });

    it('onEscape closes the lightbox', () => {
      component['product'].set(productWithImages([imageA]));
      component['openLightbox']();
      component['onEscape']();
      expect(component['lightboxOpen']()).toBeFalse();
    });
  });
});
