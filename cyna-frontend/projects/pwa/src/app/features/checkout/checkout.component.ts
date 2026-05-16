import {
  AfterViewInit,
  Component,
  computed,
  effect,
  inject,
  OnInit,
  signal
} from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { Router } from '@angular/router';
import { parsePhoneNumberFromString } from 'libphonenumber-js';
import {
  loadStripe,
  Stripe,
  StripeCardCvcElement,
  StripeCardExpiryElement,
  StripeCardNumberElement,
  StripeElements
} from '@stripe/stripe-js';
import * as countries from 'i18n-iso-countries';
import frLocale from 'i18n-iso-countries/langs/fr.json';
import enLocale from 'i18n-iso-countries/langs/en.json';
import { firstValueFrom } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { DecimalPipe } from '@angular/common';

import { CartService } from '../../core/services/cart.service';
import { AuthService } from '../../core/services/auth.service';
import { UserService } from '../../core/services/user.service';
import { OrderService } from '../../core/services/order.service';
import { PaymentService } from '../../core/services/payment.service';
import { PaymentMethodService } from '../../core/services/payment-method.service';
import { ConsentLogService } from '../../core/services/consent-log.service';
import { AddressService } from '../../core/services/address.service';
import { AddressResponse } from '../../core/models/address.model';
import { SavedPaymentMethod } from '../../core/models/saved-payment-method.model';
import { UserResponse } from '../../core/models/user.model';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { OrderSummaryComponent } from '../../shared/components/order-summary/order-summary.component';
import { phoneValidator } from '../../shared/validators/phone.validator';
import { environment } from '../../../environments/environment';

type Mode = 'new' | 'saved';
interface Country { code: string; name: string }

@Component({
  selector: 'app-checkout',
  imports: [
    ReactiveFormsModule,
    OrderSummaryComponent,
    TranslatePipe,
    DecimalPipe
  ],
  templateUrl: './checkout.component.html',
  styleUrl: './checkout.component.scss',
})
export class CheckoutComponent implements OnInit, AfterViewInit {

  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly userService = inject(UserService);
  private readonly orderService = inject(OrderService);
  private readonly paymentService = inject(PaymentService);
  private readonly paymentMethodService = inject(PaymentMethodService);
  private readonly consentLogService = inject(ConsentLogService);
  private readonly router = inject(Router);
  private readonly addressService = inject(AddressService);
  protected readonly cartService = inject(CartService);
  private readonly translate = inject(TranslateService);

  stripe!: Stripe;
  elements!: StripeElements;
  cardNumber!: StripeCardNumberElement;
  cardExpiry!: StripeCardExpiryElement;
  cardCvc!: StripeCardCvcElement;

  private stripeNumberComplete = signal(false);
  private stripeExpiryComplete = signal(false);
  private stripeCvcComplete = signal(false);

  stripeErrors = signal({
    number: null as string | null,
    expiry: null as string | null,
    cvc: null as string | null,
  });

  readonly stripeCardValid = computed(() =>
    this.stripeNumberComplete() &&
    this.stripeExpiryComplete() &&
    this.stripeCvcComplete()
  );

  addressMode = signal<Mode>('new');
  paymentMode = signal<Mode>('new');

  // GDPR consent for persisting checkout-typed info into the user account.
  // Default address = checked (low PII risk, high convenience), card = unchecked
  // (explicit opt-in required for retention beyond the active subscription).
  saveAddressConsent = signal(true);
  saveCardConsent = signal(false);

  selectedAddress = signal<AddressResponse | null>(null);
  selectedPayment = signal<SavedPaymentMethod | null>(null);

  isAddressOpen = signal(false);
  isPaymentOpen = signal(false);
  isCountryOpen = signal(false);

  billingValid = signal(false);
  paymentValid = signal(false);

  isLoading = signal(false);
  submitError = signal<string | null>(null);

  countryList = signal<Country[]>([]);
  countryCode = signal('FR');
  countrySearch = signal('');

  savedAddresses: AddressResponse[] = [];
  savedPayments: SavedPaymentMethod[] = [];

  readonly form = this.fb.group({
    billing: this.fb.group({
      firstName: ['', Validators.required],
      lastName:  ['', Validators.required],
      company:   [''],
      vatNumber: [''],
      address:   ['', Validators.required],
      address2:  [''],
      zipCode:   ['', [Validators.required, Validators.pattern(/^[0-9A-Za-z -]{3,10}$/)]],
      city:      ['', Validators.required],
      region:    ['', Validators.required],
      country:   ['FR', Validators.required],
      phone:     ['', Validators.required],
    }),
    payment: this.fb.group({
      holder: ['', Validators.required],
    }),
  });

