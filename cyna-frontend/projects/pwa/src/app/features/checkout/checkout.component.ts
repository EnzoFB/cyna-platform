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

import { CartService } from '../../core/services/cart.service';
import { AuthService } from '../../core/services/auth.service';
import { AddressService } from '../../core/services/address.service';
import { AddressResponse } from '../../core/models/address.model';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { OrderSummaryComponent } from '../../shared/components/order-summary/order-summary.component';
import {phoneValidator} from "../../shared/validators/phone.validator";

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

  selectedAddress = signal<AddressResponse | null>(null);
  selectedPayment = signal<any | null>(null);

  isAddressOpen = signal(false);
  isPaymentOpen = signal(false);
  isCountryOpen = signal(false);

  billingValid = signal(false);
  paymentValid = signal(false);

  countryList = signal<Country[]>([]);
  countryCode = signal('FR');
  countrySearch = signal('');

  savedAddresses: AddressResponse[] = [];
  savedPayments: any[] = [];

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
  readonly totalTtc = this.cartService.totalTtc;

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
    this.initMockData();
    this.initFormStatus();
    this.initCountry();
    this.initPhoneAutoFormat();
  }

  ngOnInit() {
    this.loadCountries(this.translate.getCurrentLang());
    this.translate.onLangChange.subscribe(e => this.loadCountries(e.lang));
    this.form.controls.billing.updateValueAndValidity();

    if (this.isLogged()) {
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
    }
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
      }
    });
  }

  private initSavedSelectionEffects() {
    effect(() => {
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
        this.resetStripeState();
        return;
      }

      setTimeout(() => this.mountStripeElements());
    });
  }

  private initMockData() {
    this.savedPayments = [
      { id: 1, brand: 'Visa', last4: '4242', holder: 'Jean Dupont' },
      { id: 2, brand: 'Mastercard', last4: '1234', holder: 'John Doe' }
    ];
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

    this.closeDropdowns()
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

  submit(): void {
    this.form.markAllAsTouched();

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
