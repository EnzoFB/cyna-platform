import { CurrencyPipe, UpperCasePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { distinctUntilChanged, map, of, switchMap } from 'rxjs';
import { ProductCardComponent } from './components/product-card/product-card.component';
import { Product, ProductDetail } from './models/product.model';
import { CatalogService } from './services/catalog.service';

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

    if (this.annualBillingEnabled() && currentProduct.annualBillingAvailable) {
      return currentProduct.annualMonthlyPrice;
    }

    return currentProduct.monthlyPrice;
  });

  readonly billingPeriodKey = computed(() => {
    const currentProduct = this.product();
    const useAnnualPrice =
      this.annualBillingEnabled() && Boolean(currentProduct?.annualBillingAvailable);

    return useAnnualPrice ? 'catalog.year' : 'catalog.month';
  });

  private readonly route = inject(ActivatedRoute);
  private readonly catalogService = inject(CatalogService);
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

              return this.catalogService
                .getSimilarProducts(product.id, product.category)
                .pipe(map(similarProducts => ({ product, similarProducts })));
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
}
