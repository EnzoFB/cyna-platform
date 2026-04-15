import { CurrencyPipe, UpperCasePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed, DestroyRef,
  // DestroyRef,
  inject,
  signal
} from '@angular/core';
// import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {ActivatedRoute, RouterLink} from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { distinctUntilChanged, map, of, switchMap } from 'rxjs';
import { ToastService } from '../../core/services/toast.service';
import { CartService, CartBillingCycle } from '../../core/services/cart.service';
import { ProductCardComponent } from './components/product-card/product-card.component';
import { Product, ProductDetail } from './models/product.model';
import { CatalogService } from './services/catalog.service';
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [CurrencyPipe, ProductCardComponent, RouterLink, TranslatePipe, UpperCasePipe],
  templateUrl: './product-detail.component.html',
  styleUrl: './product-detail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProductDetailComponent {
  readonly isLoading = signal(true);
  readonly product = signal<ProductDetail | null>(null);
  readonly similarProducts = signal<readonly Product[]>([]);
  readonly annualBillingEnabled = signal(false);

  readonly displayedMonthlyPrice = computed(() => {
    const currentProduct = this.product();
    if (!currentProduct) {
      return 0;
    }

    if (this.annualBillingEnabled() && currentProduct.annualPrice > 0) {
      return currentProduct.annualPrice;
    }

    return currentProduct.monthlyPrice;
  });

  readonly billingPeriodKey = computed(() => {
    const currentProduct = this.product();
    const useAnnualPrice =
      this.annualBillingEnabled() && (currentProduct?.annualPrice ?? 0) > 0;

    return useAnnualPrice ? 'catalog.year' : 'catalog.month';
  });

  private readonly route = inject(ActivatedRoute);
  private readonly catalogService = inject(CatalogService);
  private readonly cartService = inject(CartService);
  private readonly toastService = inject(ToastService);
  private readonly translate = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    this.route.paramMap
      .pipe(
        map(params => params.get('id') ?? ''),
        distinctUntilChanged(),
        switchMap(productId => {
          if (!productId) {
            return of({ product: null, similarProducts: [] as readonly Product[] });
          }

          this.isLoading.set(true);
          this.annualBillingEnabled.set(false);

          return this.catalogService.getProductById(productId).pipe(
            switchMap(product => {
              if (!product) {
                return of({ product: null, similarProducts: [] as readonly Product[] });
              }

              return of({product, similarProducts: [] as readonly Product[]})

              // return this.catalogService
              //   .getSimilarProducts(product.id, product.category)
              //   .pipe(map(similarProducts => ({ product, similarProducts })));
            })
          );
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(({ product, similarProducts }) => {
        this.product.set(product);
        this.similarProducts.set(similarProducts);
        this.isLoading.set(false);
      });
  }

  toggleAnnualBilling(): void {
    this.annualBillingEnabled.update(value => !value);
  }

  addCurrentProductToCart(): void {
    const currentProduct = this.product();
    if (!currentProduct) {
      return;
    }

    const billingCycle: CartBillingCycle =
      this.annualBillingEnabled() && currentProduct.annualPrice > 0 ? 'ANNUAL' : 'MONTHLY';

    const result = this.cartService.addProduct(currentProduct, billingCycle, 1);
    if (result === 'ok') {
      this.toastService.showSuccess(this.translate.instant('productDetail.pricing.addedToCart'));
      return;
    }

    this.toastService.showError(this.translate.instant('productDetail.pricing.maxQuantityReached'));
  }
}
