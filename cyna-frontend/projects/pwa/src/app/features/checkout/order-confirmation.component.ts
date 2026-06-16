import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { OrderResponse, OrderService } from '../../core/services/order.service';
import { OrderTaxSummaryResponse, PaymentService } from '../../core/services/payment.service';

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
  private readonly paymentService = inject(PaymentService);
  private readonly translate = inject(TranslateService);

  readonly order = signal<OrderResponse | null>(null);
  readonly loading = signal(true);
  readonly notFound = signal(false);

  // Authoritative VAT/TTC read from the order's Stripe invoices once the order is
  // PAID. Null until it lands — the summary then shows the HT subtotal only.
  readonly taxSummary = signal<OrderTaxSummaryResponse | null>(null);

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

  // The invoice (and thus its VAT) is finalised by Stripe a beat after the order
  // flips to PAID — retry a few times before settling for the HT-only fallback.
  private taxPollHandle: ReturnType<typeof setTimeout> | null = null;
  private readonly maxTaxAttempts = 6; // ~12 s with 2s interval

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
    if (this.taxPollHandle) clearTimeout(this.taxPollHandle);
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
        } else if (order.status === 'PAID' && !this.taxSummary()) {
          // Paid → pull the authoritative VAT/TTC from the Stripe invoice.
          this.fetchTaxSummary(orderId, 0);
        }
      },
      error: () => {
        this.notFound.set(true);
        this.loading.set(false);
      },
    });
  }

  private fetchTaxSummary(orderId: string, attempt: number): void {
    this.paymentService.getOrderTaxSummary(orderId).subscribe({
      next: (summary) => {
        if (summary.available) {
          this.taxSummary.set(summary);
        } else if (attempt + 1 < this.maxTaxAttempts) {
          // Invoice not finalised yet — retry shortly. Until then the summary
          // keeps showing the HT subtotal only.
          this.taxPollHandle = setTimeout(
            () => this.fetchTaxSummary(orderId, attempt + 1), 2000);
        }
      },
      // Best-effort: any error leaves the HT-only fallback in place.
      error: () => undefined,
    });
  }

  goToCatalog(): void {
    void this.router.navigate(['/catalog']);
  }
}
