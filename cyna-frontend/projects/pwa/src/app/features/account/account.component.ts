import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { catchError, finalize, forkJoin, of } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { UserService } from '../../core/services/user.service';
import { ToastService } from '../../core/services/toast.service';
import { UserResponse } from '../../core/models/user.model';
import { SubscriptionsComponent } from './tabs/subscriptions/subscriptions.component';
import { HistoryComponent } from './tabs/history/history.component';
import { ProfileComponent } from './tabs/profile/profile.component';
import { AddressesComponent } from './tabs/addresses/addresses.component';
import { PaymentMethodsComponent } from './tabs/payment-methods/payment-methods.component';
import { AccountDashboardService } from './services/account-dashboard.service';
import { AccountInvoice, AccountOrder, AccountSubscription } from './models/account.models';

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
  readonly subscriptionsLoading = signal(false);
  readonly updatingSubscriptionId = signal<string | null>(null);
  readonly orders = signal<readonly AccountOrder[]>([]);
  readonly invoices = signal<readonly AccountInvoice[]>([]);
  readonly dashboardLoading = signal(true);
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
    this.loadSubscriptions();
    this.loadDashboard();

    const tabParam = this.route.snapshot.queryParamMap.get('tab');
    if (tabParam === 'history' || tabParam === 'subscriptions' || tabParam === 'profile' || tabParam === 'addresses' || tabParam === 'payment') {
      this.activeTab.set(tabParam);
      this.router.navigate([], { queryParams: { tab: null }, queryParamsHandling: 'merge', replaceUrl: true });
    }

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

  loadSubscriptions(): void {
    this.subscriptionsLoading.set(true);

    this.dashboardService
      .listSubscriptions()
      .pipe(
        catchError(() => of<readonly AccountSubscription[]>([])),
        finalize(() => this.subscriptionsLoading.set(false))
      )
      .subscribe((subscriptions) => {
        this.subscriptions.set(subscriptions);
      });
  }

  loadDashboard(): void {
    this.dashboardLoading.set(true);

    forkJoin({
      orders: this.dashboardService.listOrders().pipe(
        catchError(() => of<readonly AccountOrder[]>([]))
      ),
      invoices: this.dashboardService.listInvoices().pipe(
        catchError(() => of<readonly AccountInvoice[]>([]))
      )
    }).subscribe(({ orders, invoices }) => {
      this.orders.set(orders);
      this.invoices.set(invoices);
      this.dashboardLoading.set(false);
    });
  }

  setTab(tab: AccountTab): void {
    this.activeTab.set(tab);
  }

  onAutoRenewChanged(event: { subscriptionId: string; autoRenew: boolean }): void {
    this.updatingSubscriptionId.set(event.subscriptionId);

    this.dashboardService
      .updateSubscriptionAutoRenew(event.subscriptionId, event.autoRenew)
      .pipe(
        finalize(() => this.updatingSubscriptionId.set(null))
      )
      .subscribe({
        next: (updatedSubscription) => {
          this.subscriptions.update((items) =>
            items.map((item) => item.id === updatedSubscription.id ? updatedSubscription : item)
          );
          const key = updatedSubscription.autoRenew
            ? 'account.subscriptions.toast.autoRenewEnabled'
            : 'account.subscriptions.toast.autoRenewDisabled';
          this.toastService.showSuccess(this.translateService.instant(key));
        },
        error: () => {
          this.toastService.showError(
            this.translateService.instant('account.subscriptions.toast.autoRenewError')
          );
        }
      });
  }

}
