import { Component, inject, signal, computed, OnInit, OnDestroy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ProductService, AdminProduct, AdminProductDetail } from '../../../../core/services/product.service';
import { CategoryService, AdminCategory } from '../../../../core/services/category.service';

type SortField = 'name' | 'categoryName' | 'priorityLevel' | 'monthlyPrice';
type SortDir   = 'asc' | 'desc';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './product-list.component.html',
  styleUrl: './product-list.component.scss',
})
export class ProductListComponent implements OnInit, OnDestroy {
  private readonly productService  = inject(ProductService);
  private readonly categoryService = inject(CategoryService);

  protected readonly loading        = signal(false);
  protected readonly products       = signal<AdminProduct[]>([]);
  protected readonly totalElements  = signal(0);
  protected readonly currentPage    = signal(0);
  protected readonly pageSize       = signal(20);
  protected readonly searchQuery    = signal('');
  protected readonly expandedId     = signal<string | null>(null);
  protected readonly expandedDetail = signal<AdminProductDetail | null>(null);
  protected readonly detailLoading  = signal(false);
  protected readonly sortField      = signal<SortField | null>(null);
  protected readonly sortDir        = signal<SortDir>('asc');
  protected readonly toast          = signal<{ message: string; type: 'success' | 'error' } | null>(null);
  protected readonly copyToast      = signal<string | null>(null);

  // Filters
  protected readonly filterCategories  = signal<AdminCategory[]>([]);
  protected readonly filterCategoryId  = signal('');
  protected readonly filterPublished   = signal('');
  protected readonly filterAvailable   = signal('');

  protected readonly hasActiveFilters = computed(() =>
    !!this.filterCategoryId() || !!this.filterPublished() || !!this.filterAvailable()
  );

  // Custom dropdowns
  protected readonly openDropdown   = signal<'category' | 'published' | 'available' | null>(null);
  protected readonly categorySearch = signal('');

  protected readonly categoryInputValue = computed(() => {
    const id = this.filterCategoryId();
    if (id) return this.filterCategories().find(c => c.id === id)?.fullName ?? '';
    return this.categorySearch();
  });

  protected readonly filteredCategories = computed(() => {
    if (this.filterCategoryId()) return this.filterCategories();
    const q = this.categorySearch().toLowerCase().trim();
    if (!q) return this.filterCategories();
    return this.filterCategories().filter(c =>
      c.fullName.toLowerCase().includes(q) || c.name.toLowerCase().includes(q)
    );
  });

  protected readonly publishedLabel = computed(() => {
    switch (this.filterPublished()) {
      case 'true':  return 'Publiés';
      case 'false': return 'Brouillons';
      default:      return 'Tous';
    }
  });

  protected readonly availableLabel = computed(() => {
    switch (this.filterAvailable()) {
      case 'true':  return 'Disponibles';
      case 'false': return 'Indisponibles';
      default:      return 'Tous';
    }
  });

  private toastTimer:     ReturnType<typeof setTimeout> | null = null;
  private copyToastTimer: ReturnType<typeof setTimeout> | null = null;
  private searchTimer:    ReturnType<typeof setTimeout> | null = null;
  private lastExpandedId: string | null = null;

  protected readonly totalPages = computed(() =>
    Math.max(1, Math.ceil(this.totalElements() / this.pageSize()))
  );

  protected readonly paginationFrom = computed(() =>
    this.totalElements() === 0 ? 0 : this.currentPage() * this.pageSize() + 1
  );

  protected readonly paginationTo = computed(() =>
    Math.min((this.currentPage() + 1) * this.pageSize(), this.totalElements())
  );

  protected readonly displayedProducts = computed(() => {
    const field = this.sortField();
    const dir   = this.sortDir();
    let result  = this.products();
    if (!field) return result;

    return [...result].sort((a, b) => {
      let cmp = 0;
      switch (field) {
        case 'name':          cmp = a.name.localeCompare(b.name, 'fr'); break;
        case 'categoryName':  cmp = a.categoryName.localeCompare(b.categoryName, 'fr'); break;
        case 'priorityLevel': cmp = a.priorityLevel - b.priorityLevel; break;
        case 'monthlyPrice':  cmp = a.monthlyPrice - b.monthlyPrice; break;
      }
      return dir === 'asc' ? cmp : -cmp;
    });
  });

  ngOnInit(): void {
    this.categoryService.getCategories().subscribe({
      next: (r) => this.filterCategories.set(r.data),
      error: () => {},
    });
    this.loadProducts();
  }

  ngOnDestroy(): void {
    [this.searchTimer, this.toastTimer, this.copyToastTimer].forEach(t => t && clearTimeout(t));
  }

  protected loadProducts(): void {
    this.loading.set(true);
    const pub = this.filterPublished();
    const avl = this.filterAvailable();
    this.productService.getProducts(this.currentPage(), this.pageSize(), {
      search:     this.searchQuery()     || undefined,
      categoryId: this.filterCategoryId() || undefined,
      published:  pub !== '' ? pub === 'true' : undefined,
      available:  avl !== '' ? avl === 'true' : undefined,
    }).subscribe({
        next: (response) => {
          this.products.set(response.data.items);
          this.totalElements.set(response.data.totalElements);
          this.loading.set(false);
        },
        error: () => {
          this.showToast('Erreur lors du chargement des produits', 'error');
          this.loading.set(false);
        },
      });
  }