  readonly isLogged = computed(() => this.authService.isAuthenticated());
  private readonly profile = signal<UserResponse | null>(null);
  readonly displayName = computed(() => {
    const p = this.profile();
    if (p) return `${p.firstName} ${p.lastName}`;
    const u = this.authService.user();
    if (u?.firstName && u?.lastName) return `${u.firstName} ${u.lastName}`;
    return u?.email ?? '';
  });
  readonly totalTtc = this.cartService.totalTtc;
  readonly currency = this.cartService.currency;
  // 'MONTHLY' | 'ANNUAL' | 'MIXED' — drives wording of the recurring notice
  // and submit button. MIXED appears when the cart has both monthly and
  // annual lines (each line becomes its own Stripe Subscription in the V14
  // checkout, so the notice must disclose both commitments).
  readonly cycleMode = this.cartService.cartCycleMode;
  readonly monthlyTotalTtc = this.cartService.monthlyTotalTtc;
  readonly annualTotalTtc = this.cartService.annualTotalTtc;

  readonly selectedCountry = computed(() => {
    const code = this.countryCode();
    return this.countryList().find(c => c.code === code) ?? null;
  });

  readonly filteredCountries = computed(() => {
    const search = this.countrySearch().toLowerCase().trim();
    const list = this.countryList();

    if (!search) return list;

    return list.filter(c =>
      c.name.toLowerCase().includes(search) ||
      c.code.toLowerCase().includes(search)
    );
  });

  readonly isCountryValid = computed(() =>
    this.form.controls.billing.get('country')?.valid
  );

  readonly summary = computed(() => {
    const cart = this.cartService;

    return {
      items: cart.items().map(item => ({
        label: item.productName,
        quantity: item.quantity,
        billingCycle: item.billingCycle,
        total: cart.getLineTotal(item)
      })),
      subtotalHt: cart.subtotalHt(),
      vatAmount: cart.vatAmount(),
      totalTtc: cart.totalTtc(),
      currency: cart.currency()
    };
  });

  readonly isSubmitDisabled = computed(() => {
    if (!this.isLogged()) return true;
    if (this.isLoading()) return true;

    const billingOk =
      this.addressMode() === 'saved'
        ? !!this.selectedAddress()
        : this.billingValid() && this.isCountryValid();

    const paymentOk =
      this.paymentMode() === 'saved'
        ? !!this.selectedPayment()
        : this.paymentValid() && this.stripeCardValid();

    return !(billingOk && paymentOk);
  });

  constructor() {
    this.initCountries();
    this.initFormsEffects();
    this.initSavedSelectionEffects();
    this.initStripeEffects();
    this.initFormStatus();
    this.initCountry();
    this.initPhoneAutoFormat();
  }

  ngOnInit() {
    this.loadCountries(this.translate.getCurrentLang());
    this.translate.onLangChange.subscribe(e => this.loadCountries(e.lang));
    this.form.controls.billing.updateValueAndValidity();

    if (this.isLogged()) {
      this.userService.getProfile().subscribe({
        next: res => this.profile.set(res.data ?? null),
      });

      this.addressService.getAll().subscribe({
        next: res => {
          this.savedAddresses = res.data ?? [];
          if (this.savedAddresses.length > 0) {
            this.addressMode.set('saved');
            const defaultAddr = this.savedAddresses.find(a => a.isDefault) ?? this.savedAddresses[0];
            this.selectedAddress.set(defaultAddr);
          }
        }
      });

      this.paymentMethodService.getAll().subscribe({
        next: methods => {
          // Expired cards can no longer authorise off-session — hide them from
          // the checkout selector entirely. The user can still see and manage
          // them from /account/payment-methods (where they appear with a
          // visible "Expirée" badge and a path to delete/replace).
          const usable = methods.filter(m => !this.isCardExpired(m.expMonth, m.expYear));
          this.savedPayments = [...usable].sort((a, b) => Number(b.isDefault) - Number(a.isDefault));
          if (this.savedPayments.length > 0) {
            this.paymentMode.set('saved');
            const defaultCard = this.savedPayments.find(m => m.isDefault) ?? this.savedPayments[0];
            this.selectedPayment.set(defaultCard);
          }
        }
      });
    }
  }

