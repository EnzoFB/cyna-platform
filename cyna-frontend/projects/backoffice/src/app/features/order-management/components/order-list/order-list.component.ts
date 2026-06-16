import {
  Component,
  HostListener,
  inject,
  signal,
  computed,
  OnInit,
  OnDestroy,
} from '@angular/core';
import { NgClass } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateService, TranslatePipe } from '@ngx-translate/core';
import { OrderService, AdminOrder, AdminOrderDetail } from '../../../../core/services/order.service';
import { OrderDetailModalComponent } from '../order-detail-modal/order-detail-modal.component';

type SortField = 'createdAt' | 'customerLastName' | 'status' | 'totalAmount';
type SortDir   = 'asc' | 'desc';

@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [NgClass, FormsModule, OrderDetailModalComponent, TranslatePipe],
  templateUrl: './order-list.component.html',
  styleUrl: './order-list.component.scss',
})
export class OrderListComponent implements OnInit, OnDestroy {
  private readonly orderService = inject(OrderService);
  private readonly translate    = inject(TranslateService);

  protected readonly loading         = signal(false);
  protected readonly orders          = signal<AdminOrder[]>([]);
  protected readonly totalElements   = signal(0);
  protected readonly currentPage     = signal(0);
  protected readonly pageSize        = signal(20);
  protected readonly searchQuery     = signal('');
  protected readonly statusFilter    = signal<string>('');
  protected readonly sortField       = signal<SortField | null>(null);
  protected readonly sortDir         = signal<SortDir>('asc');
  protected readonly copyToast       = signal<string | null>(null);
  protected readonly toast           = signal<{ message: string; type: 'success' | 'error' } | null>(null);
  protected readonly detailModalOpen = signal(false);
  protected readonly selectedOrder   = signal<AdminOrderDetail | null>(null);
  protected readonly detailLoading   = signal(false);
  protected readonly isStatusOpen    = signal(false);

  private copyToastTimer: ReturnType<typeof setTimeout> | null = null;
  private toastTimer:     ReturnType<typeof setTimeout> | null = null;

  protected readonly totalPages = computed(() =>
    Math.max(1, Math.ceil(this.totalElements() / this.pageSize()))
  );

  protected readonly paginationFrom = computed(() =>
    this.totalElements() === 0 ? 0 : this.currentPage() * this.pageSize() + 1
  );

  protected readonly paginationTo = computed(() =>
    Math.min((this.currentPage() + 1) * this.pageSize(), this.totalElements())
  );

  protected readonly filteredOrders = computed(() => {
    const q = this.searchQuery().toLowerCase().trim();
    const raw = this.orders();
    const filtered = q
      ? raw.filter(o =>
          o.id.toLowerCase().includes(q) ||
          o.customerEmail.toLowerCase().includes(q) ||
          o.customerFirstName.toLowerCase().includes(q) ||
          o.customerLastName.toLowerCase().includes(q) ||
          o.status.toLowerCase().includes(q)
        )
      : raw;

    const field = this.sortField();
    const dir   = this.sortDir();
    if (!field) return filtered;

    return [...filtered].sort((a, b) => {
      let cmp = 0;
      switch (field) {
        case 'createdAt':        cmp = new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime(); break;
        case 'customerLastName': cmp = a.customerLastName.localeCompare(b.customerLastName, 'fr'); break;
        case 'status':           cmp = a.status.localeCompare(b.status); break;
        case 'totalAmount':      cmp = a.totalAmount - b.totalAmount; break;
      }
      return dir === 'asc' ? cmp : -cmp;
    });
  });

  ngOnInit(): void {
    this.loadOrders();
  }

  ngOnDestroy(): void {
    [this.toastTimer, this.copyToastTimer].forEach(t => t && clearTimeout(t));
  }

