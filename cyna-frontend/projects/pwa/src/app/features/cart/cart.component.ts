import { CurrencyPipe, UpperCasePipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../core/services/auth.service';
import { CartBillingCycle, CartItem, CartMutationResult, CartService } from '../../core/services/cart.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-cart',
  imports: [CurrencyPipe, RouterLink, TranslatePipe, UpperCasePipe],
  templateUrl: './cart.component.html',
  styleUrl: './cart.component.scss',
})
export class CartComponent {
  private readonly cartService = inject(CartService);
  private readonly authService = inject(AuthService);
  private readonly toastService = inject(ToastService);
  private readonly translate = inject(TranslateService);
  private readonly router = inject(Router);

  readonly items = this.cartService.items;
  readonly totalItems = this.cartService.totalItems;
  readonly subtotalHt = this.cartService.subtotalHt;
  readonly vatAmount = this.cartService.vatAmount;
  readonly totalTtc = this.cartService.totalTtc;
  readonly currency = this.cartService.currency;
  readonly isEmpty = this.cartService.isEmpty;
  readonly hasUnavailableItems = this.cartService.hasUnavailableItems;
  readonly isAuthenticated = this.authService.isAuthenticated;

  readonly checkoutDisabled = computed(() => !this.cartService.checkoutAllowed());

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

    if (!this.authService.isAuthenticated()) {
      void this.router.navigate(['/auth/login']);
      return;
    }

    void this.router.navigate(['/checkout']);
  }

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