  async ngAfterViewInit() {
    const stripe = await loadStripe(environment.stripePublishableKey);
    if (!stripe) throw new Error('Stripe failed to load');

    this.stripe = stripe;
    this.elements = stripe.elements();

    this.createStripeElements();

    if (this.paymentMode() === 'new') {
      this.mountStripeElements();
    }
  }

  private initCountries() {
    countries.registerLocale(frLocale);
    countries.registerLocale(enLocale);
  }

  private initFormStatus() {
    this.form.controls.billing.statusChanges.subscribe(() => {
      this.billingValid.set(this.form.controls.billing.valid);
    });

    this.form.controls.payment.statusChanges.subscribe(() => {
      this.paymentValid.set(this.form.controls.payment.valid);
    });
  }

  private initCountry() {
    const countryControl = this.form.controls.billing.get('country')!;

    this.countryCode.set(countryControl.value ?? 'FR');

    effect(() => {
      const code = this.countryCode();
      const country = this.countryList().find(c => c.code === code);

      if (country) {
        this.countrySearch.set(country.name);
      }
    });

    countryControl.valueChanges.subscribe(value => {
      this.countryCode.set(value ?? 'FR');
    });

    countryControl.addValidators(control => {
      const value = control.value;
      const valid = this.countryList().some(c => c.code === value);

      return valid ? null : { invalidCountry: true };
    });

    const phoneControl = this.form.controls.billing.get('phone');
    phoneControl?.addValidators(
      phoneValidator(() => countryControl?.value)
    );
  }

  private initPhoneAutoFormat() {
    const phoneControl = this.form.controls.billing.get('phone')!;
    const countryControl = this.form.controls.billing.get('country')!;

    let lastValue = '';

    const format = () => {
      const value = phoneControl.value;
      const countryCode = countryControl.value;

      if (!value || !countryCode) return;

      const phone = parsePhoneNumberFromString(value, countryCode as any);

      if (!phone) return;

      const formatted = phone.formatInternational();

      if (formatted !== value && formatted !== lastValue) {
        lastValue = formatted;
        phoneControl.setValue(formatted, { emitEvent: false });
        phoneControl.updateValueAndValidity({ emitEvent: false });
      }
    };

    phoneControl.valueChanges.subscribe(format);
    countryControl.valueChanges.subscribe(format);
  }

  private initFormsEffects() {
    effect(() => this.toggleForm(this.form.controls.billing, this.addressMode()));
    effect(() => this.toggleForm(this.form.controls.payment, this.paymentMode()));

    effect(() => {
      if (this.addressMode() === 'new') {
        this.form.controls.billing.reset();
        this.form.controls.billing.patchValue({ country: 'FR' });

        this.form.controls.billing.markAsPristine();
        this.form.controls.billing.markAsUntouched();
        // Switching back to a brand-new address re-arms the default consent
        // (saved) so the user doesn't have to re-tick it on each retry.
        this.saveAddressConsent.set(true);
      } else {
        this.saveAddressConsent.set(false);
      }
    });

    effect(() => {
      // Switching to a saved card clears any pending consent on the new-card
      // path so we don't accidentally re-save the previously typed card.
      if (this.paymentMode() === 'saved') {
        this.saveCardConsent.set(false);
      }
    });
  }

  private initSavedSelectionEffects() {
    effect(() => {
      if (this.paymentMode() === 'saved' && !this.selectedPayment() && this.savedPayments.length) {
        this.selectedPayment.set(this.savedPayments[0]);
      }
    });
  }

  private initStripeEffects() {
    effect(() => {
      const mode = this.paymentMode();
      if (!this.elements || !this.cardNumber) return;

      if (mode === 'saved') {
        this.unmountStripeElements();
        this.resetStripeState();
        return;
      }

      setTimeout(() => this.mountStripeElements());
    });
  }

  private loadCountries(lang: string) {
    const list = Object.entries(
      countries.getNames(lang, { select: 'official' }) as Record<string, string>
    )
      .map(([code, name]) => ({ code, name }))
      .sort((a, b) => a.name.localeCompare(b.name, lang));

    this.countryList.set(list);
  }

  private toggleForm(group: AbstractControl, mode: Mode) {
    if (mode === 'new') {
      group.enable();
    } else {
      group.disable();
    }

    group.updateValueAndValidity();
  }

