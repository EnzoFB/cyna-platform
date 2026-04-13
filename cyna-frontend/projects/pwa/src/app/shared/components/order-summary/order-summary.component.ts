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