  // ── Sort ──────────────────────────────────────────────────────────────────

  protected sortBy(field: SortField): void {
    if (this.sortField() === field) {
      this.sortDir.set(this.sortDir() === 'asc' ? 'desc' : 'asc');
    } else {
      this.sortField.set(field);
      this.sortDir.set('asc');
    }
  }

  // ── Search ────────────────────────────────────────────────────────────────

  protected onSearchChange(value: string): void {
    this.searchQuery.set(value);
    this.expandedId.set(null);
    this.expandedDetail.set(null);
    if (this.searchTimer) clearTimeout(this.searchTimer);
    this.searchTimer = setTimeout(() => {
      this.currentPage.set(0);
      this.loadProducts();
    }, 350);
  }

  // ── Expand / detail ───────────────────────────────────────────────────────

  protected toggleExpand(id: string): void {
    if (this.expandedId() === id) {
      this.expandedId.set(null);
      this.expandedDetail.set(null);
      return;
    }
    this.expandedId.set(id);
    this.expandedDetail.set(null);
    this.detailLoading.set(true);
    this.lastExpandedId = id;

    this.productService.getProductDetail(id).subscribe({
      next: (response) => {
        if (this.lastExpandedId !== id) return;
        this.expandedDetail.set(response.data);
        this.detailLoading.set(false);
      },
      error: () => {
        if (this.lastExpandedId !== id) return;
        this.detailLoading.set(false);
        this.showToast('Erreur lors du chargement des détails', 'error');
      },
    });
  }

  // ── Pagination ────────────────────────────────────────────────────────────

  protected goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages()) return;
    this.currentPage.set(page);
    this.expandedId.set(null);
    this.expandedDetail.set(null);
    this.loadProducts();
  }

  protected onPageSizeChange(size: number): void {
    this.pageSize.set(size);
    this.currentPage.set(0);
    this.expandedId.set(null);
    this.expandedDetail.set(null);
    this.loadProducts();
  }

  // ── Filters ───────────────────────────────────────────────────────────────

  protected onCategoryInputFocus(): void {
    if (this.filterCategoryId()) {
      this.categorySearch.set(this.categoryInputValue());
      this.filterCategoryId.set('');
    }
    this.openDropdown.set('category');
  }

  protected onCategorySearch(value: string): void {
    this.categorySearch.set(value);
    this.filterCategoryId.set('');
    this.openDropdown.set('category');
  }

  protected selectCategory(cat: AdminCategory): void {
    this.filterCategoryId.set(cat.id);
    this.categorySearch.set('');
    this.openDropdown.set(null);
    this.resetPageAndReload();
  }

  protected clearCategoryFilter(): void {
    this.filterCategoryId.set('');
    this.categorySearch.set('');
    this.openDropdown.set(null);
    this.resetPageAndReload();
  }

  protected toggleDropdown(name: 'published' | 'available'): void {
    this.openDropdown.set(this.openDropdown() === name ? null : name);
  }

  protected selectPublished(value: string): void {
    this.filterPublished.set(value);
    this.openDropdown.set(null);
    this.resetPageAndReload();
  }

  protected selectAvailable(value: string): void {
    this.filterAvailable.set(value);
    this.openDropdown.set(null);
    this.resetPageAndReload();
  }

  protected resetFilters(): void {
    this.filterCategoryId.set('');
    this.filterPublished.set('');
    this.filterAvailable.set('');
    this.categorySearch.set('');
    this.openDropdown.set(null);
    this.resetPageAndReload();
  }

  private resetPageAndReload(): void {
    this.currentPage.set(0);
    this.expandedId.set(null);
    this.expandedDetail.set(null);
    this.loadProducts();
  }

  // ── Actions (stubs) ───────────────────────────────────────────────────────

  protected openCreateModal(): void { /* TODO */ }

  protected openEditModal(event: MouseEvent): void {
    event.stopPropagation();
    // TODO
  }

  protected requestDelete(event: MouseEvent): void {
    event.stopPropagation();
    // TODO
  }

  // ── Misc ──────────────────────────────────────────────────────────────────

  protected copyId(id: string, event: MouseEvent): void {
    event.stopPropagation();
    navigator.clipboard.writeText(id).then(() => {
      if (this.copyToastTimer) clearTimeout(this.copyToastTimer);
      this.copyToast.set(`ID copié : ${id}`);
      this.copyToastTimer = setTimeout(() => this.copyToast.set(null), 2500);
    });
  }

  protected formatPrice(amount: number, currency: string): string {
    return new Intl.NumberFormat('fr-FR', { style: 'currency', currency }).format(amount);
  }

  protected formatDate(dateStr: string): string {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }

  protected formatDateTime(dateStr: string): string {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleDateString('fr-FR', {
      day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
    });
  }

  private showToast(message: string, type: 'success' | 'error'): void {
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toast.set({ message, type });
    this.toastTimer = setTimeout(() => this.toast.set(null), 3000);
  }
}