  private createStripeElements() {
    const style = {
      base: {
        fontSize: '14px',
        color: '#111827',
        '::placeholder': { color: '#9ca3af' }
      }
    };

    this.cardNumber = this.elements.create('cardNumber', { style });
    this.cardExpiry = this.elements.create('cardExpiry', { style });
    this.cardCvc = this.elements.create('cardCvc', { style });

    this.cardNumber.on('change', e => {
      this.stripeNumberComplete.set(e.complete);

      this.stripeErrors.update(err => ({
        ...err,
        number: this.mapStripeError(e.error)
      }));
    });

    this.cardExpiry.on('change', e => {
      this.stripeExpiryComplete.set(e.complete);

      this.stripeErrors.update(err => ({
        ...err,
        expiry: this.mapStripeError(e.error)
      }));
    });

    this.cardCvc.on('change', e => {
      this.stripeCvcComplete.set(e.complete);

      this.stripeErrors.update(err => ({
        ...err,
        cvc: this.mapStripeError(e.error)
      }));
    });
  }

  private mapStripeError(error: any): string | null {
    if (!error) return null;

    switch (error.code) {
      case 'incomplete_number':
        return this.translate.instant('error.payment.incomplete-number');
      case 'invalid_number':
        return this.translate.instant('error.payment.invalid-number');
      case 'incomplete_expiry':
        return this.translate.instant('error.payment.incomplete-expiry');
      case 'invalid_expiry_year_past':
        return this.translate.instant('error.payment.expiry-past');
      case 'incomplete_cvc':
        return this.translate.instant('error.payment.incomplete-cvc');
      default:
        return this.translate.instant('error.invalid-field');
    }
  }

  private mountStripeElements() {
    this.cardNumber?.mount('#card-number-element');
    this.cardExpiry?.mount('#card-expiry-element');
    this.cardCvc?.mount('#card-cvc-element');
  }

  private unmountStripeElements() {
    this.cardNumber?.unmount();
    this.cardExpiry?.unmount();
    this.cardCvc?.unmount();
  }

  private resetStripeState() {
    this.stripeNumberComplete.set(false);
    this.stripeExpiryComplete.set(false);
    this.stripeCvcComplete.set(false);

    this.stripeErrors.set({
      number: null,
      expiry: null,
      cvc: null,
    });
  }

  toggleAddressDropdown() {
    this.isAddressOpen.update(v => !v);
  }

  togglePaymentDropdown() {
    this.isPaymentOpen.update(v => !v);
  }

  closeDropdowns() {
    setTimeout(() => {
      this.isAddressOpen.set(false);
      this.isPaymentOpen.set(false);
      this.isCountryOpen.set(false);
    }, 150);
  }

  selectAddress(addr: AddressResponse, event: Event) {
    event.stopPropagation();
    this.selectedAddress.set(addr);
    this.isAddressOpen.set(false);
  }

  selectPayment(card: SavedPaymentMethod, event: Event) {
    event.stopPropagation();
    this.selectedPayment.set(card);
    this.isPaymentOpen.set(false);
  }

  selectCountry(country: Country, event: Event) {
    event.stopPropagation();

    this.form.controls.billing.patchValue({
      country: country.code
    });

    this.countryCode.set(country.code);
    this.countrySearch.set(country.name);

    this.isCountryOpen.set(false);
  }

  private selectCountryInternal(country: Country) {
    this.form.controls.billing.patchValue({
      country: country.code
    });

    this.countryCode.set(country.code);
    this.countrySearch.set(country.name);
  }

  onCountrySearch(value: string) {
    this.countrySearch.set(value);
    this.isCountryOpen.set(true);

    const normalized = value.toLowerCase().trim();

    const match = this.countryList().find(c =>
      c.name.toLowerCase() === normalized
    );

    if (match) {
      this.form.controls.billing.get('country')?.setValue(match.code);
      this.countryCode.set(match.code);
    } else {
      this.form.controls.billing.get('country')?.setValue(null);
      this.countryCode.set('');
    }
  }

  onCountryBlur() {
    const control = this.form.controls.billing.get('country');
    control?.markAsTouched();

    const value = this.countrySearch().toLowerCase().trim();
    const matches = this.countryList().filter(c =>
      c.name.toLowerCase().includes(value)
    );

    if (matches.length === 1) {
      this.selectCountryInternal(matches[0]);
    }

    this.closeDropdowns();
  }

