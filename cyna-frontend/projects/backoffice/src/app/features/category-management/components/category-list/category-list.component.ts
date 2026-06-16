import { Component, HostListener, inject, signal, computed, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslateService, TranslatePipe } from '@ngx-translate/core';
import { CategoryService, AdminCategory } from '../../../../core/services/category.service';
import { toImageSrc } from '../../../../core/utils/image.utils';
import { CategoryFormModalComponent, CategoryFormData } from '../category-form-modal/category-form-modal.component';

type SortField = 'fullName' | 'active' | 'productCount' | 'updatedAt';
type SortDir   = 'asc' | 'desc';

@Component({
  selector: 'app-category-list',
  standalone: true,
  imports: [FormsModule, CategoryFormModalComponent, TranslatePipe],
  templateUrl: './category-list.component.html',
  styleUrl: './category-list.component.scss',
})
export class CategoryListComponent implements OnInit {
  private readonly categoryService = inject(CategoryService);
  private readonly translate       = inject(TranslateService);

  protected readonly loading         = signal(false);
  protected readonly searchQuery     = signal('');
  protected readonly expandedId      = signal<string | null>(null);
  protected readonly categories      = signal<AdminCategory[]>([]);
  protected readonly sortField       = signal<SortField | null>(null);
  protected readonly sortDir         = signal<SortDir>('asc');
  protected readonly modalOpen       = signal(false);
  protected readonly editingCategory = signal<AdminCategory | null>(null);
  protected readonly toast           = signal<{ message: string; type: 'success' | 'error' } | null>(null);
  protected readonly copyToast       = signal<string | null>(null);

  // Selection
  protected readonly selectedIds = signal<ReadonlySet<string>>(new Set());

  // Delete confirmation — single
  protected readonly lightboxSrc        = signal<string | null>(null);

  protected readonly deleteConfirmTarget = signal<AdminCategory | null>(null);
  protected readonly deleteLoading       = signal(false);

  // Delete confirmation — batch
  protected readonly batchDeleteBlockers    = signal<AdminCategory[]>([]);
  protected readonly showBatchDeleteConfirm = signal(false);
  protected readonly batchDeleteLoading     = signal(false);

  private toastTimer:     ReturnType<typeof setTimeout> | null = null;
  private copyToastTimer: ReturnType<typeof setTimeout> | null = null;

  protected readonly displayedCategories = computed(() => {
    const q     = this.searchQuery().toLowerCase().trim();
    const field = this.sortField();
    const dir   = this.sortDir();

    let result = this.categories();

    if (q) {
      result = result.filter(c =>
        c.fullName.toLowerCase().includes(q) ||
        c.name.toLowerCase().includes(q) ||
        (c.description?.toLowerCase().includes(q) ?? false)
      );
    }

    if (field) {
      result = [...result].sort((a, b) => {
        let cmp = 0;
        switch (field) {
          case 'fullName':     cmp = a.fullName.localeCompare(b.fullName, 'fr'); break;
          case 'active':       cmp = (a.active === b.active) ? 0 : a.active ? -1 : 1; break;
          case 'productCount': cmp = a.productCount - b.productCount; break;
          case 'updatedAt':    cmp = new Date(a.updatedAt).getTime() - new Date(b.updatedAt).getTime(); break;
        }
        return dir === 'asc' ? cmp : -cmp;
      });
    }

    return result;
  });

  protected readonly allDisplayedSelected = computed(() => {
    const displayed = this.displayedCategories();
    const selected  = this.selectedIds();
    return displayed.length > 0 && displayed.every(c => selected.has(c.id));
  });

  protected readonly someDisplayedSelected = computed(() => {
    const displayed = this.displayedCategories();
    const selected  = this.selectedIds();
    return displayed.some(c => selected.has(c.id)) && !this.allDisplayedSelected();
  });

  protected readonly selectionCount = computed(() => this.selectedIds().size);

