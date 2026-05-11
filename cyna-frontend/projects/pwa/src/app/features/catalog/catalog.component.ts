import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, debounceTime, distinctUntilChanged, map, of, skip, Subject } from 'rxjs';
import { ProductCardComponent } from './components/product-card/product-card.component';
import { Product, ProductSort } from './models/product.model';
import { CatalogService } from './services/catalog.service';
import { Category } from './models/category.model';

interface CatalogOption<TValue extends string> {
  readonly value: TValue;
  readonly labelKey: string;
}

@Component({
  selector: 'app-catalog',
  standalone: true,
  imports: [ProductCardComponent, RouterLink, TranslatePipe],
  templateUrl: './catalog.component.html',
  styleUrl: './catalog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CatalogComponent {
  private readonly catalogService = inject(CatalogService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly pageSize = 9;
  private readonly searchInput$ = new Subject<string>();

  readonly categories = signal<readonly Category[]>([]);

  readonly sortOptions: readonly CatalogOption<ProductSort>[] = [
    { value: 'default', labelKey: 'catalog.sort.default' },
    { value: 'price-asc', labelKey: 'catalog.sort.priceAsc' },
    { value: 'price-desc', labelKey: 'catalog.sort.priceDesc' }
  ];

  readonly isLoading = signal(true);
  readonly products = signal<readonly Product[]>([]);
  readonly selectedCategoryId = signal<string>('all');
  readonly selectedSort = signal<ProductSort>('default');
  readonly currentPage = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly searchInputValue = signal('');
  readonly searchValue = signal('');

  readonly displayedProducts = computed(() => this.products());

  readonly selectedCategoryData = computed(() => {
    const selectedId = this.selectedCategoryId();
    if (selectedId === 'all') {
      return null;
    }

    return this.categories().find(category => category.id === selectedId) ?? null;
  });

  constructor() {
    const initialCategoryId = this.route.snapshot.queryParamMap.get('categoryId');
    const initialSearch = this.route.snapshot.queryParamMap.get('search')?.trim() ?? '';
    if (initialCategoryId) {
      this.selectedCategoryId.set(initialCategoryId);
    }
    this.searchInputValue.set(initialSearch);
    this.searchValue.set(initialSearch);

    this.catalogService
      .getCategories()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(categories => {
        this.categories.set(categories);
      });

    this.loadProducts();

    this.route.queryParams.pipe(
      skip(1),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(params => {
      this.selectedCategoryId.set(params['categoryId'] ?? 'all');
      const search = typeof params['search'] === 'string' ? params['search'].trim() : '';
      this.searchInputValue.set(search);
      this.searchValue.set(search);
      this.currentPage.set(0);
      this.loadProducts();
    });

    this.searchInput$
      .pipe(
        map(value => value.trim()),
        debounceTime(300),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(search => {
        this.searchValue.set(search);
        this.currentPage.set(0);
        this.loadProducts();
      });
  }

  onSortChange(sort: ProductSort): void {
    this.selectedSort.set(sort);
    this.currentPage.set(0);
    this.loadProducts();
  }

  onCategoryChange(categoryId: string): void {
    this.selectedCategoryId.set(categoryId);
    this.currentPage.set(0);
    this.loadProducts();
  }

  onSearchInput(value: string): void {
    this.searchInputValue.set(value);
    this.searchInput$.next(value);
  }

  clearSearch(): void {
    this.searchInputValue.set('');
    this.searchInput$.next('');
  }

  previousPage(): void {
    if (this.currentPage() === 0) {
      return;
    }
    this.currentPage.update(page => page - 1);
    this.loadProducts();
  }

  nextPage(): void {
    if (this.currentPage() + 1 >= this.totalPages()) {
      return;
    }
    this.currentPage.update(page => page + 1);
    this.loadProducts();
  }

  toCategoryImageSrc(category: Category): string {
    if (category.imageBase64) {
      return `data:${this.detectMimeType(category.imageBase64)};base64,${category.imageBase64}`;
    }
    return `/assets/images/catalog/categories/${category.name.toLowerCase()}.svg`;
  }

  private detectMimeType(base64: string): string {
    if (base64.startsWith('/9j/'))   return 'image/jpeg';
    if (base64.startsWith('iVBOR'))  return 'image/png';
    if (base64.startsWith('UklGR'))  return 'image/webp';
    if (base64.startsWith('R0lGO'))  return 'image/gif';
    return 'image/svg+xml';
  }

  private loadProducts(): void {
    this.isLoading.set(true);
    this.catalogService.getProductPage({
      page: this.currentPage(),
      size: this.pageSize,
      categoryId: this.selectedCategoryId() === 'all' ? undefined : this.selectedCategoryId(),
      search: this.searchValue() || undefined,
      sort: this.toApiSort(this.selectedSort()),
      published: true
    })
      .pipe(catchError(() => of({
        items: [],
        page: 0,
        size: this.pageSize,
        totalElements: 0,
        totalPages: 0,
        first: true,
        last: true
      })))
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(page => {
        this.products.set(page.items);
        this.totalPages.set(page.totalPages);
        this.totalElements.set(page.totalElements);
        this.isLoading.set(false);
      });
  }

  private toApiSort(sort: ProductSort): string {
    switch (sort) {
      case 'price-asc':
        return 'price,asc';
      case 'price-desc':
        return 'price,desc';
      default:
        return 'priority,desc';
    }
  }
}
