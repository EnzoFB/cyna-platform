import {Component, effect, EventEmitter, Input, Output} from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { TranslatePipe } from '@ngx-translate/core';
import {AuthService} from "../../../core/services/auth.service";

export interface OrderSummaryItem {
  label: string;
  quantity: number;
  billingCycle: 'MONTHLY' | 'ANNUAL';
  total: number;
}

export interface OrderSummaryData {
  items: readonly OrderSummaryItem[];
  subtotalHt: number;
  currency: string;
  /**
   * Authoritative VAT computed by Stripe Tax (from the billing address +
   * VAT number). Absent on the cart page — at that point we have no country,
   * so VAT cannot be computed and only the HT subtotal is shown. Populated on
   * the checkout page as soon as the address yields a tax preview.
   */
  vatAmount?: number;
  totalTtc?: number;
  /** True when the intra-EU B2B reverse charge applies (VAT 0%, due by buyer). */
  reverseCharge?: boolean;
}

export interface OrderSummaryOptions {
  showCheckoutButton?: boolean;
  checkoutDisabled?: boolean;
  hasUnavailableItems?: boolean;
}

@Component({
  selector: 'app-order-summary',
  standalone: true,
  imports: [CurrencyPipe, TranslatePipe],
  templateUrl: './order-summary.component.html',
  styleUrl: './order-summary.component.scss',
})
export class OrderSummaryComponent {

  @Input({ required: true }) summary!: OrderSummaryData;

  /**
   * True while the checkout is waiting on the Stripe Tax preview. Shows a
   * "VAT calculating…" line in place of the VAT/total so the user knows the
   * amount is still being computed. Always false on the cart page.
   */
  @Input() taxLoading = false;

  @Input() options: OrderSummaryOptions = {
    showCheckoutButton: true,
    checkoutDisabled: false,
    hasUnavailableItems: false,
  };

  /**
   * True on the cart page, where the VAT is an *estimate* computed at the French
   * rate by default. Renders a note explaining that the binding amount is
   * recomputed at checkout from the real billing address. False on checkout,
   * where the VAT is already authoritative.
   */
  @Input() vatEstimatedNote = false;

  @Output() checkout = new EventEmitter<void>();

  isLogged = false;

  constructor(private authService: AuthService) {
    effect(() => {
      this.isLogged = this.authService.isAuthenticated();
    });
  }
}
