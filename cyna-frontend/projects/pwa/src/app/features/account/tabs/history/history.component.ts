import { ChangeDetectionStrategy, Component, inject, Input, signal } from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { jsPDF } from 'jspdf';
import { AccountInvoice, AccountOrder } from '../../models/account.models';
import { OverlayCloseDirective } from '../../../../shared/directives/overlay-close.directive';

@Component({
  selector: 'app-history',
  standalone: true,
  imports: [TranslatePipe, OverlayCloseDirective],
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
    if (Number.isNaN(date.getTime())) return '--';
    return new Intl.DateTimeFormat(this.locale, { day: '2-digit', month: 'long', year: 'numeric' }).format(date);
  }

  formatCurrency(value: number, currency: string): string {
    return new Intl.NumberFormat(this.locale, {
      style: 'currency', currency,
      minimumFractionDigits: 2, maximumFractionDigits: 2
    }).format(value);
  }

  getCountryName(countryCode: string): string {
    try {
      return new Intl.DisplayNames([this.locale], { type: 'region' }).of(countryCode) ?? countryCode;
    } catch {
      return countryCode;
    }
  }

  /** Stripe-generated PDF/hosted link. Opens in a new tab; never stored locally. */
  openInvoice(invoice: AccountInvoice): void {
    const url = invoice.invoicePdfUrl ?? invoice.hostedInvoiceUrl;
    if (url) window.open(url, '_blank', 'noopener');
  }

  downloadInvoice(order: AccountOrder): void {
    const t = (key: string) => this.translateService.instant(key);
    const doc = new jsPDF({ unit: 'mm', format: 'a4' });

    const ML = 20;   // margin left
    const MR = 190;  // margin right
    const CW = MR - ML; // content width

    type RGB = [number, number, number];
    const color: Record<string, RGB> = {
      dark:   [13, 27, 42],
      muted:  [110, 120, 135],
      line:   [220, 222, 226],
      accent: [11, 31, 66],
    };

    const right = (text: string, y: number) =>
      doc.text(text, MR, y, { align: 'right' });

    const hline = (y: number, r: RGB = color['line']) => {
      doc.setDrawColor(r[0], r[1], r[2]);
      doc.setLineWidth(0.3);
      doc.line(ML, y, MR, y);
    };

    const setColor = (c: RGB) => doc.setTextColor(c[0], c[1], c[2]);

    const label = (text: string, x: number, y: number) => {
      doc.setFont('helvetica', 'bold');
      doc.setFontSize(7.5);
      setColor(color['muted']);
      doc.text(text.toUpperCase(), x, y);
    };

    let y = 22;

    // ── Brand ──────────────────────────────────────────────────────
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(18);
    setColor(color['accent']);
    doc.text('CYNA', ML, y);

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(9);
    setColor(color['muted']);
    right(this.formatDate(order.createdAt), y);

    y += 9;
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(13);
    setColor(color['dark']);
    doc.text(t('account.history.invoice.title'), ML, y);

    y += 4;
    hline(y);
    y += 8;

    // ── Reference ──────────────────────────────────────────────────
    label(t('account.history.columns.reference'), ML, y);
    y += 5;
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(8.5);
    setColor(color['muted']);
    doc.text(order.id, ML, y);
    y += 9;

    // ── Date + Status ──────────────────────────────────────────────
    const col2 = ML + CW / 2;
    label(t('account.history.invoice.date'), ML, y);
    label(t('account.history.columns.status'), col2, y);
    y += 5;
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(10);
    setColor(color['dark']);
    doc.text(this.formatDate(order.createdAt), ML, y);
    doc.text(t(`account.common.status.${order.status}`), col2, y);
    y += 10;

    // ── Billing Address ────────────────────────────────────────────
    if (order.billingAddress) {
      label(t('account.history.invoice.billingAddress'), ML, y);
      y += 5;
      doc.setFont('helvetica', 'normal');
      doc.setFontSize(10);
      setColor(color['dark']);
      doc.text(order.billingAddress.line1, ML, y);
      y += 5;
      doc.text(`${order.billingAddress.zipCode} ${order.billingAddress.city}`, ML, y);
      y += 5;
      doc.text(this.getCountryName(order.billingAddress.countryCode), ML, y);
      y += 10;
    }

    // ── Services ───────────────────────────────────────────────────
    hline(y);
    y += 7;

    // Column headers
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(8);
    setColor(color['muted']);
    doc.text(t('account.history.details.services').toUpperCase(), ML, y);
    doc.text(t('account.subscriptions.columns.billingCycle').toUpperCase(), ML + 95, y, { align: 'center' });
    doc.text(t('account.subscriptions.columns.quantity').toUpperCase(), ML + 118, y, { align: 'center' });
    doc.text(t('account.history.columns.total').toUpperCase(), MR, y, { align: 'right' });

    y += 2;
    hline(y);
    y += 6;

    // Lines
    for (const line of order.lines) {
      const cycle = t(`account.common.billingCycle.${line.billingCycle}`);
      const lineTotal = this.formatCurrency(line.unitPrice * line.quantity, line.currency);

      doc.setFont('helvetica', 'normal');
      doc.setFontSize(10);
      setColor(color['dark']);

      // Product name — truncate if too long
      const maxNameW = 88;
      const nameParts = doc.splitTextToSize(line.productName, maxNameW) as string[];
      doc.text(nameParts[0], ML, y);

      doc.setFontSize(9);
      setColor(color['muted']);
      doc.text(cycle, ML + 95, y, { align: 'center' });
      doc.text(`x${line.quantity}`, ML + 118, y, { align: 'center' });

      doc.setFontSize(10);
      doc.setFont('helvetica', 'bold');
      setColor(color['dark']);
      right(lineTotal, y);

      y += 7;
    }

    // ── Totals ─────────────────────────────────────────────────────
    y += 2;
    hline(y);
    y += 7;

    const totalsX = ML + 100;

    const totalRow = (labelText: string, value: string, bold = false) => {
      doc.setFont('helvetica', bold ? 'bold' : 'normal');
      doc.setFontSize(bold ? 11 : 9.5);
      setColor(bold ? color['dark'] : color['muted']);
      doc.text(labelText, totalsX, y);
      doc.setFont('helvetica', bold ? 'bold' : 'normal');
      setColor(color['dark']);
      right(value, y);
      y += bold ? 0 : 6;
    };

    totalRow(t('account.history.details.subtotal'), this.formatCurrency(order.subtotalAmount, order.currency));
    totalRow(t('account.history.details.vat'), this.formatCurrency(order.vatAmount, order.currency));

    y += 3;
    hline(y, [180, 185, 195] as RGB);
    y += 7;
    totalRow(t('account.history.columns.total'), this.formatCurrency(order.totalAmount, order.currency), true);

    // ── Footer ─────────────────────────────────────────────────────
    const pageH = 297;
    hline(pageH - 18);
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(8);
    setColor(color['muted']);
    doc.text('CYNA · cyna.fr · contact@cyna.fr', ML, pageH - 12);
    right(`Ref. ${order.id.slice(0, 8).toUpperCase()}`, pageH - 12);

    // ── Save ───────────────────────────────────────────────────────
    const filename = `${t('account.history.invoice.filename')}-${order.id.slice(0, 8)}.pdf`;
    doc.save(filename);
  }

  private get locale(): string {
    return this.translateService.getCurrentLang() === 'fr' ? 'fr-FR' : 'en-US';
  }
}