  ngOnInit(): void {
    this.loadCategories();
  }

  protected loadCategories(): void {
    this.loading.set(true);
    this.categoryService.getCategories().subscribe({
      next: (response) => {
        this.categories.set(response.data);
        this.loading.set(false);
      },
      error: () => {
        this.showToast(this.translate.instant('categories.toast.loadError'), 'error');
        this.loading.set(false);
      },
    });
  }

  // ── Sort ────────────────────────────────────────────────────────────────────

  protected sortBy(field: SortField): void {
    if (this.sortField() === field) {
      this.sortDir.set(this.sortDir() === 'asc' ? 'desc' : 'asc');
    } else {
      this.sortField.set(field);
      this.sortDir.set('asc');
    }
    this.expandedId.set(null);
  }

  // ── Expand ──────────────────────────────────────────────────────────────────

  protected toggleExpand(id: string): void {
    this.expandedId.set(this.expandedId() === id ? null : id);
  }

  // ── Search ──────────────────────────────────────────────────────────────────

  protected onSearchChange(value: string): void {
    this.searchQuery.set(value);
    this.expandedId.set(null);
  }

  // ── Selection ───────────────────────────────────────────────────────────────

  protected toggleSelect(id: string, event: Event): void {
    event.stopPropagation();
    this.selectedIds.update(set => {
      const next = new Set(set);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  protected toggleSelectAll(event: Event): void {
    event.stopPropagation();
    const displayed = this.displayedCategories();
    if (this.allDisplayedSelected()) {
      this.selectedIds.update(set => {
        const next = new Set(set);
        displayed.forEach(c => next.delete(c.id));
        return next;
      });
    } else {
      this.selectedIds.update(set => {
        const next = new Set(set);
        displayed.forEach(c => next.add(c.id));
        return next;
      });
    }
  }

  // ── Create / Edit modal ─────────────────────────────────────────────────────

  protected openCreateModal(): void {
    this.editingCategory.set(null);
    this.modalOpen.set(true);
  }

  protected openEditModal(category: AdminCategory): void {
    this.editingCategory.set(category);
    this.modalOpen.set(true);
  }

  protected closeModal(): void {
    this.modalOpen.set(false);
    this.editingCategory.set(null);
  }

  protected onModalSave(data: CategoryFormData): void {
    const category = this.editingCategory();

    if (category) {
      this.categoryService.updateCategory(category.id, {
        name:         data.name,
        translations: data.translations,
        active:       data.active,
      }).subscribe({
        next: (response) => {
          if (data.imageFile) {
            this.categoryService.uploadCategoryImage(response.data, data.imageFile).subscribe({
              next:  () => this.afterSave(this.translate.instant('categories.toast.updated'), 'success'),
              error: () => this.afterSave(this.translate.instant('categories.toast.updatedWithImageError'), 'error'),
            });
          } else {
            this.afterSave('Catégorie modifiée avec succès', 'success');
          }
        },
        error: () => this.afterSave(this.translate.instant('categories.toast.editError'), 'error'),
      });
    } else {
      this.categoryService.createCategory({
        name:         data.name,
        translations: data.translations,
      }).subscribe({
        next: (response) => {
          if (data.imageFile) {
            this.categoryService.uploadCategoryImage(response.data, data.imageFile).subscribe({
              next:  () => this.afterSave(this.translate.instant('categories.toast.created'), 'success'),
              error: () => this.afterSave(this.translate.instant('categories.toast.createdWithImageError'), 'error'),
            });
          } else {
            this.afterSave('Catégorie créée avec succès', 'success');
          }
        },
        error: () => this.afterSave(this.translate.instant('categories.toast.createError'), 'error'),
      });
    }
  }

  private afterSave(message: string, type: 'success' | 'error'): void {
    this.closeModal();
    this.showToast(message, type);
    this.loadCategories();
  }

  // ── Single delete ───────────────────────────────────────────────────────────

  protected requestDeleteSingle(category: AdminCategory, event: MouseEvent): void {
    event.stopPropagation();
    if (category.productCount > 0) {
      this.showToast(this.translate.instant('categories.toast.hasProducts'), 'error');
      return;
    }
    this.deleteConfirmTarget.set(category);
  }

  protected cancelDeleteSingle(): void {
    this.deleteConfirmTarget.set(null);
  }

  protected confirmDeleteSingle(): void {
    const target = this.deleteConfirmTarget();
    if (!target) return;

    this.deleteLoading.set(true);
    this.categoryService.deleteCategory(target.id).subscribe({
      next: () => {
        this.deleteConfirmTarget.set(null);
        this.deleteLoading.set(false);
        this.selectedIds.update(set => { const next = new Set(set); next.delete(target.id); return next; });
        this.showToast(this.translate.instant('categories.toast.deleted'), 'success');
        this.loadCategories();
      },
      error: () => {
        this.deleteConfirmTarget.set(null);
        this.deleteLoading.set(false);
        this.showToast(this.translate.instant('categories.toast.deleteError'), 'error');
      },
    });
  }

  // ── Batch delete ────────────────────────────────────────────────────────────

  protected requestDeleteBatch(): void {
    const selected   = this.selectedIds();
    const categories = this.categories();
    const selectedCats = categories.filter(c => selected.has(c.id));
    const blockers     = selectedCats.filter(c => c.productCount > 0);

    if (blockers.length > 0) {
      this.batchDeleteBlockers.set(blockers);
    } else {
      this.batchDeleteBlockers.set([]);
      this.showBatchDeleteConfirm.set(true);
    }
  }

  protected cancelBatchDelete(): void {
    this.batchDeleteBlockers.set([]);
    this.showBatchDeleteConfirm.set(false);
  }

  protected confirmDeleteBatch(): void {
    const ids = [...this.selectedIds()];
    this.batchDeleteLoading.set(true);

    let completed = 0;
    let hasError  = false;

    const finish = () => {
      this.showBatchDeleteConfirm.set(false);
      this.batchDeleteLoading.set(false);
      this.selectedIds.set(new Set());
      if (hasError) {
        this.showToast(this.translate.instant('categories.toast.batchPartialError'), 'error');
      } else {
        this.showToast(this.translate.instant('categories.toast.batchDeleted'), 'success');
      }
      this.loadCategories();
    };

    ids.forEach(id => {
      this.categoryService.deleteCategory(id).subscribe({
        next: () => { if (++completed === ids.length) finish(); },
        error: () => { hasError = true; if (++completed === ids.length) finish(); },
      });
    });
  }

  // ── Misc ────────────────────────────────────────────────────────────────────

  protected copyId(id: string, event: MouseEvent): void {
    event.stopPropagation();
    navigator.clipboard.writeText(id).then(() => {
      this.showCopyToast(id);
    });
  }

  protected readonly toImageSrc = toImageSrc;

  protected openLightbox(src: string): void { this.lightboxSrc.set(src); }
  protected closeLightbox(): void           { this.lightboxSrc.set(null); }

  @HostListener('document:keydown.escape')
  onEscape(): void { this.closeLightbox(); }

  protected formatDate(dateStr: string): string {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleDateString('fr-FR', {
      day: '2-digit', month: '2-digit', year: 'numeric',
    });
  }

  protected formatDateTime(dateStr: string): string {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleDateString('fr-FR', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit',
    });
  }

  private showToast(message: string, type: 'success' | 'error'): void {
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toast.set({ message, type });
    this.toastTimer = setTimeout(() => this.toast.set(null), 3000);
  }

  private showCopyToast(id: string): void {
    if (this.copyToastTimer) clearTimeout(this.copyToastTimer);
    this.copyToast.set(`ID copié : ${id}`);
    this.copyToastTimer = setTimeout(() => this.copyToast.set(null), 2500);
  }
}
