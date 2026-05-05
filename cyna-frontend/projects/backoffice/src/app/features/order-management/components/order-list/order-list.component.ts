import {
  Component,
  inject,
  signal,
  computed,
  OnInit,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AgGridAngular } from 'ag-grid-angular';
import {
  AllCommunityModule,
  ModuleRegistry,
  ColDef,
  GridReadyEvent,
  GridApi,
  ICellRendererParams,
  themeAlpine,
} from 'ag-grid-community';
import { OrderService, AdminOrder, AdminOrderDetail } from '../../../../core/services/order.service';
import { OrderDetailModalComponent } from '../order-detail-modal/order-detail-modal.component';

ModuleRegistry.registerModules([AllCommunityModule]);

@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [AgGridAngular, FormsModule, OrderDetailModalComponent],
  templateUrl: './order-list.component.html',
  styleUrl: './order-list.component.scss',
})
export class OrderListComponent implements OnInit {
  private readonly orderService = inject(OrderService);

  private gridApi!: GridApi<AdminOrder>;

  protected readonly theme = themeAlpine.withParams({
    fontFamily: 'inherit',
    fontSize: 13,
    headerBackgroundColor: '#ffffff',
    headerTextColor: '#374151',
    borderColor: '#e5e7eb',
    rowBorder: true,
    oddRowBackgroundColor: '#ffffff',
    rowHoverColor: '#f0f4ff',
    selectedRowBackgroundColor: '#eff3ff',
    cellHorizontalPaddingScale: 1.2,
    rowHeight: 64,
    headerHeight: 48,
    checkboxCheckedShapeColor: '#4f73f5',
  });

  protected readonly loading = signal(false);
  protected readonly searchQuery = signal('');
  protected readonly statusFilter = signal<string>('');
  protected readonly copyToast = signal<string | null>(null);
  protected readonly toast = signal<{ message: string; type: 'success' | 'error' } | null>(null);
  protected readonly detailModalOpen = signal(false);
  protected readonly selectedOrder = signal<AdminOrderDetail | null>(null);
  protected readonly detailLoading = signal(false);

  private copyToastTimer: ReturnType<typeof setTimeout> | null = null;
  private toastTimer: ReturnType<typeof setTimeout> | null = null;

  protected readonly currentPage = signal(0);
  protected readonly pageSize = signal(20);
  protected readonly totalElements = signal(0);
  protected readonly totalPages = signal(0);
  protected readonly rowData = signal<AdminOrder[]>([]);

  protected readonly rangeLabel = computed(() => {
    const start = this.currentPage() * this.pageSize() + 1;
    const end = Math.min(start + this.pageSize() - 1, this.totalElements());
    return `${start} - ${end} of ${this.totalPages()} Pages`;
  });

  protected readonly pagesArray = computed(() =>
    Array.from({ length: this.totalPages() }, (_, i) => i + 1)
  );

