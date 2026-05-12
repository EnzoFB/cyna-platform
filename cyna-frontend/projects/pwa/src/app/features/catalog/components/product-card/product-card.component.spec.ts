import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { ProductCardComponent } from './product-card.component';

describe('ProductCardComponent', () => {
  let component: ProductCardComponent;
  let fixture: ComponentFixture<ProductCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProductCardComponent],
      providers: [provideTranslateService()]
    }).compileComponents();

    fixture = TestBed.createComponent(ProductCardComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('product', {
      id: '1',
      name: 'SOC Standard',
      categoryId: '00000000-0000-0000-0000-000000000001',
      categoryName: 'SOC',
      priorityLevel: 1,
      monthlyPrice: 300,
      annualPrice: 3000,
      currency: 'EUR',
      primaryImageBase64: null,
      isPublished: true,
      isAvailable: true
    });
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
