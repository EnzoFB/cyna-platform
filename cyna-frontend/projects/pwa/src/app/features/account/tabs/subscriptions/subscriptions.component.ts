import { ChangeDetectionStrategy, Component, EventEmitter, inject, Input, Output, signal } from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AccountSubscription } from '../../models/account.models';
import { OverlayCloseDirective } from '../../../../shared/directives/overlay-close.directive';
import { PaymentService } from '../../../../core/services/payment.service';
import { ToastService } from '../../../../core/services/toast.service';

@Component({
  selector: 'app-subscriptions',
  standalone: true,
  imports: [TranslatePipe, OverlayCloseDirective],
  templateUrl: './subscriptions.component.html',
  styleUrl: './subscriptions.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SubscriptionsComponent {
  private readonly translateService = inject(TranslateService);
  private readonly paymentService = inject(PaymentService);
  private readonly toastService = inject(ToastService);

  @Input() subscriptions: readonly AccountSubscription[] = [];
  @Input() loading = false;
  @Input() updatingSubscriptionId: string | null = null;

  @Output() autoRenewChanged = new EventEmitter<{ subscriptionId: string; autoRenew: boolean }>();

  readonly pendingDisable = signal<AccountSubscription | null>(null);
  readonly openingPortal = signal(false);

  /**
   * A renewal payment failed (Stripe dunning in progress). The customer must
   * update their card via the Stripe Customer Portal — same delegation as the
   * payment-methods tab — before Stripe gives up and cancels the subscription.
   */
  openBillingPortal(): void {
    if (this.openingPortal()) return;
    this.openingPortal.set(true);
    this.paymentService.openBillingPortal(window.location.href).subscribe({
      next: ({ url }) => { window.location.href = url; },
      error: () => {
        this.openingPortal.set(false);
        this.toastService.showError(
          this.translateService.instant('account.subscriptions.pastDue.portalError'));
      },
    });
  }

  formatPrice(amount: number, currency: string): string {
    const locale = this.translateService.getCurrentLang() === 'fr' ? 'fr-FR' : 'en-US';

    return new Intl.NumberFormat(locale, {
      style: 'currency',
      currency,
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(amount);
  }

  formatDate(rawDate: string | null): string {
    if (!rawDate) {
      return this.translateService.instant('account.subscriptions.noBillingDate');
    }

    const date = new Date(rawDate);
    if (Number.isNaN(date.getTime())) {
      return this.translateService.instant('account.subscriptions.noBillingDate');
    }

    const locale = this.translateService.getCurrentLang() === 'fr' ? 'fr-FR' : 'en-US';

    return new Intl.DateTimeFormat(locale, {
      day: 'numeric',
      month: 'long',
      year: 'numeric'
    }).format(date);
  }

  requestAutoRenewToggle(subscription: AccountSubscription): void {
    if (subscription.status !== 'ACTIVE' || this.updatingSubscriptionId === subscription.id) {
      return;
    }

    if (subscription.autoRenew) {
      this.pendingDisable.set(subscription);
      return;
    }

    this.autoRenewChanged.emit({
      subscriptionId: subscription.id,
      autoRenew: true
    });
  }

  confirmDisable(): void {
    const subscription = this.pendingDisable();
    if (!subscription) {
      return;
    }

    this.autoRenewChanged.emit({
      subscriptionId: subscription.id,
      autoRenew: false
    });
    this.pendingDisable.set(null);
  }

  closeDisableModal(): void {
    this.pendingDisable.set(null);
  }

}
