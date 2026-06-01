import {
  ChangeDetectionStrategy, ChangeDetectorRef, Component, computed,
  effect, EventEmitter, inject, Input, OnChanges, OnInit, Output, signal, SimpleChanges
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import frLocale from 'i18n-iso-countries/langs/fr.json';
import enLocale from 'i18n-iso-countries/langs/en.json';
import { parsePhoneNumberFromString } from 'libphonenumber-js/min';
import { phoneValidator } from '../../../shared/validators/phone.validator';
import { AddressPayload, AddressResponse } from '../../../core/models/address.model';

interface Country { code: string; name: string }
interface CountryLocalePayload {
  readonly locale: string;
  readonly countries: Record<string, string | string[]>;
}

@Component({
  selector: 'app-address-form-modal',
  standalone: true,
  imports: [ReactiveFormsModule, TranslatePipe],
  templateUrl: './address-form-modal.component.html',
  styleUrl: './address-form-modal.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AddressFormModalComponent implements OnInit, OnChanges {
  @Input() isOpen = false;
  @Input() address: AddressResponse | null = null;
  @Output() closed = new EventEmitter<void>();
  @Output() saved = new EventEmitter<AddressPayload>();

  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly countryList = signal<Country[]>([]);
  readonly countrySearch = signal('');
  readonly countryCode = signal('FR');
  readonly isCountryOpen = signal(false);

  readonly filteredCountries = computed(() => {
    const search = this.countrySearch().toLowerCase().trim();
    return search
      ? this.countryList().filter(c => c.name.toLowerCase().includes(search) || c.code.toLowerCase().includes(search))
      : this.countryList();
  });

  readonly selectedCountry = computed(() =>
    this.countryList().find(c => c.code === this.countryCode()) ?? null
  );

  readonly form = this.fb.group({
    firstName: ['', Validators.required],
    lastName:  ['', Validators.required],
    label:     ['', Validators.required],
    address:   ['', Validators.required],
    address2:  [''],
    city:      ['', Validators.required],
    zipCode:   ['', [Validators.required, Validators.pattern(/^[0-9A-Za-z -]{3,10}$/)]],
    region:    ['', Validators.required],
    country:   ['FR', Validators.required],
    phone:     ['', Validators.required],
    company:   [''],
    vatNumber: [''],
  });

  get isEditMode(): boolean { return this.address !== null; }

  constructor() {
    effect(() => {
      const code = this.countryCode();
      const country = this.countryList().find(c => c.code === code);
      if (country) this.countrySearch.set(country.name);
    });
  }

  ngOnInit(): void {
    this.loadCountries(this.translate.getCurrentLang());
    this.translate.onLangChange.subscribe(e => this.loadCountries(e.lang));

    const countryCtrl = this.form.get('country')!;
    countryCtrl.addValidators(ctrl =>
      this.countryList().some(c => c.code === ctrl.value) ? null : { invalidCountry: true }
    );

    const phoneCtrl = this.form.get('phone')!;
    phoneCtrl.addValidators(phoneValidator(() => countryCtrl.value));

    let lastPhone = '';
    phoneCtrl.valueChanges.subscribe(value => {
      if (!value || !countryCtrl.value) return;
      const parsed = parsePhoneNumberFromString(value, countryCtrl.value as any);
      if (!parsed) return;
      const formatted = parsed.formatInternational();
      if (formatted !== value && formatted !== lastPhone) {
        lastPhone = formatted;
        phoneCtrl.setValue(formatted, { emitEvent: false });
        phoneCtrl.updateValueAndValidity({ emitEvent: false });
      }
    });

    countryCtrl.valueChanges.subscribe(val => this.countryCode.set(val ?? 'FR'));
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen'] && this.isOpen) {
      if (this.address) {
        this.form.patchValue({
          firstName: this.address.firstName,
          lastName:  this.address.lastName,
          label:     this.address.label,
          address:   this.address.address,
          address2:  this.address.address2 ?? '',
          city:      this.address.city,
          zipCode:   this.address.zipCode,
          region:    this.address.region,
          country:   this.address.countryCode,
          phone:     this.address.phone,
          company:   this.address.company ?? '',
          vatNumber: this.address.vatNumber ?? '',
        });
        this.countryCode.set(this.address.countryCode);
      } else {
        this.form.reset({ country: 'FR' });
        this.countryCode.set('FR');
      }
      this.form.markAsPristine();
      this.form.markAsUntouched();
    }
  }

  get canSave(): boolean { return this.form.valid; }

  submit(): void {
    if (!this.canSave) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    this.saved.emit({
      firstName:   v.firstName!,
      lastName:    v.lastName!,
      label:       v.label!,
      address:     v.address!,
      address2:    v.address2 || null,
      zipCode:     v.zipCode!,
      city:        v.city!,
      region:      v.region!,
      countryCode: v.country!,
      phone:       v.phone!,
      company:     v.company || null,
      vatNumber:   v.vatNumber || null,
    });
  }

  close(): void { this.closed.emit(); }

  onCountrySearch(value: string): void {
    this.countrySearch.set(value);
    this.isCountryOpen.set(true);
    const match = this.countryList().find(c => c.name.toLowerCase() === value.toLowerCase().trim());
    if (match) {
      this.form.get('country')?.setValue(match.code);
      this.countryCode.set(match.code);
    } else {
      this.form.get('country')?.setValue(null);
      this.countryCode.set('');
    }
  }

  selectCountry(country: Country, event: Event): void {
    event.stopPropagation();
    this.form.get('country')?.setValue(country.code);
    this.countryCode.set(country.code);
    this.countrySearch.set(country.name);
    this.isCountryOpen.set(false);
  }

  onCountryBlur(): void {
    setTimeout(() => {
      const value = this.countrySearch().toLowerCase().trim();
      const matches = this.countryList().filter(c => c.name.toLowerCase().includes(value));
      if (matches.length === 1) {
        this.form.get('country')?.setValue(matches[0].code);
        this.countryCode.set(matches[0].code);
        this.countrySearch.set(matches[0].name);
      }
      this.isCountryOpen.set(false);
    }, 150);
  }

  getError(field: string): string | null {
    const ctrl = this.form.get(field);
    if (!ctrl?.touched || !ctrl.errors) return null;
    if (ctrl.errors['required'])       return this.translate.instant('error.mandatory-field');
    if (ctrl.errors['pattern'])        return this.translate.instant('error.invalid-format');
    if (ctrl.errors['invalidCountry']) return this.translate.instant('error.billing.invalid-country');
    if (ctrl.errors['phoneInvalid'])   return this.translate.instant('error.billing.invalid-phone-number');
    return null;
  }

  private loadCountries(lang: string): void {
    const localePayload = this.resolveCountryLocale(lang);
    const list = Object.entries(localePayload.countries)
      .map(([code, name]) => ({ code, name: Array.isArray(name) ? name[0] : name }))
      .filter((country): country is Country => Boolean(country.name))
      .sort((a, b) => a.name.localeCompare(b.name, localePayload.locale));
    this.countryList.set(list);
    this.cdr.markForCheck();
  }

  private resolveCountryLocale(lang: string): CountryLocalePayload {
    return lang.startsWith('en')
      ? (enLocale as CountryLocalePayload)
      : (frLocale as CountryLocalePayload);
  }
}
