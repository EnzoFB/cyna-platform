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
import { parsePhoneNumberFromString } from 'libphonenumber-js';
import { loadStripe, Stripe, StripeElements, StripeCardNumberElement, StripeCardExpiryElement, StripeCardCvcElement } from '@stripe/stripe-js';
import * as countries from 'i18n-iso-countries';
import frLocale from 'i18n-iso-countries/langs/fr.json';
import enLocale from 'i18n-iso-countries/langs/en.json';

import { CartService } from '../../core/services/cart.service';
import { AuthService } from '../../core/services/auth.service';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { OrderSummaryComponent } from '../../shared/components/order-summary/order-summary.component';

type Mode = 'new' | 'saved';
type Country = { code: string; name: string };

@Component({
  selector: 'app-checkout',
  imports: [
    ReactiveFormsModule,
    OrderSummaryComponent,
    TranslatePipe
  ],
  templateUrl: './checkout.component.html',
  styleUrl: './checkout.component.scss',
})
export class CheckoutComponent implements OnInit, AfterViewInit {

  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  protected readonly cartService = inject(CartService);
  private readonly translate = inject(TranslateService);

  stripe!: Stripe;
  elements!: StripeElements;
  cardNumber!: StripeCardNumberElement;
  cardExpiry!: StripeCardExpiryElement;
  cardCvc!: StripeCardCvcElement;

  stripeCardValid = signal(false);

  addressMode = signal<Mode>('new');
  paymentMode = signal<Mode>('new');

  selectedAddress = signal<any | null>(null);
  selectedPayment = signal<any | null>(null);

  isAddressOpen = signal(false);
  isPaymentOpen = signal(false);
  isCountryOpen = signal(false);

  countryList = signal<Country[]>([]);
  countryCode = signal('FR');

  savedAddresses: any[] = [];
  savedPayments: any[] = [];

