import { Injectable, computed, signal } from '@angular/core';
import { ProductDetail } from '../../features/catalog/models/product.model';

export type CartBillingCycle = 'MONTHLY' | 'ANNUAL';
export type CartMutationResult = 'ok' | 'min-reached' | 'max-reached' | 'not-found';
export type AddToCartResult = Extract<CartMutationResult, 'ok' | 'max-reached'>;

export interface CartItem {
  readonly lineId: string;
  readonly productId: string;
  readonly productName: string;
  readonly productCategory: string;
  readonly monthlyPrice: number;
  readonly annualPrice: number;
  readonly originalMonthlyPrice: number | null;
  readonly originalAnnualPrice: number | null;
  readonly promotionDiscountPercent: number | null;
  readonly currency: string;
  readonly billingCycle: CartBillingCycle;
  readonly quantity: number;
  readonly available: boolean;
}

const CART_STORAGE_KEY = 'cyna_pwa_cart';
const GUEST_TOKEN_STORAGE_KEY = 'cyna_pwa_guest_token';
const MAX_LINE_QUANTITY = 99;
const MIN_LINE_QUANTITY = 1;

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly _items = signal<readonly CartItem[]>(this.loadItems());

  readonly items = this._items.asReadonly();
  readonly totalItems = computed(() => this._items().reduce((sum, item) => sum + item.quantity, 0));
  // VAT is intentionally not computed here — Stripe Tax determines it at
  // checkout based on the billing country and B2B reverse-charge status.
  // The cart exposes HT amounts only; the checkout page upgrades to TTC
  // once the customer provides their billing address.
  readonly subtotalHt = computed(() => this.round2(
    this._items().reduce((sum, item) => sum + this.getUnitPrice(item) * item.quantity, 0)
  ));

  // Per-cycle HT subtotals, used by the checkout recurring-charge notice when
  // the cart mixes MONTHLY and ANNUAL lines. TVA is added on top by the
  // tax-preview returned from Stripe Tax at checkout.
  readonly monthlyTotalHt = computed(() => this.cycleTotalHt('MONTHLY'));
  readonly annualTotalHt = computed(() => this.cycleTotalHt('ANNUAL'));

  // 'MONTHLY' | 'ANNUAL' | 'MIXED' — drives the wording of the checkout notice
  // and the submit button label. Empty carts default to 'MONTHLY' (no impact
  // since the checkout button is disabled in that case).
  readonly cartCycleMode = computed<'MONTHLY' | 'ANNUAL' | 'MIXED'>(() => {
    const items = this._items();
    if (items.length === 0) return 'MONTHLY';
    const cycles = new Set(items.map(i => i.billingCycle));
    if (cycles.size > 1) return 'MIXED';
    return cycles.has('ANNUAL') ? 'ANNUAL' : 'MONTHLY';
  });
  readonly currency = computed(() => this._items().at(0)?.currency ?? 'EUR');
  readonly isEmpty = computed(() => this._items().length === 0);
  readonly hasUnavailableItems = computed(() => this._items().some(item => !item.available));
  // Mixed cycles (MONTHLY + ANNUAL in the same cart) are supported since the V14
  // backend checkout — each line becomes its own Stripe Subscription with its own
  // cycle. The flag is kept as a read-only signal in case the UI ever wants to
  // surface a hint, but it no longer gates the checkout action.
  readonly hasMixedBillingCycles = computed(() => {
    const items = this._items();
    if (items.length < 2) return false;
    const firstCycle = items[0].billingCycle;
    return items.some(item => item.billingCycle !== firstCycle);
  });
  readonly checkoutAllowed = computed(() =>
    !this.isEmpty() && !this.hasUnavailableItems()
  );

  addProduct(product: ProductDetail, billingCycle: CartBillingCycle, quantity = 1): AddToCartResult {
    if (quantity < MIN_LINE_QUANTITY) {
      return 'max-reached';
    }

    const currentItems = [...this._items()];
    const existingIndex = currentItems.findIndex(
      item => item.productId === product.id && item.billingCycle === billingCycle
    );

    if (existingIndex >= 0) {
      const existing = currentItems[existingIndex];
      const nextQuantity = existing.quantity + quantity;
      if (nextQuantity > MAX_LINE_QUANTITY) {
        return 'max-reached';
      }
      currentItems[existingIndex] = { ...existing, quantity: nextQuantity };
      this.persistItems(currentItems);
      return 'ok';
    }

    currentItems.push({
      lineId: this.buildLineId(product.id, billingCycle),
      productId: product.id,
      productName: product.translations['fr']?.name ?? '',
      productCategory: product.categoryName,
      monthlyPrice: product.monthlyPrice,
      annualPrice: product.annualPrice,
      originalMonthlyPrice: product.originalMonthlyPrice ?? null,
      originalAnnualPrice: product.originalAnnualPrice ?? null,
      promotionDiscountPercent: product.promotionDiscountPercent ?? null,
      currency: product.currency,
      billingCycle,
      quantity,
      available: product.isAvailable
    });

    this.persistItems(currentItems);
    return 'ok';
  }

  incrementQuantity(lineId: string): CartMutationResult {
    const line = this._items().find(item => item.lineId === lineId);
    if (!line) {
      return 'not-found';
    }
    return this.updateQuantity(lineId, line.quantity + 1);
  }

  decrementQuantity(lineId: string): CartMutationResult {
    const line = this._items().find(item => item.lineId === lineId);
    if (!line) {
      return 'not-found';
    }
    return this.updateQuantity(lineId, line.quantity - 1);
  }

  updateQuantity(lineId: string, nextQuantity: number): CartMutationResult {
    if (nextQuantity < MIN_LINE_QUANTITY) {
      return 'min-reached';
    }
    if (nextQuantity > MAX_LINE_QUANTITY) {
      return 'max-reached';
    }

    const currentItems = [...this._items()];
    const index = currentItems.findIndex(item => item.lineId === lineId);
    if (index < 0) {
      return 'not-found';
    }

    currentItems[index] = {
      ...currentItems[index],
      quantity: nextQuantity
    };
    this.persistItems(currentItems);
    return 'ok';
  }

  changeBillingCycle(lineId: string, nextCycle: CartBillingCycle): CartMutationResult {
    const currentItems = [...this._items()];
    const sourceIndex = currentItems.findIndex(item => item.lineId === lineId);
    if (sourceIndex < 0) {
      return 'not-found';
    }

    const source = currentItems[sourceIndex];
    if (source.billingCycle === nextCycle) {
      return 'ok';
    }

    const targetIndex = currentItems.findIndex(
      item => item.productId === source.productId && item.billingCycle === nextCycle
    );

    if (targetIndex >= 0) {
      const target = currentItems[targetIndex];
      const mergedQuantity = target.quantity + source.quantity;
      if (mergedQuantity > MAX_LINE_QUANTITY) {
        return 'max-reached';
      }

      currentItems[targetIndex] = { ...target, quantity: mergedQuantity };
      currentItems.splice(sourceIndex, 1);
      this.persistItems(currentItems);
      return 'ok';
    }

    currentItems[sourceIndex] = {
      ...source,
      billingCycle: nextCycle,
      lineId: this.buildLineId(source.productId, nextCycle)
    };
    this.persistItems(currentItems);
    return 'ok';
  }

  removeItem(lineId: string): void {
    const filtered = this._items().filter(item => item.lineId !== lineId);
    this.persistItems(filtered);
  }

  clear(): void {
    this.persistItems([]);
  }

  getUnitPrice(item: CartItem): number {
    return item.billingCycle === 'ANNUAL' ? item.annualPrice : item.monthlyPrice;
  }

  getOriginalUnitPrice(item: CartItem): number | null {
    const originalPrice = item.billingCycle === 'ANNUAL'
      ? item.originalAnnualPrice
      : item.originalMonthlyPrice;

    if (typeof originalPrice !== 'number' || originalPrice <= this.getUnitPrice(item)) {
      return null;
    }
    return originalPrice;
  }

  private cycleTotalHt(cycle: CartBillingCycle): number {
    const ht = this._items()
      .filter(item => item.billingCycle === cycle)
      .reduce((sum, item) => sum + this.getUnitPrice(item) * item.quantity, 0);
    return this.round2(ht);
  }

  getLineTotal(item: CartItem): number {
    return this.round2(this.getUnitPrice(item) * item.quantity);
  }

  getOrCreateGuestToken(): string {
    const existing = localStorage.getItem(GUEST_TOKEN_STORAGE_KEY);
    if (existing && existing.trim().length > 0) {
      return existing;
    }

    const token = crypto.randomUUID();
    localStorage.setItem(GUEST_TOKEN_STORAGE_KEY, token);
    return token;
  }

  private loadItems(): readonly CartItem[] {
    try {
      const raw = localStorage.getItem(CART_STORAGE_KEY);
      if (!raw) {
        return [];
      }
      const parsed = JSON.parse(raw) as unknown;
      if (!Array.isArray(parsed)) {
        return [];
      }
      return parsed
        .map(item => this.normalizeItem(item))
        .filter((item): item is CartItem => item !== null);
    } catch {
      return [];
    }
  }

  private normalizeItem(rawItem: unknown): CartItem | null {
    if (!rawItem || typeof rawItem !== 'object') {
      return null;
    }
    const item = rawItem as Partial<CartItem>;
    const { productId, productName, productCategory, currency, billingCycle } = item;

    if (
      typeof productId !== 'string' || productId.trim().length === 0 ||
      typeof productName !== 'string' || productName.trim().length === 0 ||
      typeof productCategory !== 'string' || productCategory.trim().length === 0 ||
      typeof currency !== 'string' || currency.trim().length === 0
    ) {
      return null;
    }

    if (billingCycle !== 'MONTHLY' && billingCycle !== 'ANNUAL') {
      return null;
    }

    if (typeof item.monthlyPrice !== 'number' || !Number.isFinite(item.monthlyPrice)) {
      return null;
    }
    if (typeof item.annualPrice !== 'number' || !Number.isFinite(item.annualPrice)) {
      return null;
    }
    if (
      typeof item.quantity !== 'number' ||
      !Number.isInteger(item.quantity) ||
      item.quantity < MIN_LINE_QUANTITY ||
      item.quantity > MAX_LINE_QUANTITY
    ) {
      return null;
    }

    const lineId = typeof item.lineId === 'string' && item.lineId.trim().length > 0
      ? item.lineId
      : this.buildLineId(productId, billingCycle);

    return {
      lineId,
      productId,
      productName,
      productCategory,
      monthlyPrice: item.monthlyPrice,
      annualPrice: item.annualPrice,
      originalMonthlyPrice: typeof item.originalMonthlyPrice === 'number' ? item.originalMonthlyPrice : null,
      originalAnnualPrice: typeof item.originalAnnualPrice === 'number' ? item.originalAnnualPrice : null,
      promotionDiscountPercent: typeof item.promotionDiscountPercent === 'number'
        ? item.promotionDiscountPercent
        : null,
      currency,
      billingCycle,
      quantity: item.quantity,
      available: typeof item.available === 'boolean' ? item.available : true
    };
  }

  private persistItems(items: readonly CartItem[]): void {
    this._items.set(items);
    localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(items));
  }

  private buildLineId(productId: string, billingCycle: CartBillingCycle): string {
    return `${productId}::${billingCycle}`;
  }

  private round2(value: number): number {
    return Math.round(value * 100) / 100;
  }
}
