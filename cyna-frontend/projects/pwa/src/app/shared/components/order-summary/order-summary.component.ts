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
  vatAmount: number;
  totalTtc: number;
  currency: string;
  /**
   * True when the amounts are the EXACT VAT computed by Stripe Tax (from the
   * billing address + VAT number), false/undefined when they are the local 20%
   * estimate. Drives whether the "estimated VAT" disclaimer is shown.
   */
  vatExact?: boolean;
  /** True when the intra-EU B2B reverse charge applies (VAT 0%, due by buyer). */
  reverseCharge?: boolean;
}

export interface OrderSummaryOptions {
  showCheckoutButton?: boolean;
  checkoutDisabled?: boolean;
  hasUnavailableItems?: boolean;
  /**
   * Show a muted note that the displayed VAT is indicative and the final
   * amount is determined by the billing address (Stripe Tax computes the
   * authoritative VAT — incl. B2B reverse charge — on the invoice). Enabled
   * at checkout where a billing address is collected.
   */
  vatNote?: boolean;
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

  @Input() options: OrderSummaryOptions = {
    showCheckoutButton: true,
    checkoutDisabled: false,
    hasUnavailableItems: false,
  };

  @Output() checkout = new EventEmitter<void>();

  isLogged = false;

  constructor(private authService: AuthService) {
    effect(() => {
      this.isLogged = this.authService.isAuthenticated();
    });
  }
}
