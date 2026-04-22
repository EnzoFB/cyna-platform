import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../core/services/auth.service';
import { UserService } from '../../core/services/user.service';
import { ToastService } from '../../core/services/toast.service';
import { UserResponse } from '../../core/models/user.model';
import { SubscriptionsComponent } from './tabs/subscriptions.component';
import { HistoryComponent } from './tabs/history.component';
import { ProfileComponent } from './tabs/profile.component';
import { AddressesComponent } from './tabs/addresses.component';
import { PaymentMethodsComponent } from './tabs/payment-methods.component';

export type AccountTab = 'subscriptions' | 'history' | 'profile' | 'addresses' | 'payment';

@Component({
  selector: 'app-account',
  standalone: true,
  imports: [
    TranslatePipe,
    SubscriptionsComponent,
    HistoryComponent,
    ProfileComponent,
    AddressesComponent,
    PaymentMethodsComponent,
  ],
  templateUrl: './account.component.html',
  styleUrl: './account.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AccountComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly userService = inject(UserService);
  private readonly toastService = inject(ToastService);
  private readonly translateService = inject(TranslateService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  private readonly authUser = this.authService.user;
  readonly profile = signal<UserResponse | null>(null);
  readonly activeTab = signal<AccountTab>('subscriptions');

  readonly displayName = computed(() => {
    const p = this.profile();
    if (p) return `${p.firstName} ${p.lastName}`;
    const u = this.authUser();
    if (u?.firstName && u?.lastName) return `${u.firstName} ${u.lastName}`;
    return u?.email ?? '';
  });

  ngOnInit(): void {
    this.loadProfile();

    const confirmToken = this.route.snapshot.queryParamMap.get('confirmEmail');
    if (confirmToken) {
      this.activeTab.set('profile');
      this.router.navigate([], { queryParams: {}, replaceUrl: true });
      this.userService.confirmEmailChange(confirmToken).subscribe({
        next: () => {
          this.toastService.showSuccess(
            this.translateService.instant('account.profile.emailConfirmed')
          );
          this.loadProfile();
        },
        error: () => {
          this.toastService.showError(
            this.translateService.instant('account.profile.emailConfirmError')
          );
        }
      });
    }
  }

  loadProfile(): void {
    this.userService.getProfile().subscribe({
      next: (res) => this.profile.set(res.data),
      error: () => {}
    });
  }

  setTab(tab: AccountTab): void {
    this.activeTab.set(tab);
  }
}
