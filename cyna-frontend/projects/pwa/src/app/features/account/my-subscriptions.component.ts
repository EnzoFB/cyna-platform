import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import {
  SubscriptionResponse,
  SubscriptionService,
} from '../../core/services/subscription.service';
import { PaymentService } from '../../core/services/payment.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-my-subscriptions',
  imports: [CurrencyPipe, DatePipe, RouterLink, TranslatePipe],
  templateUrl: './my-subscriptions.component.html',
  styleUrl: './my-subscriptions.component.scss',
})
export class MySubscriptionsComponent implements OnInit {

  private readonly subscriptionService = inject(SubscriptionService);
  private readonly paymentService = inject(PaymentService);
  private readonly toastService = inject(ToastService);
  private readonly translate = inject(TranslateService);

  readonly subscriptions = signal<SubscriptionResponse[]>([]);
  readonly loading = signal(true);
  readonly error = signal(false);
  readonly cancellingId = signal<string | null>(null);
  readonly confirmCancelId = signal<string | null>(null);
  readonly openingPortal = signal(false);

  readonly hasItems = computed(() => this.subscriptions().length > 0);

  ngOnInit(): void {
    this.fetch();
  }

  private fetch(): void {
    this.loading.set(true);
    this.error.set(false);
    this.subscriptionService.list().subscribe({
      next: (paged) => {
        this.subscriptions.set(paged.items);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }

  askCancel(id: string): void {
    this.confirmCancelId.set(id);
  }

  dismissCancel(): void {
    this.confirmCancelId.set(null);
  }

  confirmCancel(): void {
    const id = this.confirmCancelId();
    if (!id) return;

    this.cancellingId.set(id);
    this.confirmCancelId.set(null);
    this.subscriptionService.cancel(id).subscribe({
      next: (updated) => {
        this.subscriptions.update((list) =>
          list.map((s) => (s.id === updated.id ? updated : s))
        );
        this.cancellingId.set(null);
        this.toastService.showSuccess(
          this.translate.instant('mySubscriptions.cancelSuccess')
        );
      },
      error: () => {
        this.cancellingId.set(null);
        this.toastService.showError(
          this.translate.instant('mySubscriptions.cancelError')
        );
      },
    });
  }

  isCancelling(id: string): boolean {
    return this.cancellingId() === id;
  }

  isCancelable(status: SubscriptionResponse['status']): boolean {
    return status === 'ACTIVE' || status === 'PAST_DUE' || status === 'PAUSED';
  }

  statusLabel(status: SubscriptionResponse['status']): string {
    return this.translate.instant('mySubscriptions.status.' + status);
  }

  statusClass(status: SubscriptionResponse['status']): string {
    switch (status) {
      case 'ACTIVE': return 'status-badge--active';
      case 'PAST_DUE': return 'status-badge--past-due';
      case 'CANCELLED': return 'status-badge--cancelled';
      case 'EXPIRED': return 'status-badge--expired';
      case 'PAUSED': return 'status-badge--paused';
      case 'PENDING': return 'status-badge--pending';
      default: return '';
    }
  }

  cycleLabel(cycle: 'MONTHLY' | 'ANNUAL'): string {
    return cycle === 'ANNUAL'
      ? this.translate.instant('cartPage.annual')
      : this.translate.instant('cartPage.monthly');
  }

  openBillingPortal(): void {
    if (this.openingPortal()) return;
    this.openingPortal.set(true);
    const returnUrl = window.location.origin + '/account/subscriptions';
    this.paymentService.openBillingPortal(returnUrl).subscribe({
      next: ({ url }) => {
        // Hard navigation off-app to Stripe-hosted portal.
        window.location.href = url;
      },
      error: () => {
        this.openingPortal.set(false);
        this.toastService.showError(
          this.translate.instant('mySubscriptions.portalError')
        );
      },
    });
  }
}