  protected readonly colDefs: ColDef<AdminOrder>[] = [
    {
      headerCheckboxSelection: true,
      checkboxSelection: true,
      width: 50,
      minWidth: 50,
      maxWidth: 50,
      resizable: false,
      sortable: false,
      filter: false,
      pinned: 'left',
    },
    {
      headerName: 'Commande',
      field: 'id',
      minWidth: 200,
      flex: 2,
      cellRenderer: (params: ICellRendererParams<AdminOrder>) => {
        const order = params.data!;
        const shortId = order.id.substring(0, 8).toUpperCase();
        const date = new Date(order.createdAt).toLocaleDateString('fr-FR', {
          day: '2-digit',
          month: '2-digit',
          year: 'numeric',
          hour: '2-digit',
          minute: '2-digit',
        });
        return `
          <div class="cell-name">
            <div class="cell-name__info">
              <span class="cell-name__id cell-name__id--copyable" data-copy-id="${this.escapeHtml(order.id)}" title="Cliquer pour copier l'ID">#${this.escapeHtml(shortId)}...</span>
              <span class="cell-name__full">${this.escapeHtml(date)}</span>
            </div>
          </div>`;
      },
    },
    {
      headerName: 'Client',
      field: 'customerEmail',
      minWidth: 220,
      flex: 2.5,
      cellRenderer: (params: ICellRendererParams<AdminOrder>) => {
        const order = params.data!;
        const fullName = `${this.escapeHtml(order.customerLastName.toUpperCase())} ${this.escapeHtml(order.customerFirstName)}`;
        return `
          <div class="cell-contact">
            <span class="cell-contact__email">${this.escapeHtml(order.customerEmail)}</span>
            <span class="cell-contact__phone">${fullName}</span>
          </div>`;
      },
    },
    {
      headerName: 'Statut',
      field: 'status',
      minWidth: 130,
      flex: 1.2,
      cellRenderer: (params: ICellRendererParams<AdminOrder>) => {
        const status = params.data!.status;
        const { label, color, bg } = this.getStatusMeta(status);
        return `<span class="status-badge" style="color:${color};background:${bg}">${label}</span>`;
      },
    },
    {
      headerName: 'Produits',
      field: 'lineCount',
      minWidth: 120,
      flex: 1,
      cellRenderer: (params: ICellRendererParams<AdminOrder>) => {
        const count = params.data!.lineCount;
        return `<span class="cell-address">${count} produit(s)</span>`;
      },
    },
    {
      headerName: 'Montant TTC',
      field: 'totalAmount',
      minWidth: 140,
      flex: 1.2,
      cellRenderer: (params: ICellRendererParams<AdminOrder>) => {
        const order = params.data!;
        const formatted = new Intl.NumberFormat('fr-FR', {
          style: 'currency',
          currency: order.currency || 'EUR',
        }).format(order.totalAmount);
        return `<span class="cell-purchases">${this.escapeHtml(formatted)}</span>`;
      },
    },
    {
      headerName: 'Actions',
      minWidth: 100,
      maxWidth: 110,
      sortable: false,
      filter: false,
      resizable: false,
      cellRenderer: (params: ICellRendererParams<AdminOrder>) => {
        const order = params.data!;
        const canCancel = order.status === 'PENDING' || order.status === 'CONFIRMED';
        const cancelBtn = canCancel
          ? `<button class="action-btn action-btn--delete" title="Annuler la commande" data-action="cancel" data-order-id="${this.escapeHtml(order.id)}">
               <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                 <circle cx="12" cy="12" r="10"/>
                 <line x1="15" y1="9" x2="9" y2="15"/>
                 <line x1="9" y1="9" x2="15" y2="15"/>
               </svg>
             </button>`
          : '';
        return `
          <div class="cell-actions">
            <button class="action-btn action-btn--view" title="Voir le détail" data-action="view" data-order-id="${this.escapeHtml(order.id)}">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                <circle cx="12" cy="12" r="3"/>
              </svg>
            </button>
            ${cancelBtn}
          </div>`;
      },
    },
  ];

  protected readonly defaultColDef: ColDef = {
    sortable: true,
    filter: false,
    resizable: true,
  };

  ngOnInit(): void {
    this.loadOrders();
  }

  protected onGridReady(event: GridReadyEvent<AdminOrder>): void {
    this.gridApi = event.api;
  }

