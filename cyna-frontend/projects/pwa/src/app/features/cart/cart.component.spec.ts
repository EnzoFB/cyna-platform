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
      name: 'SOC Standard',
      imageUrl: null,
      category: 'soc',
      monthlyPrice: 300,
      annualMonthlyPrice: 270,
      annualBillingAvailable: true,
      availableImmediately: true,
      currency: 'EUR',
      description: 'desc',
      technicalDescription: 'tech',
      highlightPoints: []
    };
    cartService.addProduct(product, 'MONTHLY', 1);

    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('SOC Standard');
  });
});
