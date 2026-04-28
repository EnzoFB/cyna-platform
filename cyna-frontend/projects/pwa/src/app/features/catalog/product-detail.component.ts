import { CurrencyPipe, NgOptimizedImage, UpperCasePipe } from '@angular/common';
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
import { catchError, distinctUntilChanged, map, of, switchMap } from 'rxjs';
import { ToastService } from '../../core/services/toast.service';
import { CartService, CartBillingCycle } from '../../core/services/cart.service';
import { ProductCardComponent } from './components/product-card/product-card.component';
import { Product, ProductDetail } from './models/product.model';
import { CatalogService } from './services/catalog.service';
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [CurrencyPipe, NgOptimizedImage, ProductCardComponent, RouterLink, TranslatePipe, UpperCasePipe],
  templateUrl: './product-detail.component.html',
  styleUrl: './product-detail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProductDetailComponent {
  readonly isLoading = signal(true);
  readonly product = signal<ProductDetail | null>(null);
  readonly similarProducts = signal<readonly Product[]>([]);
  readonly similarStartIndex = signal(0);
  readonly annualBillingEnabled = signal(false);
  readonly currentImageIndex = signal(0);
  readonly isImageAnimating = signal(false);

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

  readonly hasCarousel = computed(() => (this.product()?.imageUrls.length ?? 0) > 1);
  readonly hasSimilarCarousel = computed(() => this.similarProducts().length > 4);
  readonly visibleSimilarProducts = computed(() =>
    this.similarProducts().slice(this.similarStartIndex(), this.similarStartIndex() + 4)
  );

  readonly displayedImageUrl = computed(() => {
    const currentProduct = this.product();
    if (!currentProduct || currentProduct.imageUrls.length === 0) {
      return null;
    }
    const safeIndex = Math.max(0, Math.min(this.currentImageIndex(), currentProduct.imageUrls.length - 1));
    return currentProduct.imageUrls[safeIndex];
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
          this.currentImageIndex.set(0);
          this.similarStartIndex.set(0);

          return this.catalogService.getProductById(productId).pipe(
            switchMap(product => {
              if (!product) {
                return of({ product: null, similarProducts: [] as readonly Product[] });
              }

              return this.catalogService.getProductPage({
                page: 0,
                size: 100,
                categoryId: product.categoryId,
                sort: 'priority,desc',
                published: true
              }).pipe(
                map(page => ({
                  product,
                  similarProducts: page.items.filter(item => item.id !== product.id)
                }))
              );
            }),
            catchError(() => of({ product: null, similarProducts: [] as readonly Product[] }))
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

  previousImage(): void {
    const total = this.product()?.imageUrls.length ?? 0;
    if (total <= 1) {
      return;
    }
    this.currentImageIndex.update(index => (index - 1 + total) % total);
    this.triggerImageAnimation();
  }

  nextImage(): void {
    const total = this.product()?.imageUrls.length ?? 0;
    if (total <= 1) {
      return;
    }
    this.currentImageIndex.update(index => (index + 1) % total);
    this.triggerImageAnimation();
  }

  previousSimilar(): void {
    if (!this.hasSimilarCarousel()) {
      return;
    }

    this.similarStartIndex.update(index => Math.max(0, index - 1));
  }

  nextSimilar(): void {
    if (!this.hasSimilarCarousel()) {
      return;
    }

    const maxStart = Math.max(0, this.similarProducts().length - 4);
    this.similarStartIndex.update(index => Math.min(maxStart, index + 1));
  }

  private triggerImageAnimation(): void {
    this.isImageAnimating.set(false);
    queueMicrotask(() => this.isImageAnimating.set(true));
    setTimeout(() => this.isImageAnimating.set(false), 280);
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
