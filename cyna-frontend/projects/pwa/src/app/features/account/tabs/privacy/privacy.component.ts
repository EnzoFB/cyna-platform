import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../../../core/services/auth.service';
import { ToastService } from '../../../../core/services/toast.service';
import { AccountPrivacyService } from '../../services/account-privacy.service';

@Component({
  selector: 'app-privacy',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './privacy.component.html',
  styleUrl: './privacy.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PrivacyComponent {
  private readonly privacyService = inject(AccountPrivacyService);
  private readonly authService = inject(AuthService);
  private readonly toastService = inject(ToastService);
  private readonly translateService = inject(TranslateService);

  readonly exporting = signal(false);
  readonly deleting = signal(false);

  exportData(): void {
    if (this.exporting()) return;
    this.exporting.set(true);
    this.privacyService.exportMyData().subscribe({
      next: () => this.exporting.set(false),
      error: () => {
        this.exporting.set(false);
        this.toastService.showError(
          this.translateService.instant('account.privacy.export.error'));
      },
    });
  }

  deleteAccount(): void {
    if (this.deleting()) return;
    const confirmed = window.confirm(
      this.translateService.instant('account.privacy.delete.confirm'));
    if (!confirmed) return;

    this.deleting.set(true);
    this.privacyService.deleteMyAccount().subscribe({
      next: () => {
        this.toastService.showSuccess(
          this.translateService.instant('account.privacy.delete.success'));
        // Session is revoked server-side; clear client state and leave.
        this.authService.logout();
      },
      error: () => {
        this.deleting.set(false);
        this.toastService.showError(
          this.translateService.instant('account.privacy.delete.error'));
      },
    });
  }
}