  readonly form = this.fb.group({
    billing: this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      address: ['', Validators.required],
      address2: [''],
      zipCode: ['', [Validators.required, Validators.pattern(/^[0-9A-Za-z -]{3,10}$/)]],
      city: ['', [Validators.required, Validators.minLength(2)]],
      region: ['', Validators.required],
      country: ['FR', Validators.required],
      phone: ['', Validators.required],
    }),
    payment: this.fb.group({
      holder: ['', [Validators.required, Validators.minLength(2)]],
    }),
  });

  formValid = signal(false);

  readonly isLogged = computed(() => this.authService.isAuthenticated());

  readonly totalTtc = this.cartService.totalTtc;

  readonly selectedCountry = computed(() => {
    const code = this.countryCode();
    return this.countryList().find(c => c.code === code) ?? null;
  });

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

    const billingOk =
      this.addressMode() === 'saved'
        ? !!this.selectedAddress()
        : this.formValid();

    const paymentOk =
      this.paymentMode() === 'saved'
        ? !!this.selectedPayment()
        : this.formValid() && this.stripeCardValid();

    return !(billingOk && paymentOk);
  });

  constructor() {
    this.initCountries();
    this.initFormsEffects();
    this.initSavedSelectionEffects();
    this.initStripeEffects();
    this.initMockData();
    this.initFormStatusSync();
    this.initCountrySync();
    this.initPhoneAutoFormat();
  }

  ngOnInit() {
    this.loadCountries(this.translate.getCurrentLang());

    this.translate.onLangChange.subscribe(e => {
      this.loadCountries(e.lang);
    });

    this.form.controls.billing.updateValueAndValidity();
  }

  async ngAfterViewInit() {
    const stripe = await loadStripe('pk_test_xxx');
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

  private initFormStatusSync() {
    this.form.statusChanges.subscribe(s => {
      this.formValid.set(s === 'VALID');
    });
  }

  private initCountrySync() {
    const control = this.form.controls.billing.get('country')!;

    this.countryCode.set(control.value ?? 'FR');

    control.valueChanges.subscribe(value => {
      this.countryCode.set(value ?? 'FR');
    });
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
        const control = this.form.controls.billing.get('country');
        if (!control?.value) {
          control?.setValue('FR', { emitEvent: false });
        }
      }
    });
  }

  private initSavedSelectionEffects() {
    effect(() => {
      if (this.addressMode() === 'saved' && !this.selectedAddress() && this.savedAddresses.length) {
        this.selectAddressInternal(this.savedAddresses[0]);
      }

      if (this.paymentMode() === 'saved' && !this.selectedPayment() && this.savedPayments.length) {
        this.selectPaymentInternal(this.savedPayments[0]);
      }
    });
  }

  private initStripeEffects() {
    effect(() => {
      const mode = this.paymentMode();
      if (!this.elements || !this.cardNumber) return;

      if (mode === 'saved') {
        this.unmountStripeElements();
        return;
      }

      setTimeout(() => this.mountStripeElements());
    });
  }

  private initMockData() {
    this.savedAddresses = [
      {
        id: 1,
        label: 'Domicile',
        firstName: 'Jean',
        lastName: 'Dupont',
        address: '12 rue de la Paix',
        zipCode: '75001',
        region: 'IDF',
        city: 'Paris',
        country: 'FR',
        phone: '+33612345678'
      }
    ];

    this.savedPayments = [
      { id: 1, brand: 'Visa', last4: '4242', holder: 'Jean Dupont' }
    ];
  }

  private loadCountries(lang: string) {
    const list = Object.entries(
      countries.getNames(lang, { select: 'official' })
    )
      .map(([code, name]) => ({ code, name }))
      .sort((a, b) => a.name.localeCompare(b.name, lang));

    this.countryList.set(list);
  }

  private toggleForm(group: AbstractControl, mode: Mode) {
    mode === 'new'
      ? group.enable({ emitEvent: false })
      : group.disable({ emitEvent: false });

    this.form.updateValueAndValidity({ emitEvent: false });
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

    let number = false, expiry = false, cvc = false;

    const update = () =>
      this.stripeCardValid.set(number && expiry && cvc);

    this.cardNumber.on('change', e => {
      number = e.complete;
      update();
    });
    this.cardExpiry.on('change', e => {
      expiry = e.complete;
      update();
    });
    this.cardCvc.on('change', e => {
      cvc = e.complete;
      update();
    });
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

  toggleAddressDropdown() {
    this.isAddressOpen.update(v => !v);
  }

  togglePaymentDropdown() {
    this.isPaymentOpen.update(v => !v);
  }

  toggleCountryDropdown() {
    this.isCountryOpen.update(v => !v);
  }

  closeDropdowns() {
    setTimeout(() => {
      this.isAddressOpen.set(false);
      this.isPaymentOpen.set(false);
      this.isCountryOpen.set(false);
    }, 150);
  }

  selectAddress(addr: any, event: Event) {
    event.stopPropagation();
    this.selectAddressInternal(addr);
    this.isAddressOpen.set(false);
  }

  private selectAddressInternal(addr: any) {
    this.selectedAddress.set(addr);
    this.form.controls.billing.patchValue(addr);
  }

  selectPayment(card: any, event: Event) {
    event.stopPropagation();
    this.selectPaymentInternal(card);
    this.isPaymentOpen.set(false);
  }

  private selectPaymentInternal(card: any) {
    this.selectedPayment.set(card);
  }

  selectCountry(country: Country, event: Event) {
    event.stopPropagation();
    this.form.controls.billing.get('country')?.setValue(country.code);
    this.isCountryOpen.set(false);
  }

  submit(): void {
    if (this.isSubmitDisabled()) return;

    const address = this.addressMode() === 'new'
      ? this.form.controls.billing.getRawValue()
      : this.selectedAddress();

    const payment = this.paymentMode() === 'new'
      ? {
        holder: this.form.controls.payment.value.holder,
        stripeCard: 'stripe-element'
      }
      : this.selectedPayment();

    console.log('CHECKOUT DATA', {
      addressMode: this.addressMode(),
      paymentMode: this.paymentMode(),
      address,
      payment,
      cart: this.cartService.items(),
      total: this.cartService.totalTtc()
    });
  }
}
