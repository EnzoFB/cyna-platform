import { CurrencyPipe, UpperCasePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed, DestroyRef,
  HostListener,
  inject,
  signal
} from '@angular/core';
import {ActivatedRoute, RouterLink} from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { catchError, distinctUntilChanged, map, of, switchMap } from 'rxjs';
import { ToastService } from '../../core/services/toast.service';
import { AuthService } from '../../core/services/auth.service';
import { SubscriptionService } from '../../core/services/subscription.service';
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
  private readonly translate = inject(TranslateService);

  readonly isLoading = signal(true);
  readonly product = signal<ProductDetail | null>(null);
  readonly similarProducts = signal<readonly Product[]>([]);
  readonly similarStartIndex = signal(0);
  readonly annualBillingEnabled = signal(false);
  readonly currentImageIndex = signal(0);
  readonly isImageAnimating = signal(false);
  readonly lightboxOpen = signal(false);

  // ── Language tracking ────────────────────────────────────────────────────────
  private readonly lang = signal(this.translate.currentLang ?? 'fr');

  readonly localizedName = computed(() => {
    const p = this.product();
    const lang = this.lang();
    return p?.translations[lang]?.name ?? p?.translations['fr']?.name ?? '';
  });

  readonly localizedServiceDescription = computed(() => {
    const p = this.product();
    const lang = this.lang();
    return p?.translations[lang]?.serviceDescription ?? p?.translations['fr']?.serviceDescription ?? '';
  });

  readonly localizedTechnicalDescription = computed(() => {
    const p = this.product();
    const lang = this.lang();
    return p?.translations[lang]?.technicalDescription ?? p?.translations['fr']?.technicalDescription ?? '';
  });

  readonly localizedHighlightPoints = computed((): readonly string[] => {
    const p = this.product();
    if (!p) return [];
    const lang = this.lang();
    return p.translations[lang]?.highlightPoints ?? p.translations['fr']?.highlightPoints ?? [];
  });

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

  readonly originalDisplayedPrice = computed(() => {
    const currentProduct = this.product();
    if (!currentProduct) {
      return null;
    }

    const useAnnualPrice =
      this.annualBillingEnabled() && (currentProduct?.annualPrice ?? 0) > 0;
    const original = useAnnualPrice ? currentProduct.originalAnnualPrice : currentProduct.originalMonthlyPrice;

    if (typeof original !== 'number' || original <= this.displayedMonthlyPrice()) {
      return null;
    }
    return original;
  });

  readonly billingPeriodKey = computed(() => {
    const currentProduct = this.product();
    const useAnnualPrice =
      this.annualBillingEnabled() && (currentProduct?.annualPrice ?? 0) > 0;

    return useAnnualPrice ? 'catalog.year' : 'catalog.month';
  });

  readonly hasDisplayedPromotion = computed(() => this.originalDisplayedPrice() !== null);

  readonly isAvailable = computed(() => this.product()?.isAvailable ?? true);

  // Free-trial length advertised on the product. > 0 turns the CTA into a
  // "try free" action and surfaces the trial badge.
  readonly freeTrialDays = computed(() => this.product()?.freeTrialDays ?? 0);
  // Whether THIS user can still claim the trial. Defaults true (anonymous
  // visitors, or before the check resolves); set to false once the server says
  // the authenticated user already subscribed to this product — same rule the
  // checkout enforces, so the CTA never promises a trial that would be refused.
  private readonly _trialEligible = signal(true);
  readonly trialEligible = this._trialEligible.asReadonly();
  readonly hasFreeTrial = computed(() =>
    this.isAvailable() && this.freeTrialDays() > 0 && this.trialEligible());

  readonly hasCarousel = computed(() => (this.product()?.images.length ?? 0) > 1);

  readonly sortedSimilarProducts = computed(() =>
    [...this.similarProducts()].sort((a, b) => {
      if (a.isAvailable === b.isAvailable) return 0;
      return a.isAvailable ? -1 : 1;
    })
  );

  readonly hasSimilarCarousel = computed(() => this.sortedSimilarProducts().length > 4);
  readonly visibleSimilarProducts = computed(() =>
    this.sortedSimilarProducts().slice(this.similarStartIndex(), this.similarStartIndex() + 4)
  );

  readonly displayedImageSrc = computed(() => {
    const currentProduct = this.product();
    if (!currentProduct || currentProduct.images.length === 0) {
      return null;
    }
    const safeIndex = Math.max(0, Math.min(this.currentImageIndex(), currentProduct.images.length - 1));
    const b64 = currentProduct.images[safeIndex].base64;
    return `data:${this.detectMimeType(b64)};base64,${b64}`;
  });

  private detectMimeType(base64: string): string {
    if (base64.startsWith('iVBOR')) return 'image/png';
    if (base64.startsWith('PHN2') || base64.startsWith('PD94')) return 'image/svg+xml';
    return 'image/jpeg';
  }

  private readonly route = inject(ActivatedRoute);
  private readonly catalogService = inject(CatalogService);
  private readonly cartService = inject(CartService);
  private readonly toastService = inject(ToastService);
  private readonly authService = inject(AuthService);
  private readonly subscriptionService = inject(SubscriptionService);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    this.translate.onLangChange
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(e => this.lang.set(e.lang));

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
          this._trialEligible.set(true);

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
        this.refreshTrialEligibility(product);
      });
  }

  // For an authenticated user on a product that advertises a trial, confirm with
  // the server whether they're still entitled to it (false once they've ever
  // subscribed). Anonymous visitors keep the optimistic default — the checkout
  // re-checks server-side regardless. A failed check leaves the optimistic value.
  private refreshTrialEligibility(product: ProductDetail | null): void {
    if (!product || product.freeTrialDays <= 0 || !this.authService.isAuthenticated()) {
      return;
    }
    this.subscriptionService.isTrialEligible(product.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: eligible => this._trialEligible.set(eligible),
        error: () => { /* keep optimistic default; checkout enforces the rule */ }
      });
  }

  toggleAnnualBilling(): void {
    this.annualBillingEnabled.update(value => !value);
  }

  previousImage(): void {
    const total = this.product()?.images.length ?? 0;
    if (total <= 1) {
      return;
    }
    this.currentImageIndex.update(index => (index - 1 + total) % total);
    this.triggerImageAnimation();
  }

  goToImage(index: number): void {
    const total = this.product()?.images.length ?? 0;
    if (index < 0 || index >= total || index === this.currentImageIndex()) return;
    this.currentImageIndex.set(index);
    this.triggerImageAnimation();
  }

  nextImage(): void {
    const total = this.product()?.images.length ?? 0;
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

  openLightbox(): void {
    if (this.displayedImageSrc()) {
      this.lightboxOpen.set(true);
    }
  }

  closeLightbox(): void {
    this.lightboxOpen.set(false);
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.closeLightbox();
  }

  addCurrentProductToCart(): void {
    const currentProduct = this.product();
    if (!currentProduct || !currentProduct.isAvailable) {
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
