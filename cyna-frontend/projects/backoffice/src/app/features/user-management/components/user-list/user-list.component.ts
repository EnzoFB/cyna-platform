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
import { UserService, AdminUser } from '../../../../core/services/user.service';
import { UserFormModalComponent, UserFormData } from '../user-form-modal/user-form-modal.component';

ModuleRegistry.registerModules([AllCommunityModule]);

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [AgGridAngular, FormsModule, UserFormModalComponent],
  templateUrl: './user-list.component.html',
  styleUrl: './user-list.component.scss',
})
export class UserListComponent implements OnInit {
  private readonly userService = inject(UserService);

  private gridApi!: GridApi<AdminUser>;

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
  protected readonly copyToast = signal<string | null>(null);
  protected readonly modalOpen = signal(false);
  protected readonly editingUser = signal<AdminUser | null>(null);
  protected readonly toast = signal<{ message: string; type: 'success' | 'error' } | null>(null);
  private copyToastTimer: ReturnType<typeof setTimeout> | null = null;
  private toastTimer: ReturnType<typeof setTimeout> | null = null;
  protected readonly currentPage = signal(0);
  protected readonly pageSize = signal(10);
  protected readonly totalElements = signal(0);
  protected readonly totalPages = signal(0);
  protected readonly rowData = signal<AdminUser[]>([]);

  protected readonly rangeLabel = computed(() => {
    const start = this.currentPage() * this.pageSize() + 1;
    const end = Math.min(start + this.pageSize() - 1, this.totalElements());
    return `${start} - ${end} of ${this.totalPages()} Pages`;
  });

  protected readonly pagesArray = computed(() =>
    Array.from({ length: this.totalPages() }, (_, i) => i + 1)
  );

