import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AccountPaymentMethod } from '../../models/account.models';
import { PaymentService } from '../../../../core/services/payment.service';
import { ToastService } from '../../../../core/services/toast.service';

const PAYMENT_STORAGE_KEY = 'cyna_pwa_account_payment_methods';

const DEFAULT_PAYMENT_METHODS: readonly AccountPaymentMethod[] = [
  {
    id: 'payment-default-1',
    holder: 'Nom Prénom',
    brand: 'Visa',
    last4: '4242',
    expiryMonth: '12',
    expiryYear: '2026',
    isDefault: true
  }
];

@Component({
  selector: 'app-payment-methods',
  standalone: true,
  imports: [TranslatePipe, ReactiveFormsModule],
  templateUrl: './payment-methods.component.html',
  styleUrl: './payment-methods.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PaymentMethodsComponent {
  private readonly fb = inject(FormBuilder);
  private readonly paymentService = inject(PaymentService);
  private readonly toastService = inject(ToastService);
  private readonly translate = inject(TranslateService);

  readonly methods = signal<readonly AccountPaymentMethod[]>(this.loadMethods());
  readonly showCreateModal = signal(false);
  readonly openingPortal = signal(false);

  /**
   * Opens the Stripe-hosted Customer Portal so the user can manage their
   * real payment methods, view invoices and cancel subscriptions in a
   * PCI-compliant flow. Replaces the local mock progressively.
   */
  openBillingPortal(): void {
    if (this.openingPortal()) return;
    this.openingPortal.set(true);
    const returnUrl = window.location.origin + '/account';
    this.paymentService.openBillingPortal(returnUrl).subscribe({
      next: ({ url }) => {
        window.location.href = url;
      },
      error: () => {
        this.openingPortal.set(false);
        this.toastService.showError(
          this.translate.instant('account.payment.portalError')
        );
      },
    });
  }

  readonly orderedMethods = computed(() =>
    [...this.methods()].sort((a, b) => Number(b.isDefault) - Number(a.isDefault))
  );

  readonly form = this.fb.nonNullable.group({
    holder: ['', [Validators.required, Validators.minLength(2)]],
    cardNumber: ['', [Validators.required, Validators.pattern(/^\d(?:\s?\d){12,18}$/)]],
    expiry: ['', [Validators.required, Validators.pattern(/^(0[1-9]|1[0-2])\/\d{2}$/)]],
    cvv: ['', [Validators.required, Validators.pattern(/^\d{3,4}$/)]]
  });

  openCreateModal(): void {
    this.form.reset({
      holder: '',
      cardNumber: '',
      expiry: '',
      cvv: ''
    });
    this.form.markAsPristine();
    this.form.markAsUntouched();
    this.showCreateModal.set(true);
  }

  closeModal(): void {
    this.showCreateModal.set(false);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const digits = value.cardNumber.replace(/\s+/g, '');
    const [expiryMonth, expiryYearShort] = value.expiry.split('/');
    const expiryYear = `20${expiryYearShort}`;

    const nextMethod: AccountPaymentMethod = {
      id: this.newId(),
      holder: value.holder.trim(),
      brand: this.detectBrand(digits),
      last4: digits.slice(-4),
      expiryMonth,
      expiryYear,
      isDefault: this.methods().length === 0
    };

    this.persistMethods([nextMethod, ...this.methods()]);
    this.closeModal();
  }

  remove(id: string): void {
    const current = this.methods();
    const removed = current.find(method => method.id === id);
    if (!removed) {
      return;
    }

    const next = current.filter(method => method.id !== id);
    if (!next.length) {
      this.persistMethods([]);
      return;
    }

    if (removed.isDefault && !next.some(method => method.isDefault)) {
      const [first, ...rest] = next;
      this.persistMethods([{ ...first, isDefault: true }, ...rest]);
      return;
    }

    this.persistMethods(next);
  }

  setDefault(id: string): void {
    this.methods.update(list => list.map(m => ({ ...m, isDefault: m.id === id })));
    setTimeout(() => {
      const sorted = [...this.methods()].sort((a, b) => Number(b.isDefault) - Number(a.isDefault));
      this.persistMethods(sorted);
    }, 900);
  }

  isInvalid(controlName: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[controlName];
    return control.invalid && (control.touched || control.dirty);
  }

  private loadMethods(): readonly AccountPaymentMethod[] {
    try {
      const raw = localStorage.getItem(PAYMENT_STORAGE_KEY);
      if (!raw) {
        return DEFAULT_PAYMENT_METHODS;
      }

      const parsed = JSON.parse(raw) as unknown;
      if (!Array.isArray(parsed)) {
        return DEFAULT_PAYMENT_METHODS;
      }

      const normalized = parsed
        .filter((item): item is AccountPaymentMethod => this.isMethod(item))
        .map(item => ({ ...item }));

      if (!normalized.length) {
        return DEFAULT_PAYMENT_METHODS;
      }

      if (!normalized.some(method => method.isDefault)) {
        normalized[0] = { ...normalized[0], isDefault: true };
      }

      return normalized;
    } catch {
      return DEFAULT_PAYMENT_METHODS;
    }
  }

  private persistMethods(methods: readonly AccountPaymentMethod[]): void {
    this.methods.set(methods);
    localStorage.setItem(PAYMENT_STORAGE_KEY, JSON.stringify(methods));
  }

  private detectBrand(cardNumber: string): string {
    if (/^4/.test(cardNumber)) {
      return 'Visa';
    }
    if (/^5[1-5]/.test(cardNumber)) {
      return 'Mastercard';
    }
    if (/^3[47]/.test(cardNumber)) {
      return 'American Express';
    }
    return 'Card';
  }

  private isMethod(item: unknown): item is AccountPaymentMethod {
    if (!item || typeof item !== 'object') {
      return false;
    }

    const candidate = item as Partial<AccountPaymentMethod>;
    return (
      typeof candidate.id === 'string' &&
      typeof candidate.holder === 'string' &&
      typeof candidate.brand === 'string' &&
      typeof candidate.last4 === 'string' &&
      typeof candidate.expiryMonth === 'string' &&
      typeof candidate.expiryYear === 'string' &&
      typeof candidate.isDefault === 'boolean'
    );
  }

  private newId(): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
      return crypto.randomUUID();
    }

    return `payment-${Date.now()}`;
  }
}