  protected loadOrders(): void {
    this.loading.set(true);
    const status = this.statusFilter() || undefined;
    this.orderService.getOrders(this.currentPage(), this.pageSize(), status).subscribe({
      next: (response) => {
        this.orders.set(response.data.items);
        this.totalElements.set(response.data.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }

  @HostListener('document:click')
  onDocumentClick(): void {
    this.isStatusOpen.set(false);
  }

  protected toggleStatusDropdown(event: Event): void {
    event.stopPropagation();
    this.isStatusOpen.update(v => !v);
  }

  protected selectStatusFilter(value: string): void {
    this.isStatusOpen.set(false);
    this.onStatusFilterChange(value);
  }

  protected getStatusDotClass(status: string): string {
    const map: Record<string, string> = {
      PENDING:   'status-dot--orange',
      CONFIRMED: 'status-dot--blue',
      PAID:      'status-dot--green',
      FULFILLED: 'status-dot--purple',
      CANCELLED: 'status-dot--gray',
    };
    return map[status] ?? 'status-dot--gray';
  }

  protected onSearchChange(value: string): void {
    this.searchQuery.set(value);
  }

  protected onStatusFilterChange(value: string): void {
    this.statusFilter.set(value);
    this.currentPage.set(0);
    this.loadOrders();
  }

  protected sortBy(field: SortField): void {
    if (this.sortField() === field) {
      this.sortDir.set(this.sortDir() === 'asc' ? 'desc' : 'asc');
    } else {
      this.sortField.set(field);
      this.sortDir.set('asc');
    }
  }

  protected goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages()) return;
    this.currentPage.set(page);
    this.loadOrders();
  }

  protected onPageSizeChange(size: number): void {
    this.pageSize.set(size);
    this.currentPage.set(0);
    this.loadOrders();
  }

  protected exportCsv(): void {
    const headers = ['ID', 'Date', 'Client', 'Email', 'Statut', 'Produits', 'Montant'];
    const rows = this.filteredOrders().map(o => [
      o.id,
      this.formatDate(o.createdAt),
      `${o.customerLastName} ${o.customerFirstName}`,
      o.customerEmail,
      this.getStatusLabel(o.status),
      String(o.lineCount),
      this.formatCurrency(o.totalAmount, o.currency),
    ]);
    const csv = [headers, ...rows]
      .map(r => r.map(v => `"${v.replace(/"/g, '""')}"`).join(','))
      .join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url  = URL.createObjectURL(blob);
    const a    = document.createElement('a');
    a.href     = url;
    a.download = `commandes-${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }

  protected copyId(id: string, event: MouseEvent): void {
    event.stopPropagation();
    navigator.clipboard.writeText(id).then(() => {
      if (this.copyToastTimer) clearTimeout(this.copyToastTimer);
      this.copyToast.set(`ID copié : #${id.substring(0, 8).toUpperCase()}...`);
      this.copyToastTimer = setTimeout(() => this.copyToast.set(null), 2500);
    });
  }

  protected openDetail(orderId: string): void {
    this.detailModalOpen.set(true);
    this.selectedOrder.set(null);
    this.detailLoading.set(true);

    const listRow = this.orders().find(o => o.id === orderId);

    this.orderService.getOrderById(orderId).subscribe({
      next: (response) => {
        this.selectedOrder.set({
          ...response.data,
          customerEmail:     listRow?.customerEmail     ?? '',
          customerFirstName: listRow?.customerFirstName ?? '',
          customerLastName:  listRow?.customerLastName  ?? '',
        });
        this.detailLoading.set(false);
      },
      error: () => {
        this.detailLoading.set(false);
        this.showToast(this.translate.instant('orders.toast.loadError'), 'error');
        this.detailModalOpen.set(false);
      },
    });
  }

  protected closeDetail(): void {
    this.detailModalOpen.set(false);
    this.selectedOrder.set(null);
  }

  protected cancelOrder(orderId: string): void {
    if (!confirm(this.translate.instant('orders.confirmCancel'))) return;

    this.orderService.cancelOrder(orderId, 'Cancelled by administrator').subscribe({
      next: () => {
        this.showToast(this.translate.instant('orders.toast.cancelled'), 'success');
        if (this.selectedOrder()?.id === orderId) {
          this.closeDetail();
        }
        this.loadOrders();
      },
      error: () => {
        this.showToast(this.translate.instant('orders.toast.cancelError'), 'error');
      },
    });
  }

  protected canCancel(status: string): boolean {
    return status === 'PENDING' || status === 'CONFIRMED';
  }

  protected formatDate(dateStr: string): string {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleDateString('fr-FR', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit',
    });
  }

  protected formatCurrency(amount: number, currency: string): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: currency || 'EUR',
    }).format(amount);
  }

  protected getStatusLabel(status: string): string {
    const key = `orders.status.${status}`;
    return key;
  }

  private showToast(message: string, type: 'success' | 'error'): void {
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toast.set({ message, type });
    this.toastTimer = setTimeout(() => this.toast.set(null), 3000);
  }
}
