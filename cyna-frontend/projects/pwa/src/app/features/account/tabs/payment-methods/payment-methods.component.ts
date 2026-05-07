import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  inject,
  OnDestroy,
  OnInit,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { loadStripe, Stripe, StripeCardElement, StripeElements } from '@stripe/stripe-js';
import { environment } from '../../../../../environments/environment';
import { PaymentMethodService } from '../../../../core/services/payment-method.service';
import { ToastService } from '../../../../core/services/toast.service';
import { SavedPaymentMethod } from '../../../../core/models/saved-payment-method.model';

@Component({
  selector: 'app-payment-methods',
  standalone: true,
  imports: [TranslatePipe, ReactiveFormsModule],
  templateUrl: './payment-methods.component.html',
  styleUrl: './payment-methods.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaymentMethodsComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly service = inject(PaymentMethodService);
  private readonly toastService = inject(ToastService);
  private readonly translate = inject(TranslateService);
  private readonly fb = inject(FormBuilder);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly methods = signal<SavedPaymentMethod[]>([]);
  readonly loading = signal(false);
  readonly modalOpen = signal(false);
  readonly saving = signal(false);
  readonly stripeReady = signal(false);
  readonly stripeError = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    holder: ['', [Validators.required, Validators.minLength(2)]],
  });

  private stripe!: Stripe;
  private elements!: StripeElements;
  private cardElement!: StripeCardElement;

  ngOnInit(): void {
    this.loadMethods();
  }

  async ngAfterViewInit(): Promise<void> {
    const stripe = await loadStripe(environment.stripePublishableKey);
    if (stripe) {
      this.stripe = stripe;
    }
  }

  ngOnDestroy(): void {
    this.cardElement?.destroy();
  }

  loadMethods(): void {
    this.loading.set(true);
    this.service.getAll().subscribe({
      next: methods => {
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

  async openModal(): Promise<void> {
    this.form.reset({ holder: '' });
    this.form.markAsPristine();
    this.stripeError.set(null);
    this.stripeReady.set(false);
    this.modalOpen.set(true);
    this.cdr.markForCheck();

    // Mount Stripe Card Element after the modal DOM is rendered
    setTimeout(() => this.mountCardElement(), 0);
  }

  closeModal(): void {
    this.modalOpen.set(false);
    this.cardElement?.unmount();
  }

  async save(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (!this.stripeReady()) {
      return;
    }

    this.saving.set(true);
    this.stripeError.set(null);

    try {
      // 1. Get a SetupIntent client_secret from the backend
      const clientSecret = await this.service.createSetupIntent().toPromise();
      if (!clientSecret) throw new Error('No client secret');

      // 2. Confirm card setup with Stripe — no card data touches our server
      const { setupIntent, error } = await this.stripe.confirmCardSetup(clientSecret, {
        payment_method: {
          card: this.cardElement,
          billing_details: { name: this.form.controls.holder.value },
        },
      });

      if (error) {
        this.stripeError.set(error.message ?? this.translate.instant('account.payment.error.generic'));
        this.saving.set(false);
        this.cdr.markForCheck();
        return;
      }

      const paymentMethodId = setupIntent?.payment_method as string | undefined;
      if (!paymentMethodId) {
        this.stripeError.set(this.translate.instant('account.payment.error.generic'));
        this.saving.set(false);
        this.cdr.markForCheck();
        return;
      }

      // 3. Persist the PaymentMethod id in our backend
      await this.service.save(paymentMethodId).toPromise();

      this.toastService.showSuccess(this.translate.instant('account.payment.toast.created'));
      this.closeModal();
      this.loadMethods();
    } catch {
      this.toastService.showError(this.translate.instant('account.payment.toast.error'));
    } finally {
      this.saving.set(false);
      this.cdr.markForCheck();
    }
  }

  deleteMethod(id: string): void {
    this.service.delete(id).subscribe({
      next: () => {
        this.toastService.showSuccess(this.translate.instant('account.payment.toast.deleted'));
        this.loadMethods();
      },
      error: () => this.toastService.showError(this.translate.instant('account.payment.toast.error')),
    });
  }

  setDefault(id: string): void {
    this.service.setDefault(id).subscribe({
      next: () => {
        // Optimistic update: mark as default in place so the animation plays, reorder on reload
        this.methods.update(list => list.map(m => ({ ...m, isDefault: m.id === id })));
        this.cdr.markForCheck();
        setTimeout(() => this.loadMethods(), 900);
      },
      error: () => this.toastService.showError(this.translate.instant('account.payment.toast.error')),
    });
  }

  isInvalid(field: 'holder'): boolean {
    const c = this.form.controls[field];
    return c.invalid && (c.touched || c.dirty);
  }

  private mountCardElement(): void {
    const container = document.getElementById('stripe-card-element');
    if (!container || !this.stripe) return;

    this.elements = this.stripe.elements();
    this.cardElement = this.elements.create('card', {
      style: {
        base: {
          fontSize: '14px',
          color: '#1e293b',
          '::placeholder': { color: '#94a3b8' },
        },
      },
      hidePostalCode: true,
    });

    this.cardElement.mount(container);
    this.cardElement.on('change', event => {
      this.stripeReady.set(event.complete);
      this.stripeError.set(event.error?.message ?? null);
      this.cdr.markForCheck();
    });
  }
}
