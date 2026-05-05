import { ChangeDetectionStrategy, Component, EventEmitter, inject, Input, Output, signal } from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AccountSubscription } from '../../models/account.models';

@Component({
  selector: 'app-subscriptions',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './subscriptions.component.html',
  styleUrl: './subscriptions.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SubscriptionsComponent {
  private readonly translateService = inject(TranslateService);

  @Input() subscriptions: readonly AccountSubscription[] = [];
  @Input() loading = false;
  @Input() updatingSubscriptionId: string | null = null;

  @Output() autoRenewChanged = new EventEmitter<{ subscriptionId: string; autoRenew: boolean }>();

  readonly pendingDisable = signal<AccountSubscription | null>(null);

  formatPrice(amount: number, currency: string): string {
    const locale = this.translateService.getCurrentLang() === 'fr' ? 'fr-FR' : 'en-US';

    return new Intl.NumberFormat(locale, {
      style: 'currency',
      currency,
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
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
