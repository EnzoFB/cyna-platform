import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { SubscriptionsComponent } from './subscriptions.component';
import { AccountSubscription } from '../../models/account.models';

describe('SubscriptionsComponent', () => {
  let component: SubscriptionsComponent;
  let fixture: ComponentFixture<SubscriptionsComponent>;

  const baseSubscription: AccountSubscription = {
    id: 'sub-1',
    userId: 'user-1',
    orderId: 'order-1',
    productId: 'prod-1',
    productName: 'SOC',
    productCategory: 'SOC',
    billingCycle: 'ANNUAL',
    status: 'ACTIVE',
    quantity: 1,
    unitPrice: 1200,
    currency: 'EUR',
    startAt: '2026-01-01T00:00:00Z',
    endAt: '2027-01-01T00:00:00Z',
    nextBillingAt: '2027-01-01T00:00:00Z',
    cancelledAt: null,
    autoRenew: true,
    autoRenewNoticeSentAt: null,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z'
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SubscriptionsComponent],
      // Use the real TranslateService — the previous hand-rolled mock only
      // exposed `instant`, but the template renders via the `| translate` pipe
      // which calls `translate.get(...)` (observable) and crashes when missing.
      providers: [provideTranslateService()]
    }).compileComponents();

    fixture = TestBed.createComponent(SubscriptionsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should open disable modal when auto renew is enabled', () => {
    component.requestAutoRenewToggle(baseSubscription);

    expect(component.pendingDisable()?.id).toBe(baseSubscription.id);
  });

  it('should emit enable event when auto renew is disabled', () => {
    const emitSpy = spyOn(component.autoRenewChanged, 'emit');
    const disabledSubscription: AccountSubscription = {
      ...baseSubscription,
      autoRenew: false
    };

    component.requestAutoRenewToggle(disabledSubscription);

    expect(emitSpy).toHaveBeenCalledWith({
      subscriptionId: disabledSubscription.id,
      autoRenew: true
    });
  });
});
