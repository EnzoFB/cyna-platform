import {Component, computed, effect, inject, signal} from '@angular/core';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {CartService} from "../../core/services/cart.service";
import {AuthService} from "../../core/services/auth.service";
import {OrderSummaryComponent} from "../../shared/components/order-summary/order-summary.component";
import {TranslatePipe} from "@ngx-translate/core";

type Mode = 'new' | 'saved';

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
export class CheckoutComponent {
  private readonly fb = inject(FormBuilder);
  protected readonly cartService = inject(CartService);

  addressMode = signal<Mode>('new');
  paymentMode = signal<Mode>('new');

  isLogged = false;

  savedAddresses: any[] = [];
  savedPayments: any[] = [];

  readonly form = this.fb.group({
    billing: this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      address: ['', Validators.required],
      address2: [''],
      zipCode: ['', Validators.required],
      city: ['', Validators.required],
      region: [''],
      country: ['', Validators.required],
      phone: ['', Validators.required],
    }),
    payment: this.fb.group({
      cardNumber: ['', Validators.required],
      expiry: ['', Validators.required],
      cvv: ['', Validators.required],
      holder: ['', Validators.required],
    }),
  });

  readonly totalTtc = this.cartService.totalTtc;

  readonly isSubmitDisabled = computed(() => {
    if (!this.isLogged) return true;

    if (this.addressMode() === 'new' && this.form.controls.billing.invalid) {
      return true;
    }

    return this.paymentMode() === 'new' && this.form.controls.payment.invalid;
  });

  readonly summary = computed(() => ({
    items: this.cartService.items().map(item => ({
      label: item.productName,
      quantity: item.quantity,
      billingCycle: item.billingCycle,
      total: this.cartService.getLineTotal(item)
    })),
    subtotalHt: this.cartService.subtotalHt(),
    vatAmount: this.cartService.vatAmount(),
    totalTtc: this.cartService.totalTtc(),
    currency: this.cartService.currency()
  }));


  constructor(private authService: AuthService) {
    effect(() => {
      this.isLogged = this.authService.isAuthenticated();
    });

    effect(() => {
      if (this.addressMode() === 'new') {
        this.form.controls.billing.reset();
        this.form.controls.billing.enable();
      } else {
        this.form.controls.billing.disable();
      }
    });

    effect(() => {
      if (this.paymentMode() === 'new') {
        this.form.controls.payment.reset();
        this.form.controls.payment.enable();
      } else {
        this.form.controls.payment.disable();
      }
    });

    this.savedAddresses = [
      {
        id: 1,
        label: 'Domicile',
        firstName: 'Jean',
        lastName: 'Dupont',
        address: '12 rue de la Paix',
        zipCode: '75001',
        city: 'Paris',
        country: 'France',
        phone: '+33612345678'
      },
      {
        id: 2,
        label: 'Bureau',
        firstName: 'Jean',
        lastName: 'Dupont',
        address: '5 avenue de l\'Opéra',
        zipCode: '75002',
        city: 'Paris',
        country: 'France',
        phone: '+33600000000'
      }
    ];

    this.savedPayments = [
      { id: 1, brand: 'Visa', last4: '4242', holder: 'Jean Dupont' },
      { id: 2, brand: 'Mastercard', last4: '4444', holder: 'Will Smith'}
    ];
  }

  submit(): void {
    if (this.isSubmitDisabled()) return;

    console.log({
      addressMode: this.addressMode(),
      paymentMode: this.paymentMode(),
      form: this.form.value,
    });
  }

  onSelectAddress(event: Event) {
    const id = Number((event.target as HTMLSelectElement).value);
    const addr = this.savedAddresses.find(a => a.id === id);
    if (!addr) return;

    this.form.controls.billing.patchValue(addr);
  }

  onSelectPayment(event: Event) {
    const id = Number((event.target as HTMLSelectElement).value);
    const card = this.savedPayments.find(c => c.id === id);
    if (!card) return;

    this.form.controls.payment.patchValue({
      cardNumber: '**** **** **** ' + card.last4,
      holder: card.holder
    });
  }
}
