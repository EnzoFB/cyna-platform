import { TestBed } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';
import { HistoryComponent } from './history.component';
import { AccountOrder } from '../../models/account.models';

const makeOrder = (partial: Partial<AccountOrder> & Pick<AccountOrder, 'id' | 'createdAt' | 'status'>): AccountOrder => ({
  userId: 'user-1',
  subtotalHt: 100,
  currency: 'EUR',
  billingAddress: null,
  updatedAt: partial.createdAt,
  lines: [],
  ...partial,
});

const ORDER_2023: AccountOrder = makeOrder({
  id: 'order-abc-2023',
  createdAt: '2023-03-15T10:00:00Z',
  status: 'PAID',
  lines: [{ id: 'l1', productId: 'p1', productName: 'Firewall Pro', productCategory: 'Security', billingCycle: 'MONTHLY', quantity: 1, unitPrice: 100, currency: 'EUR' }],
});

const ORDER_2024: AccountOrder = makeOrder({
  id: 'order-xyz-2024',
  createdAt: '2024-06-01T10:00:00Z',
  status: 'PENDING',
  lines: [{ id: 'l2', productId: 'p2', productName: 'VPN Secure', productCategory: 'Network', billingCycle: 'ANNUAL', quantity: 2, unitPrice: 50, currency: 'EUR' }],
});

const ORDER_2024B: AccountOrder = makeOrder({
  id: 'order-def-2024',
  createdAt: '2024-11-20T10:00:00Z',
  status: 'FULFILLED',
  lines: [{ id: 'l3', productId: 'p3', productName: 'Firewall Basic', productCategory: 'Security', billingCycle: 'MONTHLY', quantity: 1, unitPrice: 30, currency: 'EUR' }],
});

function createComponent(orders: AccountOrder[] = []) {
  const fixture = TestBed.createComponent(HistoryComponent);
  fixture.componentRef.setInput('orders', orders);
  fixture.componentRef.setInput('invoices', []);
  fixture.componentRef.setInput('loading', false);
  fixture.detectChanges();
  return fixture.componentInstance;
}

