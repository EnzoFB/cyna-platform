import { CurrencyPipe, UpperCasePipe } from '@angular/common';
import { AfterViewInit, Component, computed, DestroyRef, effect, ElementRef, HostListener, inject, OnDestroy, signal, ViewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { Subject, debounceTime } from 'rxjs';
import { CartBillingCycle, CartItem, CartMutationResult, CartService } from '../../core/services/cart.service';
import { ToastService } from '../../core/services/toast.service';
import { PaymentService, TaxPreviewResponse } from '../../core/services/payment.service';
import {OrderSummaryComponent} from "../../shared/components/order-summary/order-summary.component";

@Component({
  selector: 'app-cart',
  imports: [CurrencyPipe, RouterLink, TranslatePipe, UpperCasePipe, OrderSummaryComponent],
  templateUrl: './cart.component.html',
  styleUrl: './cart.component.scss',
})
export class CartComponent implements AfterViewInit, OnDestroy {
  private readonly cartService = inject(CartService);
  private readonly toastService = inject(ToastService);
  private readonly translate = inject(TranslateService);
  private readonly router = inject(Router);
  private readonly paymentService = inject(PaymentService);
  private readonly destroyRef = inject(DestroyRef);

  /**
   * Cart VAT is always previewed against the French rate. Cyna is a French
   * company, so French VAT is the sensible default for the in-cart estimate;
   * the legally-binding VAT is recomputed at checkout from the real billing
   * address (incl. B2B reverse charge). A note in the summary makes this clear.
   */
  private static readonly DEFAULT_VAT_COUNTRY = 'FR';

  readonly items = this.cartService.items;
  readonly totalItems = this.cartService.totalItems;
  readonly subtotalHt = this.cartService.subtotalHt;
  readonly currency = this.cartService.currency;
  readonly isEmpty = this.cartService.isEmpty;
  readonly hasUnavailableItems = this.cartService.hasUnavailableItems;
  readonly hasMixedBillingCycles = this.cartService.hasMixedBillingCycles;

  readonly checkoutDisabled = computed(() => !this.cartService.checkoutAllowed());

  // Estimated VAT/TTC from Stripe Tax for the current cart at the French rate.
  // Null while unknown (Stripe Tax off or in flight).
  private readonly taxPreview = signal<TaxPreviewResponse | null>(null);
  readonly taxLoading = signal(false);
  private readonly taxPreviewTrigger = new Subject<void>();

  readonly openCycle = signal<string | null>(null);

  /** true quand le récapitulatif est visible dans le viewport (IntersectionObserver) */
  readonly summaryVisible = signal(true);
  private observer?: IntersectionObserver;

  @ViewChild('orderSummaryAnchor', { read: ElementRef })
  private orderSummaryAnchor?: ElementRef<HTMLElement>;

  constructor() {
    // Debounce preview requests so rapid quantity/cycle changes hit Stripe once.
    this.taxPreviewTrigger
      .pipe(debounceTime(400), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.runTaxPreview());

    // Recompute the preview whenever the cart changes. Marks loading
    // immediately so the summary shows "calculating…".
    effect(() => {
      this.cartService.items();
      if (!this.cartService.isEmpty()) {
        this.taxLoading.set(true);
        this.taxPreviewTrigger.next();
      } else {
        this.taxLoading.set(false);
        this.taxPreview.set(null);
      }
    });
  }

  private runTaxPreview(): void {
    const items = this.cartService.items();
    if (items.length === 0) {
      this.taxPreview.set(null);
      this.taxLoading.set(false);
      return;
    }

    this.paymentService.previewTax({
      currency: this.cartService.currency(),
      lines: items.map(i => ({
        productId: i.productId,
        billingCycle: i.billingCycle,
        quantity: i.quantity,
      })),
      countryCode: CartComponent.DEFAULT_VAT_COUNTRY,
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        // Only trust an "exact" Stripe Tax result; otherwise stay HT-only.
        next: preview => {
          this.taxPreview.set(preview.exact ? preview : null);
          this.taxLoading.set(false);
        },
        error: () => {
          this.taxPreview.set(null);
          this.taxLoading.set(false);
        },
      });
  }

  ngAfterViewInit(): void {
    if (!this.orderSummaryAnchor) return;
    this.observer = new IntersectionObserver(
      ([entry]) => this.summaryVisible.set(entry.isIntersecting),
      { threshold: 0 }
    );
    this.observer.observe(this.orderSummaryAnchor.nativeElement);
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }

  @HostListener('document:click')
  onDocumentClick(): void {
    this.openCycle.set(null);
  }

  toggleCycleDropdown(lineId: string, event: Event): void {
    event.stopPropagation();
    this.openCycle.update(current => current === lineId ? null : lineId);
  }

  selectBillingCycle(item: CartItem, value: string): void {
    this.openCycle.set(null);
    this.changeBillingCycle(item, value);
  }

  decreaseQuantity(item: CartItem): void {
    const result = this.cartService.decrementQuantity(item.lineId);
    this.handleMutationResult(result);
  }

  increaseQuantity(item: CartItem): void {
    const result = this.cartService.incrementQuantity(item.lineId);
    this.handleMutationResult(result);
  }

  changeBillingCycle(item: CartItem, value: string): void {
    const nextCycle: CartBillingCycle = value === 'ANNUAL' ? 'ANNUAL' : 'MONTHLY';
    const result = this.cartService.changeBillingCycle(item.lineId, nextCycle);
    this.handleMutationResult(result);
  }

  removeItem(item: CartItem): void {
    this.cartService.removeItem(item.lineId);
  }

  getLineUnitPrice(item: CartItem): number {
    return this.cartService.getUnitPrice(item);
  }

  getOriginalLineUnitPrice(item: CartItem): number | null {
    return this.cartService.getOriginalUnitPrice(item);
  }

  hasPromotion(item: CartItem): boolean {
    return this.getOriginalLineUnitPrice(item) !== null;
  }

  getLineTotal(item: CartItem): number {
    return this.cartService.getLineTotal(item);
  }

  getPeriodKey(item: CartItem): string {
    return item.billingCycle === 'ANNUAL' ? 'catalog.year' : 'catalog.month';
  }

  checkout(): void {
    if (!this.cartService.checkoutAllowed()) {
      this.toastService.showError(this.translate.instant('cartPage.checkoutBlocked'));
      return;
    }

    void this.router.navigate(['/checkout']);
  }

  readonly summary = computed(() => {
    const items = this.items().map(item => ({
      label: item.productName,
      quantity: item.quantity,
      billingCycle: item.billingCycle,
      total: this.cartService.getLineTotal(item)
    }));

    const preview = this.taxPreview();
    // Exact Stripe Tax result for the signed-in user's default address → show
    // TTC. Otherwise only the HT subtotal (guest, no address, or Stripe Tax off).
    if (preview?.exact && preview.vatAmount != null && preview.totalTtc != null) {
      return {
        items,
        subtotalHt: preview.subtotalHt ?? this.subtotalHt(),
        vatAmount: preview.vatAmount,
        totalTtc: preview.totalTtc,
        currency: preview.currency ?? this.currency(),
        reverseCharge: preview.reverseCharge,
      };
    }

    return {
      items,
      subtotalHt: this.subtotalHt(),
      currency: this.currency()
    };
  });

  // Grand total shown in the floating sticky bar: TTC when known, else HT.
  readonly stickyTotal = computed(() => {
    const s = this.summary();
    return s.totalTtc ?? s.subtotalHt;
  });

  private handleMutationResult(result: CartMutationResult): void {
    if (result === 'ok') {
      return;
    }
    if (result === 'min-reached') {
      this.toastService.showError(this.translate.instant('cartPage.minQuantityReached'));
      return;
    }
    if (result === 'max-reached') {
      this.toastService.showError(this.translate.instant('cartPage.maxQuantityReached'));
      return;
    }
    this.toastService.showError(this.translate.instant('cartPage.lineNotFound'));
  }
}
