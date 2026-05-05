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
  readonly currency: string;
  readonly billingCycle: CartBillingCycle;
  readonly quantity: number;
  readonly available: boolean;
}

const CART_STORAGE_KEY = 'cyna_pwa_cart';
const GUEST_TOKEN_STORAGE_KEY = 'cyna_pwa_guest_token';
const MAX_LINE_QUANTITY = 99;
const MIN_LINE_QUANTITY = 1;
const VAT_RATE = 0.20;

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly _items = signal<readonly CartItem[]>(this.loadItems());

  readonly items = this._items.asReadonly();
  readonly totalItems = computed(() => this._items().reduce((sum, item) => sum + item.quantity, 0));
  readonly subtotalHt = computed(() => this.round2(
    this._items().reduce((sum, item) => sum + this.getUnitPrice(item) * item.quantity, 0)
  ));
  readonly vatAmount = computed(() => this.round2(this.subtotalHt() * VAT_RATE));
  readonly totalTtc = computed(() => this.round2(this.subtotalHt() + this.vatAmount()));
  readonly currency = computed(() => this._items().at(0)?.currency ?? 'EUR');
  readonly isEmpty = computed(() => this._items().length === 0);
  readonly hasUnavailableItems = computed(() => this._items().some(item => !item.available));
  readonly hasMixedBillingCycles = computed(() => {
    const items = this._items();
    if (items.length < 2) return false;
    const firstCycle = items[0].billingCycle;
    return items.some(item => item.billingCycle !== firstCycle);
  });
  readonly checkoutAllowed = computed(() =>
    !this.isEmpty() && !this.hasUnavailableItems() && !this.hasMixedBillingCycles()
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
      productName: product.name,
      productCategory: product.categoryName,
      monthlyPrice: product.monthlyPrice,
      annualPrice: product.annualPrice,
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
