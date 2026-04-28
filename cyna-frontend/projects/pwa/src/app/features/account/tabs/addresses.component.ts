import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { AccountAddress } from '../models/account.models';

const ADDRESS_STORAGE_KEY = 'cyna_pwa_account_addresses';

const DEFAULT_ADDRESSES: readonly AccountAddress[] = [
  {
    id: 'address-default-1',
    label: 'Intitulé',
    address: '123 Avenue des Champs-Élysées',
    address2: 'Appartement 4B',
    city: 'Paris',
    zipCode: '75008',
    region: 'Ile-de-France',
    country: 'France',
    phone: '+33 1 23 45 67 89',
    isDefault: true
  },
  {
    id: 'address-secondary-2',
    label: 'Intitulé',
    address: '123 Avenue des Champs-Élysées',
    address2: 'Appartement 4B',
    city: 'Paris',
    zipCode: '75008',
    region: 'Ile-de-France',
    country: 'France',
    phone: '+33 1 23 45 67 89',
    isDefault: false
  }
];

@Component({
  selector: 'app-addresses',
  standalone: true,
  imports: [TranslatePipe, ReactiveFormsModule],
  templateUrl: './addresses.component.html',
  styleUrl: './addresses.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AddressesComponent {
  private readonly fb = inject(FormBuilder);

  readonly addresses = signal<readonly AccountAddress[]>(this.loadAddresses());
  readonly modalMode = signal<'create' | 'edit' | null>(null);
  readonly editingAddressId = signal<string | null>(null);

  readonly orderedAddresses = computed(() =>
    [...this.addresses()].sort((a, b) => Number(b.isDefault) - Number(a.isDefault))
  );

  readonly form = this.fb.nonNullable.group({
    label: ['', [Validators.required, Validators.minLength(2)]],
    address: ['', [Validators.required, Validators.minLength(4)]],
    address2: [''],
    city: ['', [Validators.required, Validators.minLength(2)]],
    zipCode: ['', [Validators.required, Validators.pattern(/^[0-9A-Za-z -]{3,10}$/)]],
    region: ['', [Validators.required, Validators.minLength(2)]],
    country: ['', [Validators.required, Validators.minLength(2)]],
    phone: ['', [Validators.required, Validators.minLength(8)]]
  });

  openCreateModal(): void {
    this.modalMode.set('create');
    this.editingAddressId.set(null);
    this.form.reset({
      label: '',
      address: '',
      address2: '',
      city: '',
      zipCode: '',
      region: '',
      country: 'France',
      phone: ''
    });
    this.form.markAsPristine();
    this.form.markAsUntouched();
  }

  openEditModal(address: AccountAddress): void {
    this.modalMode.set('edit');
    this.editingAddressId.set(address.id);
    this.form.reset({
      label: address.label,
      address: address.address,
      address2: address.address2,
      city: address.city,
      zipCode: address.zipCode,
      region: address.region,
      country: address.country,
      phone: address.phone
    });
    this.form.markAsPristine();
    this.form.markAsUntouched();
  }

  closeModal(): void {
    this.modalMode.set(null);
    this.editingAddressId.set(null);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const mode = this.modalMode();
    if (!mode) {
      return;
    }

    const value = this.form.getRawValue();

    if (mode === 'create') {
      const nextAddress: AccountAddress = {
        id: this.newId(),
        label: value.label.trim(),
        address: value.address.trim(),
        address2: value.address2.trim(),
        city: value.city.trim(),
        zipCode: value.zipCode.trim(),
        region: value.region.trim(),
        country: value.country.trim(),
        phone: value.phone.trim(),
        isDefault: this.addresses().length === 0
      };

      const nextAddresses = [nextAddress, ...this.addresses()];
      this.persistAddresses(nextAddresses);
      this.closeModal();
      return;
    }

    const editingId = this.editingAddressId();
    if (!editingId) {
      return;
    }

    const updated = this.addresses().map(address =>
      address.id === editingId
        ? {
            ...address,
            label: value.label.trim(),
            address: value.address.trim(),
            address2: value.address2.trim(),
            city: value.city.trim(),
            zipCode: value.zipCode.trim(),
            region: value.region.trim(),
            country: value.country.trim(),
            phone: value.phone.trim()
          }
        : address
    );

    this.persistAddresses(updated);
    this.closeModal();
  }

  setDefault(id: string): void {
    const next = this.addresses().map(address => ({
      ...address,
      isDefault: address.id === id
    }));

    this.persistAddresses(next);
  }

  remove(id: string): void {
    const current = this.addresses();
    const removedAddress = current.find(address => address.id === id);
    if (!removedAddress) {
      return;
    }

    const next = current.filter(address => address.id !== id);
    if (!next.length) {
      this.persistAddresses([]);
      return;
    }

    if (removedAddress.isDefault && !next.some(address => address.isDefault)) {
      const [first, ...rest] = next;
      this.persistAddresses([{ ...first, isDefault: true }, ...rest]);
      return;
    }

    this.persistAddresses(next);
  }

  isInvalid(controlName: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[controlName];
    return control.invalid && (control.touched || control.dirty);
  }

  private loadAddresses(): readonly AccountAddress[] {
    try {
      const raw = localStorage.getItem(ADDRESS_STORAGE_KEY);
      if (!raw) {
        return DEFAULT_ADDRESSES;
      }

      const parsed = JSON.parse(raw) as unknown;
      if (!Array.isArray(parsed)) {
        return DEFAULT_ADDRESSES;
      }

      const normalized = parsed
        .filter((item): item is AccountAddress => this.isAddress(item))
        .map(item => ({ ...item }));

      if (!normalized.length) {
        return DEFAULT_ADDRESSES;
      }

      if (!normalized.some(item => item.isDefault)) {
        normalized[0] = { ...normalized[0], isDefault: true };
      }

      return normalized;
    } catch {
      return DEFAULT_ADDRESSES;
    }
  }

  private persistAddresses(addresses: readonly AccountAddress[]): void {
    this.addresses.set(addresses);
    localStorage.setItem(ADDRESS_STORAGE_KEY, JSON.stringify(addresses));
  }

  private isAddress(item: unknown): item is AccountAddress {
    if (!item || typeof item !== 'object') {
      return false;
    }

    const candidate = item as Partial<AccountAddress>;
    return (
      typeof candidate.id === 'string' &&
      typeof candidate.label === 'string' &&
      typeof candidate.address === 'string' &&
      typeof candidate.address2 === 'string' &&
      typeof candidate.city === 'string' &&
      typeof candidate.zipCode === 'string' &&
      typeof candidate.region === 'string' &&
      typeof candidate.country === 'string' &&
      typeof candidate.phone === 'string' &&
      typeof candidate.isDefault === 'boolean'
    );
  }

  private newId(): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
      return crypto.randomUUID();
    }

    return `address-${Date.now()}`;
  }
}