  protected readonly colDefs: ColDef<AdminUser>[] = [
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
      headerName: 'Full Name',
      field: 'firstName',
      minWidth: 200,
      flex: 2,
      cellRenderer: (params: ICellRendererParams<AdminUser>) => {
        const user = params.data!;
        return `
          <div class="cell-name">
            <div class="cell-name__info">
              <span class="cell-name__id cell-name__id--copyable" data-copy-id="${this.escapeHtml(user.id)}" title="Cliquer pour copier l'ID">ID ${user.id.substring(0, 9).toUpperCase()}...</span>
              <span class="cell-name__full">${this.escapeHtml(user.lastName.toUpperCase())} ${this.escapeHtml(user.firstName)}</span>
            </div>
          </div>`;
      },
    },
    {
      headerName: 'Contact',
      field: 'email',
      minWidth: 200,
      flex: 2,
      cellRenderer: (params: ICellRendererParams<AdminUser>) => {
        const user = params.data!;
        return `
          <div class="cell-contact">
            <span class="cell-contact__email">${this.escapeHtml(user.email)}</span>
            <span class="cell-contact__phone">${this.escapeHtml(user.phone || '—')}</span>
          </div>`;
      },
    },
    // {
    //   headerName: 'Enterprise',
    //   field: 'enterprise',
    //   minWidth: 200,
    //   flex: 2,
    //   cellRenderer: (params: ICellRendererParams<AdminUser>) => {
    //     return `<span class="cell-enter">${this.escapeHtml(params.data!.address.split(',')[0] || '—')}</span>`;
    //   }
    // },
    {
      headerName: 'Address',
      field: 'address',
      minWidth: 220,
      flex: 2.5,
      cellRenderer: (params: ICellRendererParams<AdminUser>) => {
        return `<span class="cell-address">${this.escapeHtml(params.data!.address || '—')}</span>`;
      },
    },
    {
      headerName: 'Role',
      field: 'role',
      minWidth: 80,
    },
    {
      headerName: 'Status',
      field: 'status',
      minWidth: 80,
    },
    {
      headerName: 'Action',
      minWidth: 120,
      maxWidth: 120,
      sortable: false,
      filter: false,
      resizable: false,
      cellRenderer: (_params: ICellRendererParams<AdminUser>) => {
        const id = _params.data!.id;
        return `
          <div class="cell-actions">
            <button class="action-btn action-btn--edit" title="Modifier" data-action="edit" data-user-id="${this.escapeHtml(id)}">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/>
                <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/>
              </svg>
            </button>
            <button class="action-btn action-btn--delete" title="Supprimer" data-action="delete" data-user-id="${this.escapeHtml(id)}">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="3 6 5 6 21 6"/>
                <path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a1 1 0 011-1h4a1 1 0 011 1v2"/>
              </svg>
            </button>
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
    this.loadUsers();
  }

  protected onGridReady(event: GridReadyEvent<AdminUser>): void {
    this.gridApi = event.api;
  }

  protected loadUsers(): void {
    this.loading.set(true);
    this.userService.getUsers(this.currentPage(), this.pageSize()).subscribe({
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

  protected goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages()) return;
    this.currentPage.set(page);
    this.loadUsers();
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
    this.loadUsers();
  }

  protected exportCsv(): void {
    this.gridApi?.exportDataAsCsv({
      fileName: `utilisateurs-${new Date().toISOString().slice(0, 10)}.csv`,
    });
  }

  protected onGridClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;

    // Handle copy ID
    const copyEl = target.closest<HTMLElement>('[data-copy-id]');
    if (copyEl) {
      const userId = copyEl.dataset['copyId'];
      if (userId) {
        navigator.clipboard.writeText(userId).then(() => {
          this.showCopyToast(userId);
        });
      }
      return;
    }

    // Handle action buttons
    const actionEl = target.closest<HTMLElement>('[data-action]');
    if (!actionEl) return;

    const action = actionEl.dataset['action'];
    const userId = actionEl.dataset['userId'];
    if (!action || !userId) return;

    const user = this.rowData().find(u => u.id === userId);
    if (!user) return;

    if (action === 'edit') {
      this.openEditModal(user);
    } else if (action === 'delete') {
      this.deleteUser(user);
    }
  }

  protected openCreateModal(): void {
    this.editingUser.set(null);
    this.modalOpen.set(true);
  }

  protected openEditModal(user: AdminUser): void {
    this.editingUser.set(user);
    this.modalOpen.set(true);
  }

  protected closeModal(): void {
    this.modalOpen.set(false);
    this.editingUser.set(null);
  }

  protected onModalSave(data: UserFormData): void {
    const editUser = this.editingUser();
    if (editUser) {
      this.userService.updateUser(editUser.id, {
        firstName: data.firstName,
        lastName: data.lastName,
        role: data.role,
        status: data.status!,
      }).subscribe({
        next: () => {
          this.closeModal();
          this.showToast('Utilisateur modifié avec succès', 'success');
          this.loadUsers();
        },
        error: () => {
          this.showToast('Erreur lors de la modification', 'error');
          this.closeModal();
        },
      });
    } else {
      this.userService.createUser({
        email: data.email,
        password: data.password!,
        firstName: data.firstName,
        lastName: data.lastName,
        role: data.role,
      }).subscribe({
        next: () => {
          this.closeModal();
          this.showToast('Utilisateur créé avec succès', 'success');
          this.loadUsers();
        },
        error: () => {
          this.showToast('Erreur lors de la création', 'error');
          this.closeModal();
        },
      });
    }
  }

  private deleteUser(user: AdminUser): void {
    if (!confirm(`Supprimer ${user.firstName} ${user.lastName} ?`)) return;

    this.userService.deleteUser(user.id).subscribe({
      next: () => {
        this.showToast('Utilisateur supprimé', 'success');
        this.loadUsers();
      },
      error: () => {
        this.showToast('Erreur lors de la suppression', 'error');
      },
    });
  }

  private showCopyToast(id: string): void {
    if (this.copyToastTimer) clearTimeout(this.copyToastTimer);
    this.copyToast.set(`ID copié : ${id.substring(0, 9).toUpperCase()}...`);
    this.copyToastTimer = setTimeout(() => this.copyToast.set(null), 2500);
  }

  private showToast(message: string, type: 'success' | 'error'): void {
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toast.set({ message, type });
    this.toastTimer = setTimeout(() => this.toast.set(null), 3000);
  }

  private escapeHtml(str: string): string {
    const div = document.createElement('div');
    div.appendChild(document.createTextNode(str));
    return div.innerHTML;
  }
}