  protected loadOrders(): void {
    this.loading.set(true);
    const status = this.statusFilter() || undefined;
    this.orderService.getOrders(this.currentPage(), this.pageSize(), status).subscribe({
      next: (response) => {
        this.rowData.set(response.data.items);
        this.totalElements.set(response.data.totalElements);
        this.totalPages.set(response.data.totalPages);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }

  protected onSearchChange(value: string): void {
    this.searchQuery.set(value);
    this.gridApi?.setGridOption('quickFilterText', value);
  }

  protected onStatusFilterChange(value: string): void {
    this.statusFilter.set(value);
    this.currentPage.set(0);
    this.loadOrders();
  }

  protected goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages()) return;
    this.currentPage.set(page);
    this.loadOrders();
  }

  protected previousPage(): void {
    this.goToPage(this.currentPage() - 1);
  }

  protected nextPage(): void {
    this.goToPage(this.currentPage() + 1);
  }

  protected onPageSizeChange(size: number): void {
    this.pageSize.set(size);
    this.currentPage.set(0);
    this.loadOrders();
  }

  protected exportCsv(): void {
    this.gridApi?.exportDataAsCsv({
      fileName: `commandes-${new Date().toISOString().slice(0, 10)}.csv`,
    });
  }

  protected onGridClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;

    // Handle copy ID
    const copyEl = target.closest<HTMLElement>('[data-copy-id]');
    if (copyEl) {
      const orderId = copyEl.dataset['copyId'];
      if (orderId) {
        navigator.clipboard.writeText(orderId).then(() => {
          this.showCopyToast(orderId);
        });
      }
      return;
    }

    // Handle action buttons
    const actionEl = target.closest<HTMLElement>('[data-action]');
    if (!actionEl) return;

    const action = actionEl.dataset['action'];
    const orderId = actionEl.dataset['orderId'];
    if (!action || !orderId) return;

    if (action === 'view') {
      this.openDetail(orderId);
    } else if (action === 'cancel') {
      this.cancelOrder(orderId);
    }
  }

  protected openDetail(orderId: string): void {
    this.detailModalOpen.set(true);
    this.selectedOrder.set(null);
    this.detailLoading.set(true);

    const listRow = this.rowData().find(o => o.id === orderId);

    this.orderService.getOrderById(orderId).subscribe({
      next: (response) => {
        this.selectedOrder.set({
          ...response.data,
          customerEmail: listRow?.customerEmail ?? '',
          customerFirstName: listRow?.customerFirstName ?? '',
          customerLastName: listRow?.customerLastName ?? '',
        });
        this.detailLoading.set(false);
      },
      error: () => {
        this.detailLoading.set(false);
        this.showToast('Erreur lors du chargement de la commande', 'error');
        this.detailModalOpen.set(false);
      },
    });
  }

  protected closeDetail(): void {
    this.detailModalOpen.set(false);
    this.selectedOrder.set(null);
  }

  protected cancelOrder(orderId: string): void {
    if (!confirm('Confirmer l\'annulation de cette commande ?')) return;

    this.orderService.cancelOrder(orderId, 'Annulé par l\'administrateur').subscribe({
      next: () => {
        this.showToast('Commande annulée avec succès', 'success');
        // Close detail modal if open for this order
        if (this.selectedOrder()?.id === orderId) {
          this.closeDetail();
        }
        this.loadOrders();
      },
      error: () => {
        this.showToast('Erreur lors de l\'annulation', 'error');
      },
    });
  }

  private showCopyToast(id: string): void {
    if (this.copyToastTimer) clearTimeout(this.copyToastTimer);
    this.copyToast.set(`ID copié : #${id.substring(0, 8).toUpperCase()}...`);
    this.copyToastTimer = setTimeout(() => this.copyToast.set(null), 2500);
  }

  private showToast(message: string, type: 'success' | 'error'): void {
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toast.set({ message, type });
    this.toastTimer = setTimeout(() => this.toast.set(null), 3000);
  }

  private getStatusMeta(status: string): { label: string; color: string; bg: string } {
    switch (status) {
      case 'PENDING':   return { label: 'En attente',  color: '#f59e0b', bg: '#fef3c7' };
      case 'CONFIRMED': return { label: 'Confirmée',   color: '#3b82f6', bg: '#dbeafe' };
      case 'PAID':      return { label: 'Payée',       color: '#10b981', bg: '#d1fae5' };
      case 'FULFILLED': return { label: 'Livrée',      color: '#8b5cf6', bg: '#ede9fe' };
      case 'CANCELLED': return { label: 'Annulée',     color: '#6b7280', bg: '#f3f4f6' };
      default:          return { label: status,        color: '#6b7280', bg: '#f3f4f6' };
    }
  }

  private escapeHtml(str: string): string {
    const div = document.createElement('div');
    div.appendChild(document.createTextNode(str));
    return div.innerHTML;
  }
}
