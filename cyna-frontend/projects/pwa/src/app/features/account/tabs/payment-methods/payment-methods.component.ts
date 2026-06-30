import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { PaymentMethodService } from '../../../../core/services/payment-method.service';
import { PaymentService } from '../../../../core/services/payment.service';
import { ToastService } from '../../../../core/services/toast.service';
import { SavedPaymentMethod } from '../../../../core/models/saved-payment-method.model';

/**
 * Read-only listing of the user's saved cards + an entry point to the Stripe
 * Customer Portal for every actual operation (add, delete, update expired,
 * change default, view invoices). We deliberately don't reimplement what
 * Stripe already does securely and PSD2-compliantly on the portal side.
 *
 * The list itself stays in sync with the portal through three Stripe webhooks
 * (`payment_method.attached`, `detached`, `automatically_updated`) handled
 * by `SyncSavedPaymentMethodFromStripeCommandHandler` on the backend.
 */
@Component({
  selector: 'app-payment-methods',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './payment-methods.component.html',
  styleUrl: './payment-methods.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaymentMethodsComponent implements OnInit {
  private readonly service = inject(PaymentMethodService);
  private readonly paymentService = inject(PaymentService);
  private readonly toastService = inject(ToastService);
  private readonly translate = inject(TranslateService);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly methods = signal<SavedPaymentMethod[]>([]);
  readonly loading = signal(false);
  readonly openingPortal = signal(false);

  ngOnInit(): void {
    this.loadMethods();
  }

  loadMethods(): void {
    this.loading.set(true);
    this.service.getAll().subscribe({
      next: methods => {
        // Default first; the rest in insertion order from the API.
        this.methods.set([...methods].sort((a, b) => Number(b.isDefault) - Number(a.isDefault)));
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  isExpired(method: SavedPaymentMethod): boolean {
    const month = Number(method.expMonth);
    const year = Number(method.expYear);
    if (!Number.isFinite(month) || !Number.isFinite(year) || month < 1 || month > 12) return false;
    return new Date(Date.UTC(year, month, 0, 23, 59, 59)).getTime() < Date.now();
  }

  /**
   * Redirect to Stripe Customer Portal. The portal handles add / delete /
   * set-default / expired-card update / invoices / subscription management.
   * Returns the user to the exact same page on completion; the three
   * `payment_method.*` webhooks then propagate any change back into our cache.
   */
  openStripePortal(): void {
    if (this.openingPortal()) return;
    this.openingPortal.set(true);

    this.paymentService.openBillingPortal(window.location.href).subscribe({
      next: ({ url }) => {
        window.location.href = url;
      },
      error: () => {
        this.openingPortal.set(false);
        this.toastService.showError(this.translate.instant('account.payment.portal.error'));
        this.cdr.markForCheck();
      },
    });
  }
}
