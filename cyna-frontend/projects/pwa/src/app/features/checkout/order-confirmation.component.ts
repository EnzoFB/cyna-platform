import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { OrderResponse, OrderService } from '../../core/services/order.service';

@Component({
  selector: 'app-order-confirmation',
  imports: [RouterLink, CurrencyPipe, DatePipe, TranslatePipe],
  templateUrl: './order-confirmation.component.html',
  styleUrl: './order-confirmation.component.scss',
})
export class OrderConfirmationComponent implements OnInit, OnDestroy {

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly orderService = inject(OrderService);
  private readonly translate = inject(TranslateService);

  readonly order = signal<OrderResponse | null>(null);
  readonly loading = signal(true);
  readonly notFound = signal(false);

  // Order is "confirmed" once the webhook has flipped it to PAID.
  readonly isConfirmed = computed(() => this.order()?.status === 'PAID');
  readonly isPending = computed(() => this.order()?.status === 'PENDING');

  // Each line ends one "billing period" after createdAt (1 month or 1 year).
  readonly nextBillingDate = computed(() => {
    const o = this.order();
    if (!o || !o.lines.length) return null;
    const start = new Date(o.createdAt);
    const cycle = o.lines[0].billingCycle;
    const next = new Date(start);
    if (cycle === 'ANNUAL') {
      next.setFullYear(next.getFullYear() + 1);
    } else {
      next.setMonth(next.getMonth() + 1);
    }
    return next;
  });

  readonly billingCycleLabel = computed(() => {
    const cycle = this.order()?.lines?.[0]?.billingCycle;
    if (cycle === 'ANNUAL') return this.translate.instant('cartPage.annual');
    return this.translate.instant('cartPage.monthly');
  });

  private pollHandle: ReturnType<typeof setTimeout> | null = null;
  private pollAttempts = 0;
  private readonly maxPollAttempts = 15; // ~30 s with 2s interval

  ngOnInit(): void {
    const orderId = this.route.snapshot.paramMap.get('id');
    if (!orderId) {
      this.notFound.set(true);
      this.loading.set(false);
      return;
    }
    this.fetchOrder(orderId);
  }

  ngOnDestroy(): void {
    if (this.pollHandle) clearTimeout(this.pollHandle);
  }

  private fetchOrder(orderId: string): void {
    this.orderService.getOrder(orderId).subscribe({
      next: (order) => {
        this.order.set(order);
        this.loading.set(false);
        // Poll while the webhook is still on its way.
        if (order.status === 'PENDING' && this.pollAttempts < this.maxPollAttempts) {
          this.pollAttempts++;
          this.pollHandle = setTimeout(() => this.fetchOrder(orderId), 2000);
        }
      },
      error: () => {
        this.notFound.set(true);
        this.loading.set(false);
      },
    });
  }

  goToCatalog(): void {
    void this.router.navigate(['/catalog']);
  }
}
