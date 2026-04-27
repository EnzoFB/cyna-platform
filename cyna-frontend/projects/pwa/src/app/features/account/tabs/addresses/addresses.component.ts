import {
  ChangeDetectionStrategy, ChangeDetectorRef, Component, inject, OnInit, signal
} from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AddressService } from '../../../../core/services/address.service';
import { ToastService } from '../../../../core/services/toast.service';
import { AddressPayload, AddressResponse } from '../../../../core/models/address.model';
import { AddressFormModalComponent } from '../../modals/address-form-modal.component';

@Component({
  selector: 'app-addresses',
  standalone: true,
  imports: [TranslatePipe, AddressFormModalComponent],
  templateUrl: './addresses.component.html',
  styleUrl: './addresses.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AddressesComponent implements OnInit {
  private readonly addressService = inject(AddressService);
  private readonly toastService = inject(ToastService);
  private readonly translate = inject(TranslateService);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly addresses = signal<AddressResponse[]>([]);
  readonly loading = signal(false);
  readonly modalOpen = signal(false);
  readonly editingAddress = signal<AddressResponse | null>(null);

  ngOnInit(): void {
    this.loadAddresses();
  }

  loadAddresses(): void {
    this.loading.set(true);
    this.addressService.getAll().subscribe({
      next: res => {
        this.addresses.set(res.data ?? []);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading.set(false);
        this.cdr.markForCheck();
      }
    });
  }

  openAdd(): void {
    this.editingAddress.set(null);
    this.modalOpen.set(true);
  }

  openEdit(addr: AddressResponse): void {
    this.editingAddress.set(addr);
    this.modalOpen.set(true);
  }

  closeModal(): void {
    this.modalOpen.set(false);
  }

  onSaved(payload: AddressPayload): void {
    const editing = this.editingAddress();
    if (editing) {
      this.addressService.update(editing.id, payload).subscribe({
        next: () => {
          this.toastService.showSuccess(this.translate.instant('account.addresses.toast.updated'));
          this.closeModal();
          this.loadAddresses();
        },
        error: () => this.toastService.showError(this.translate.instant('account.addresses.toast.error'))
      });
    } else {
      this.addressService.create(payload).subscribe({
        next: () => {
          this.toastService.showSuccess(this.translate.instant('account.addresses.toast.created'));
          this.closeModal();
          this.loadAddresses();
        },
        error: () => this.toastService.showError(this.translate.instant('account.addresses.toast.error'))
      });
    }
  }

  deleteAddress(id: string): void {
    this.addressService.delete(id).subscribe({
      next: () => {
        this.toastService.showSuccess(this.translate.instant('account.addresses.toast.deleted'));
        this.loadAddresses();
      },
      error: () => this.toastService.showError(this.translate.instant('account.addresses.toast.error'))
    });
  }

  setDefault(id: string): void {
    this.addressService.setDefault(id).subscribe({
      next: () => this.loadAddresses(),
      error: () => this.toastService.showError(this.translate.instant('account.addresses.toast.error'))
    });
  }
}