  getError(controlName: string, group: 'billing' | 'payment' = 'billing'): string | null {
    const control = (this.form.controls[group] as any).get(controlName);

    if (!control || !control.touched || !control.errors) {
      return null;
    }
    if (control.errors['invalidCountry']) return this.translate.instant('error.billing.invalid-country');
    if (control.errors['required']) return this.translate.instant('error.mandatory-field');
    if (control.errors['pattern']) return this.translate.instant('error.invalid-format');
    if (control.errors['phoneInvalid']) return this.translate.instant('error.billing.invalid-phone-number');

    return this.translate.instant('error.invalid-field');
  }

  async submit(): Promise<void> {
    this.form.markAllAsTouched();

    if (this.isSubmitDisabled()) return;

    this.isLoading.set(true);
    this.submitError.set(null);

    try {
      // 1. Create the Order from the cart. Lines may now mix MONTHLY and ANNUAL —
      //    each will become its own Stripe Subscription in step 4.
      const cartItems = this.cartService.items();
      const lines = cartItems.map(item => ({
        productId: item.productId,
        billingCycle: item.billingCycle,
        quantity: item.quantity,
      }));
      const orderId = await firstValueFrom(this.orderService.createOrder(lines));

      // 2. Initiate the V14 checkout: backend creates a SetupIntent the frontend
      //    will use to collect a PaymentMethod with Stripe.js — no charge yet.
      const initiated = await firstValueFrom(this.paymentService.initiatePayment(orderId));

      // 3. Resolve the PaymentMethod: either reuse a saved one, or collect a new
      //    card via the SetupIntent. For saved cards we skip Stripe entirely —
      //    the PaymentMethod is already attached to the customer in Stripe.
      const useSavedCard = this.paymentMode() === 'saved' && this.selectedPayment() !== null;
      let paymentMethodId: string;

      if (useSavedCard) {
        paymentMethodId = this.selectedPayment()!.stripePaymentMethodId;
      } else {
        const { setupIntent, error } = await this.stripe.confirmCardSetup(
          initiated.setupIntentClientSecret,
          { payment_method: { card: this.cardNumber, billing_details: this.buildBillingDetails() } }
        );
        if (error) {
          this.submitError.set(this.mapStripePaymentError(error));
          return;
        }
        const pm = setupIntent?.payment_method;
        const resolvedPmId = typeof pm === 'string' ? pm : pm?.id;
        if (!resolvedPmId) {
          // SetupIntent succeeded but Stripe didn't return a PaymentMethod —
          // shouldn't happen in practice (Stripe.js would have given us an error
          // first), but bail out cleanly rather than calling finalize with empty.
          this.submitError.set(this.translate.instant('error.payment.generic'));
          return;
        }
        paymentMethodId = resolvedPmId;
      }

      // 4. Finalize: backend creates one Stripe Subscription per OrderLine using
      //    the PaymentMethod we just produced. Each first invoice is charged
      //    immediately off-session; the response tells us which lines went
      //    through cleanly (`active`) vs which need user attention (`incomplete`,
      //    typically 3DS abandoned or card declined).
      const finalized = await firstValueFrom(
        this.paymentService.finalizePayment(orderId, paymentMethodId)
      );

      const incompleteLines = finalized.lines.filter(l => l.stripeStatus === 'incomplete');
      if (incompleteLines.length > 0) {
        // At least one sub couldn't be charged inline. The Order is still marked
        // PAID (we treat the checkout as completed once any line succeeded), and
        // the confirmation page + the account/subscriptions page will surface
        // the incomplete lines so the user can resolve them.
        // We still cart-clear and navigate so the user doesn't lose their way.
      }

      // Persist the typed billing address and/or card into the user account if
      // they consented. Best-effort: a failure here must not block the success
      // navigation — the order has already been paid and the subscriptions
      // created. We just log and move on.
      await this.persistCheckoutInputsBestEffort(paymentMethodId, useSavedCard);

      this.cartService.clear();
      void this.router.navigate(['/checkout/success', orderId]);
    } catch (err) {
      this.submitError.set(this.mapBackendError(err));
    } finally {
      this.isLoading.set(false);
    }
  }

