import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ProductCardComponent } from './components/product-card/product-card.component';
import { Product, ProductCategory, ProductSort } from './models/product.model';
import { CatalogService } from './services/catalog.service';

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
  readonly categoryOptions: readonly CatalogOption<ProductCategory>[] = [
    { value: 'all', labelKey: 'catalog.filters.allCategories' },
    { value: 'soc', labelKey: 'catalog.filters.soc' },
    { value: 'edr', labelKey: 'catalog.filters.edr' },
    { value: 'xdr', labelKey: 'catalog.filters.xdr' }
  ];

  readonly sortOptions: readonly CatalogOption<ProductSort>[] = [
    { value: 'default', labelKey: 'catalog.sort.default' },
    { value: 'price-asc', labelKey: 'catalog.sort.priceAsc' },
    { value: 'price-desc', labelKey: 'catalog.sort.priceDesc' }
  ];

  readonly isLoading = signal(true);
  readonly products = signal<readonly Product[]>([]);
  readonly selectedCategory = signal<ProductCategory>('all');
  readonly selectedSort = signal<ProductSort>('default');

  readonly displayedProducts = computed(() => {
    const currentCategory = this.selectedCategory();
    const currentSort = this.selectedSort();

    const filteredProducts = this.products().filter(product =>
      currentCategory === 'all' ? true : product.category === currentCategory
    );

    switch (currentSort) {
      case 'price-asc':
        return [...filteredProducts].sort((left, right) => left.monthlyPrice - right.monthlyPrice);
      case 'price-desc':
        return [...filteredProducts].sort((left, right) => right.monthlyPrice - left.monthlyPrice);
      default:
        return filteredProducts;
    }
  });

  private readonly catalogService = inject(CatalogService);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    this.catalogService
      .getProducts()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(products => {
        this.products.set(products);
        this.isLoading.set(false);
      });
  }

  onCategoryChange(category: ProductCategory): void {
    this.selectedCategory.set(category);
  }

  onSortChange(sort: ProductSort): void {
    this.selectedSort.set(sort);
  }
}
