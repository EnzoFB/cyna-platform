import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { provideTranslateService, TranslateService } from '@ngx-translate/core';
import { CartService } from '../../core/services/cart.service';
import { ProductDetail } from '../catalog/models/product.model';
import { CartComponent } from './cart.component';

describe('CartComponent', () => {
  let component: CartComponent;
  let fixture: ComponentFixture<CartComponent>;
  let cartService: CartService;
  let translate: TranslateService;

  beforeEach(async () => {
    localStorage.removeItem('cyna_pwa_cart');
    localStorage.removeItem('cyna_pwa_guest_token');

    await TestBed.configureTestingModule({
      imports: [CartComponent],
      providers: [provideRouter([]), provideHttpClient(), provideTranslateService()]
    }).compileComponents();

    fixture = TestBed.createComponent(CartComponent);
    component = fixture.componentInstance;
    cartService = TestBed.inject(CartService);
    translate = TestBed.inject(TranslateService);
    translate.setTranslation('fr', { cartPage: { empty: { title: 'Votre panier est vide' } } }, true);
    void translate.use('fr');
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show empty state when cart has no line', () => {
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Votre panier est vide');
  });

  it('should show added product line', () => {
    const product: ProductDetail = {
      id: 'soc-standard',
      translations: {
        fr: {
          name: 'SOC Standard',
          serviceDescription: 'desc',
          technicalDescription: 'tech',
          highlightPoints: [],
        }
      },
      categoryId: '00000000-0000-0000-0000-000000000001',
      categoryName: 'SOC',
      priorityLevel: 1,
      monthlyPrice: 300,
      annualPrice: 270,
      currency: 'EUR',
      primaryImageBase64: null,
      isPublished: true,
      isAvailable: true,
      freeTrialDays: 0,
      images: []
    };
    cartService.addProduct(product, 'MONTHLY', 1);

    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('SOC Standard');
  });
});
