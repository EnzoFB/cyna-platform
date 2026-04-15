import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ProductCardComponent } from './components/product-card/product-card.component';
import { Product, ProductSort } from './models/product.model';
import { CatalogService } from './services/catalog.service';
import {Category} from "./models/category.model";

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

  readonly categories = signal<readonly Category[]>([]);

  readonly sortOptions: readonly CatalogOption<ProductSort>[] = [
    { value: 'default', labelKey: 'catalog.sort.default' },
    { value: 'price-asc', labelKey: 'catalog.sort.priceAsc' },
    { value: 'price-desc', labelKey: 'catalog.sort.priceDesc' }
  ];

  readonly isLoading = signal(true);
  readonly products = signal<readonly Product[]>([]);
  readonly selectedCategory = signal<string>('all');
  readonly selectedSort = signal<ProductSort>('default');

  readonly displayedProducts = computed(() => {
    const currentCategory = this.selectedCategory();
    const currentSort = this.selectedSort();

    const filteredProducts = this.products().filter(product =>
      currentCategory === 'all' ? true : product.categoryName === currentCategory
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

  constructor() {
    this.catalogService
      .getProducts()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(products => {
        console.log(products)
        this.products.set(products);
        this.isLoading.set(false);
      });

    this.catalogService
      .getCategories()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(categories => {
        this.categories.set(categories);
      });
  }

  onSortChange(sort: ProductSort): void {
    this.selectedSort.set(sort);
  }
}