  private buildBillingDetails() {
    const inline = this.form.controls.billing.getRawValue();
    const saved = this.selectedAddress();
    const useSaved = this.addressMode() === 'saved' && saved !== null;

    return {
      name: this.form.controls.payment.value.holder
        ?? `${useSaved ? saved!.firstName : inline.firstName} ${useSaved ? saved!.lastName : inline.lastName}`,
      email: this.authService.user()?.email,
      address: {
        line1: useSaved ? saved!.address : (inline.address ?? ''),
        line2: (useSaved ? saved!.address2 : inline.address2) ?? undefined,
        postal_code: useSaved ? saved!.zipCode : (inline.zipCode ?? ''),
        city: useSaved ? saved!.city : (inline.city ?? ''),
        country: useSaved ? saved!.countryCode : (inline.country ?? ''),
      },
    };
  }

  private mapBackendError(err: unknown): string {
    if (err instanceof HttpErrorResponse) {
      const code: string | undefined = err.error?.error?.code;
      switch (code) {
        case 'ORDER_NOT_FOUND':
          return this.translate.instant('error.payment.order-not-found');
        case 'ORDER_NOT_PAYABLE':
          return this.translate.instant('error.payment.order-not-payable');
        case 'PAYMENT_NOT_INITIATED':
        case 'PAYMENT_NOT_FINALIZABLE':
          return this.translate.instant('error.payment.order-not-payable');
        case 'NO_STRIPE_CUSTOMER':
        case 'USER_NOT_FOUND':
          return this.translate.instant('error.payment.user-not-found');
        case 'STRIPE_ERROR':
          return this.translate.instant('error.payment.stripe-error');
      }
    }
    return this.translate.instant('error.payment.generic');
  }

  /**
   * Save the typed address / new card into the user account when the user
   * consented via the checkout checkboxes. Strictly best-effort: errors are
   * logged and swallowed so we always navigate to the confirmation page.
   */
  private async persistCheckoutInputsBestEffort(paymentMethodId: string, useSavedCard: boolean): Promise<void> {
    if (!this.isLogged()) return;

    if (this.addressMode() === 'new' && this.saveAddressConsent()) {
      try {
        await firstValueFrom(this.addressService.create(this.buildAddressPayloadFromForm()));
      } catch (err) {
        console.warn('[checkout] Failed to persist billing address', err);
      }
    }

    if (!useSavedCard && this.saveCardConsent()) {
      try {
        await firstValueFrom(this.paymentMethodService.save(paymentMethodId));
        // GDPR proof-of-consent: record the explicit "Reuse this card" tick.
        // Sequenced after save() — if the save failed there's no card to
        // attach the consent to. Best-effort: a logging failure must not
        // block the user from completing their purchase.
        try {
          await firstValueFrom(this.consentLogService.logPaymentMethodConsent(paymentMethodId));
        } catch (err) {
          console.warn('[checkout] Failed to log payment method consent', err);
        }
      } catch (err) {
        console.warn('[checkout] Failed to persist saved payment method', err);
      }
    }
  }

  /** Mirrors {@link PaymentMethodsComponent.isExpired} — kept private here to avoid a circular import. */
  private isCardExpired(expMonth: string, expYear: string): boolean {
    const month = Number(expMonth);
    const year = Number(expYear);
    if (!Number.isFinite(month) || !Number.isFinite(year) || month < 1 || month > 12) {
      return false;
    }
    const endOfMonth = new Date(Date.UTC(year, month, 0, 23, 59, 59));
    return endOfMonth.getTime() < Date.now();
  }

  private buildAddressPayloadFromForm() {
    const inline = this.form.controls.billing.getRawValue();
    return {
      firstName: inline.firstName ?? '',
      lastName: inline.lastName ?? '',
      label: this.translate.instant('checkout.billing.saveDefaultLabel'),
      address: inline.address ?? '',
      address2: inline.address2 || null,
      zipCode: inline.zipCode ?? '',
      city: inline.city ?? '',
      region: inline.region ?? '',
      countryCode: inline.country ?? 'FR',
      phone: inline.phone ?? '',
      company: inline.company || null,
      vatNumber: inline.vatNumber || null,
    };
  }

  private mapStripePaymentError(error: any): string {
    switch (error?.code) {
      case 'card_declined':
        return this.translate.instant('error.payment.card-declined');
      case 'insufficient_funds':
        return this.translate.instant('error.payment.insufficient-funds');
      case 'incorrect_cvc':
        return this.translate.instant('error.payment.incorrect-cvc');
      case 'expired_card':
        return this.translate.instant('error.payment.expired-card');
      case 'authentication_required':
        return this.translate.instant('error.payment.authentication-required');
      default:
        return error?.message ?? this.translate.instant('error.payment.generic');
    }
  }
}
