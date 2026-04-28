import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { catchError, forkJoin, of } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { UserService } from '../../core/services/user.service';
import { ToastService } from '../../core/services/toast.service';
import { UserResponse } from '../../core/models/user.model';
import { SubscriptionsComponent } from './tabs/subscriptions.component';
import { HistoryComponent } from './tabs/history.component';
import { ProfileComponent } from './tabs/profile.component';
import { AddressesComponent } from './tabs/addresses.component';
import { PaymentMethodsComponent } from './tabs/payment-methods.component';
import { AccountDashboardService } from './services/account-dashboard.service';
import { AccountOrder, AccountSubscription } from './models/account.models';

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
  private readonly dashboardService = inject(AccountDashboardService);
  private readonly toastService = inject(ToastService);
  private readonly translateService = inject(TranslateService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  private readonly authUser = this.authService.user;
  readonly profile = signal<UserResponse | null>(null);
  readonly subscriptions = signal<readonly AccountSubscription[]>([]);
  readonly orders = signal<readonly AccountOrder[]>([]);
  readonly dashboardLoading = signal(true);
  readonly activeTab = signal<AccountTab>('subscriptions');

  readonly displayName = computed(() => {
    const p = this.profile();
    if (p) return `${p.firstName} ${p.lastName}`;
    const u = this.authUser();
    if (u?.firstName && u?.lastName) return `${u.firstName} ${u.lastName}`;
    return u?.email ?? '';
  });

  readonly activeSubscriptionsCount = computed(() =>
    this.subscriptions().filter(item => item.status === 'ACTIVE').length
  );

  readonly nextBillingDate = computed(() => {
    const nextDate = this.subscriptions()
      .filter(item => item.status === 'ACTIVE' && !!item.nextBillingAt)
      .map(item => item.nextBillingAt as string)
      .sort((a, b) => new Date(a).getTime() - new Date(b).getTime())
      .at(0);

    if (!nextDate) {
      return this.translateService.instant('account.dashboard.metrics.noBillingDate');
    }

    return this.formatDate(nextDate);
  });

  readonly annualSpending = computed(() => {
    const currentYear = new Date().getFullYear();
    const total = this.orders()
      .filter(order => new Date(order.createdAt).getFullYear() === currentYear)
      .reduce((sum, order) => sum + order.totalAmount, 0);

    return this.formatCurrency(total, this.orders()[0]?.currency ?? 'EUR');
  });

  ngOnInit(): void {
    this.loadProfile();
    this.loadDashboard();

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

  loadDashboard(): void {
    this.dashboardLoading.set(true);

    forkJoin({
      subscriptions: this.dashboardService.listSubscriptions().pipe(
        catchError(() => of<readonly AccountSubscription[]>([]))
      ),
      orders: this.dashboardService.listOrders().pipe(
        catchError(() => of<readonly AccountOrder[]>([]))
      )
    }).subscribe(({ subscriptions, orders }) => {
      this.subscriptions.set(subscriptions);
      this.orders.set(orders);
      this.dashboardLoading.set(false);
    });
  }

  setTab(tab: AccountTab): void {
    this.activeTab.set(tab);
  }

  private formatDate(rawDate: string): string {
    const date = new Date(rawDate);
    if (Number.isNaN(date.getTime())) {
      return this.translateService.instant('account.dashboard.metrics.noBillingDate');
    }

    const locale = this.translateService.getCurrentLang() === 'fr' ? 'fr-FR' : 'en-US';

    return new Intl.DateTimeFormat(locale, {
      day: 'numeric',
      month: 'long'
    }).format(date);
  }

  private formatCurrency(value: number, currency: string): string {
    const locale = this.translateService.getCurrentLang() === 'fr' ? 'fr-FR' : 'en-US';

    return new Intl.NumberFormat(locale, {
      style: 'currency',
      currency,
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(value);
  }
}
