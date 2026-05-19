import {
  Component,
  inject,
  signal,
  computed,
  OnInit,
  OnDestroy,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { UserService, AdminUser } from '../../../../core/services/user.service';
import { UserFormModalComponent, UserFormData } from '../user-form-modal/user-form-modal.component';

type SortField = 'lastName' | 'email' | 'role' | 'status';
type SortDir   = 'asc' | 'desc';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [FormsModule, UserFormModalComponent],
  templateUrl: './user-list.component.html',
  styleUrl: './user-list.component.scss',
})
export class UserListComponent implements OnInit, OnDestroy {
  private readonly userService = inject(UserService);

  protected readonly loading       = signal(false);
  protected readonly users         = signal<AdminUser[]>([]);
  protected readonly totalElements = signal(0);
  protected readonly currentPage   = signal(0);
  protected readonly pageSize      = signal(10);
  protected readonly searchQuery   = signal('');
  protected readonly sortField     = signal<SortField | null>(null);
  protected readonly sortDir       = signal<SortDir>('asc');
  protected readonly copyToast     = signal<string | null>(null);
  protected readonly toast         = signal<{ message: string; type: 'success' | 'error' } | null>(null);
  protected readonly modalOpen     = signal(false);
  protected readonly editingUser   = signal<AdminUser | null>(null);

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

  protected readonly filteredUsers = computed(() => {
    const q = this.searchQuery().toLowerCase().trim();
    const raw = this.users();
    const filtered = q
      ? raw.filter(u =>
          u.id.toLowerCase().includes(q) ||
          u.firstName.toLowerCase().includes(q) ||
          u.lastName.toLowerCase().includes(q) ||
          u.email.toLowerCase().includes(q) ||
          (u.phone ?? '').toLowerCase().includes(q) ||
          (u.address ?? '').toLowerCase().includes(q)
        )
      : raw;

    const field = this.sortField();
    const dir   = this.sortDir();
    if (!field) return filtered;

    return [...filtered].sort((a, b) => {
      let cmp = 0;
      switch (field) {
        case 'lastName': cmp = a.lastName.localeCompare(b.lastName, 'fr'); break;
        case 'email':    cmp = a.email.localeCompare(b.email); break;
        case 'role':     cmp = a.role.localeCompare(b.role); break;
        case 'status':   cmp = a.status.localeCompare(b.status); break;
      }
      return dir === 'asc' ? cmp : -cmp;
    });
  });

  ngOnInit(): void {
    this.loadUsers();
  }

  ngOnDestroy(): void {
    [this.toastTimer, this.copyToastTimer].forEach(t => t && clearTimeout(t));
  }

  protected loadUsers(): void {
    this.loading.set(true);
    this.userService.getUsers(this.currentPage(), this.pageSize()).subscribe({
      next: (response) => {
        this.users.set(response.data.items);
        this.totalElements.set(response.data.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }

  protected onSearchChange(value: string): void {
    this.searchQuery.set(value);
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
    this.loadUsers();
  }

  protected onPageSizeChange(size: number): void {
    this.pageSize.set(size);
    this.currentPage.set(0);
    this.loadUsers();
  }

  protected exportCsv(): void {
    const headers = ['ID', 'Nom', 'Prénom', 'Email', 'Téléphone', 'Adresse', 'Rôle', 'Statut'];
    const rows = this.filteredUsers().map(u => [
      u.id,
      u.lastName,
      u.firstName,
      u.email,
      u.phone ?? '',
      u.address ?? '',
      u.role,
      u.status,
    ]);
    const csv = [headers, ...rows]
      .map(r => r.map(v => `"${v.replace(/"/g, '""')}"`).join(','))
      .join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url  = URL.createObjectURL(blob);
    const a    = document.createElement('a');
    a.href     = url;
    a.download = `utilisateurs-${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }

  protected copyId(id: string, event: MouseEvent): void {
    event.stopPropagation();
    navigator.clipboard.writeText(id).then(() => {
      if (this.copyToastTimer) clearTimeout(this.copyToastTimer);
      this.copyToast.set(`ID copié : ${id.substring(0, 9).toUpperCase()}...`);
      this.copyToastTimer = setTimeout(() => this.copyToast.set(null), 2500);
    });
  }

  protected openCreateModal(): void {
    this.editingUser.set(null);
    this.modalOpen.set(true);
  }

  protected openEditModal(user: AdminUser, event: MouseEvent): void {
    event.stopPropagation();
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
        lastName:  data.lastName,
        role:      data.role,
        status:    data.status!,
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
        email:     data.email,
        password:  data.password!,
        firstName: data.firstName,
        lastName:  data.lastName,
        role:      data.role,
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

  protected deleteUser(user: AdminUser, event: MouseEvent): void {
    event.stopPropagation();
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

  private showToast(message: string, type: 'success' | 'error'): void {
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toast.set({ message, type });
    this.toastTimer = setTimeout(() => this.toast.set(null), 3000);
  }
}
