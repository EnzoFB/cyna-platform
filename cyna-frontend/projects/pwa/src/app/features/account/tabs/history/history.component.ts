import { ChangeDetectionStrategy, Component, inject, Input, signal } from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AccountInvoice, AccountOrder } from '../../models/account.models';

@Component({
  selector: 'app-history',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './history.component.html',
  styleUrl: './history.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HistoryComponent {
  private readonly translateService = inject(TranslateService);

  @Input() orders: readonly AccountOrder[] = [];
  @Input() invoices: readonly AccountInvoice[] = [];
  @Input() loading = false;

  readonly selectedOrder = signal<AccountOrder | null>(null);

  openDetails(order: AccountOrder): void {
    this.selectedOrder.set(order);
  }

  closeDetails(): void {
    this.selectedOrder.set(null);
  }

  formatDate(rawDate: string): string {
    const date = new Date(rawDate);
    if (Number.isNaN(date.getTime())) {
      return '--';
    }

    const locale = this.translateService.getCurrentLang() === 'fr' ? 'fr-FR' : 'en-US';
    return new Intl.DateTimeFormat(locale, {
      day: '2-digit',
      month: 'long',
      year: 'numeric'
    }).format(date);
  }

  formatCurrency(value: number, currency: string): string {
    const locale = this.translateService.getCurrentLang() === 'fr' ? 'fr-FR' : 'en-US';
    return new Intl.NumberFormat(locale, {
      style: 'currency',
      currency,
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(value);
  }

  /** Stripe-generated PDF/hosted link. Opens in a new tab; never stored locally. */
  openInvoice(invoice: AccountInvoice): void {
    const url = invoice.invoicePdfUrl ?? invoice.hostedInvoiceUrl;
    if (url) {
      window.open(url, '_blank', 'noopener');
    }
  }

  downloadInvoice(order: AccountOrder): void {
    const lines = order.lines
      .map(line => `${line.productName} x${line.quantity} - ${this.formatCurrency(line.unitPrice, line.currency)}`)
      .join('\n');

    const content = [
      `${this.translateService.instant('account.history.invoice.title')} ${order.id}`,
      `${this.translateService.instant('account.history.invoice.date')} ${this.formatDate(order.createdAt)}`,
      '',
      lines,
      '',
      `${this.translateService.instant('account.history.invoice.total')} ${this.formatCurrency(order.totalAmount, order.currency)}`
    ].join('\n');

    const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `invoice-${order.id}.txt`;
    anchor.click();
    URL.revokeObjectURL(url);
  }
}
