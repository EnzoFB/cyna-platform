import { Injectable, computed, signal } from '@angular/core';
import { ProductDetail } from '../../features/catalog/models/product.model';

export type CartBillingCycle = 'MONTHLY' | 'ANNUAL';

export interface CartItem {
  readonly productId: string;
  readonly productName: string;
  readonly productCategory: string;
  readonly monthlyPrice: number;
  readonly annualMonthlyPrice: number;
  readonly currency: string;
  readonly billingCycle: CartBillingCycle;
  readonly quantity: number;
}

type AddToCartResult = 'ok' | 'max-reached';

const CART_STORAGE_KEY = 'cyna_pwa_cart';
const GUEST_TOKEN_STORAGE_KEY = 'cyna_pwa_guest_token';
const MAX_LINE_QUANTITY = 99;

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly _items = signal<readonly CartItem[]>(this.loadItems());

  readonly items = this._items.asReadonly();
  readonly totalItems = computed(() => this._items().reduce((sum, item) => sum + item.quantity, 0));

  addProduct(product: ProductDetail, billingCycle: CartBillingCycle, quantity = 1): AddToCartResult {
    if (quantity <= 0) {
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
      currentItems[existingIndex] = {
        ...existing,
        quantity: nextQuantity
      };
    } else {
      currentItems.push({
        productId: product.id,
        productName: product.name,
        productCategory: product.category,
        monthlyPrice: product.monthlyPrice,
        annualMonthlyPrice: product.annualMonthlyPrice,
        currency: product.currency,
        billingCycle,
        quantity
      });
    }

    this.persistItems(currentItems);
    return 'ok';
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
      const parsed = JSON.parse(raw) as readonly CartItem[];
      if (!Array.isArray(parsed)) {
        return [];
      }
      return parsed.filter(item => this.isValidItem(item));
    } catch {
      return [];
    }
  }

  private persistItems(items: readonly CartItem[]): void {
    this._items.set(items);
    localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(items));
  }

  private isValidItem(item: CartItem | undefined): item is CartItem {
    if (!item) {
      return false;
    }
    const validCycle = item.billingCycle === 'MONTHLY' || item.billingCycle === 'ANNUAL';
    return Boolean(item.productId)
      && Boolean(item.productName)
      && Boolean(item.productCategory)
      && Boolean(item.currency)
      && Number.isFinite(item.monthlyPrice)
      && Number.isFinite(item.annualMonthlyPrice)
      && Number.isInteger(item.quantity)
      && item.quantity >= 1
      && item.quantity <= MAX_LINE_QUANTITY
      && validCycle;
  }
}