describe('HistoryComponent', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HistoryComponent, TranslateModule.forRoot()],
    });
  });

  describe('availableStatuses()', () => {
    it('returns only statuses present in orders', () => {
      const component = createComponent([ORDER_2023, ORDER_2024, ORDER_2024B]);
      expect(component.availableStatuses()).toEqual(['FULFILLED', 'PAID', 'PENDING']);
    });

    it('returns empty array when there are no orders', () => {
      const component = createComponent([]);
      expect(component.availableStatuses()).toEqual([]);
    });

    it('deduplicates statuses when multiple orders share the same status', () => {
      const order2 = makeOrder({ id: 'order-dup', createdAt: '2024-01-01T00:00:00Z', status: 'PAID' });
      const component = createComponent([ORDER_2023, order2]);
      expect(component.availableStatuses()).toEqual(['PAID']);
    });
  });

  describe('availableYears()', () => {
    it('extracts unique years from orders in descending order', () => {
      const component = createComponent([ORDER_2023, ORDER_2024, ORDER_2024B]);
      expect(component.availableYears()).toEqual([2024, 2023]);
    });

    it('returns empty array when there are no orders', () => {
      const component = createComponent([]);
      expect(component.availableYears()).toEqual([]);
    });
  });

  describe('filteredOrders()', () => {
    it('returns all orders when no filter is active', () => {
      const component = createComponent([ORDER_2023, ORDER_2024, ORDER_2024B]);
      expect(component.filteredOrders().length).toBe(3);
    });

    it('filters by order id (search query)', () => {
      const component = createComponent([ORDER_2023, ORDER_2024, ORDER_2024B]);
      component.searchQuery.set('abc');
      expect(component.filteredOrders()).toEqual([ORDER_2023]);
    });

    it('filters by product name (search query)', () => {
      const component = createComponent([ORDER_2023, ORDER_2024, ORDER_2024B]);
      component.searchQuery.set('firewall');
      expect(component.filteredOrders()).toEqual([ORDER_2023, ORDER_2024B]);
    });

    it('is case-insensitive for search', () => {
      const component = createComponent([ORDER_2023, ORDER_2024]);
      component.searchQuery.set('VPN');
      expect(component.filteredOrders()).toEqual([ORDER_2024]);
    });

    it('filters by year', () => {
      const component = createComponent([ORDER_2023, ORDER_2024, ORDER_2024B]);
      component.selectedYear.set(2024);
      expect(component.filteredOrders()).toEqual([ORDER_2024, ORDER_2024B]);
    });

    it('filters by status', () => {
      const component = createComponent([ORDER_2023, ORDER_2024, ORDER_2024B]);
      component.selectedStatus.set('PENDING');
      expect(component.filteredOrders()).toEqual([ORDER_2024]);
    });

    it('combines year and status filters', () => {
      const component = createComponent([ORDER_2023, ORDER_2024, ORDER_2024B]);
      component.selectedYear.set(2024);
      component.selectedStatus.set('FULFILLED');
      expect(component.filteredOrders()).toEqual([ORDER_2024B]);
    });

    it('combines search and year filters', () => {
      const component = createComponent([ORDER_2023, ORDER_2024, ORDER_2024B]);
      component.searchQuery.set('firewall');
      component.selectedYear.set(2024);
      expect(component.filteredOrders()).toEqual([ORDER_2024B]);
    });

    it('returns empty array when no order matches', () => {
      const component = createComponent([ORDER_2023, ORDER_2024]);
      component.searchQuery.set('nonexistent-ref');
      expect(component.filteredOrders()).toEqual([]);
    });
  });

  describe('clearFilters()', () => {
    it('resets all filters to their default values', () => {
      const component = createComponent([ORDER_2023, ORDER_2024]);
      component.searchQuery.set('abc');
      component.selectedYear.set(2023);
      component.selectedStatus.set('PAID');

      component.clearFilters();

      expect(component.searchQuery()).toBe('');
      expect(component.selectedYear()).toBeNull();
      expect(component.selectedStatus()).toBeNull();
    });

    it('shows all orders after clearing filters', () => {
      const component = createComponent([ORDER_2023, ORDER_2024]);
      component.searchQuery.set('no-match');
      expect(component.filteredOrders().length).toBe(0);

      component.clearFilters();
      expect(component.filteredOrders().length).toBe(2);
    });
  });

  describe('toggleDropdown()', () => {
    it('opens the given dropdown', () => {
      const component = createComponent([ORDER_2023]);
      const event = new MouseEvent('click');
      component.toggleDropdown('year', event);
      expect(component.openDropdown()).toBe('year');
    });

    it('closes the dropdown when toggled again', () => {
      const component = createComponent([ORDER_2023]);
      const event = new MouseEvent('click');
      component.toggleDropdown('year', event);
      component.toggleDropdown('year', event);
      expect(component.openDropdown()).toBeNull();
    });

    it('switches to the other dropdown without closing', () => {
      const component = createComponent([ORDER_2023]);
      const event = new MouseEvent('click');
      component.toggleDropdown('year', event);
      component.toggleDropdown('status', event);
      expect(component.openDropdown()).toBe('status');
    });
  });

  describe('selectYear()', () => {
    it('sets the selected year and closes the dropdown', () => {
      const component = createComponent([ORDER_2023]);
      const event = new MouseEvent('click');
      component.toggleDropdown('year', event);
      component.selectYear(2023, event);
      expect(component.selectedYear()).toBe(2023);
      expect(component.openDropdown()).toBeNull();
    });

    it('clears the year when null is passed', () => {
      const component = createComponent([ORDER_2023]);
      const event = new MouseEvent('click');
      component.selectedYear.set(2023);
      component.selectYear(null, event);
      expect(component.selectedYear()).toBeNull();
    });
  });

  describe('selectStatus()', () => {
    it('sets the selected status and closes the dropdown', () => {
      const component = createComponent([ORDER_2023]);
      const event = new MouseEvent('click');
      component.toggleDropdown('status', event);
      component.selectStatus('PAID', event);
      expect(component.selectedStatus()).toBe('PAID');
      expect(component.openDropdown()).toBeNull();
    });

    it('clears the status when null is passed', () => {
      const component = createComponent([ORDER_2023]);
      const event = new MouseEvent('click');
      component.selectedStatus.set('PAID');
      component.selectStatus(null, event);
      expect(component.selectedStatus()).toBeNull();
    });
  });
});
