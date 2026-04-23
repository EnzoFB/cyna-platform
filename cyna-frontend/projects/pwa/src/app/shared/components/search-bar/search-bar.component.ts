import { Component, DestroyRef, ElementRef, ViewChild, inject, signal, computed } from '@angular/core';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { NgOptimizedImage } from '@angular/common';
import { CatalogService } from '../../../features/catalog/services/catalog.service';
import { Product } from '../../../features/catalog/models/product.model';
import { Category } from '../../../features/catalog/models/category.model';

type SearchResult =
  | { type: 'product'; data: Product }
  | { type: 'category'; data: Category };

@Component({
  selector: 'app-search-bar',
  imports: [NgOptimizedImage, TranslatePipe],
  templateUrl: './search-bar.component.html',
  styleUrl: './search-bar.component.scss',
})
export class SearchBarComponent {
  private readonly router = inject(Router);
  private readonly catalogService = inject(CatalogService);
  private readonly destroyRef = inject(DestroyRef);

  @ViewChild('searchInput') searchInput?: ElementRef<HTMLInputElement>;

  private readonly searchQuery = signal('');
  readonly showDropdown = signal(false);
  private readonly allProducts = signal<readonly Product[]>([]);
  private readonly allCategories = signal<readonly Category[]>([]);
  private dataLoaded = false;
  private readonly searchSubject = new Subject<string>();

  readonly searchResults = computed<SearchResult[]>(() => {
    const query = this.searchQuery().toLowerCase().trim();
    if (query.length < 2) return [];

    const categoryResults: SearchResult[] = this.allCategories()
      .filter(c => c.name.toLowerCase().includes(query))
      .map(c => ({ type: 'category' as const, data: c }));

    const productResults: SearchResult[] = this.allProducts()
      .filter(p => p.name.toLowerCase().includes(query))
      .map(p => ({ type: 'product' as const, data: p }));

    return [...categoryResults, ...productResults];
  });

  constructor() {
    this.searchSubject.pipe(
      debounceTime(200),
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(query => {
      this.searchQuery.set(query);
      if (query.length >= 2) {
        if (!this.dataLoaded) {
          this.loadSearchData();
        }
        this.showDropdown.set(true);
      } else {
        this.showDropdown.set(false);
      }
    });
  }

  onSearchInput(event: Event): void {
    this.searchSubject.next((event.target as HTMLInputElement).value);
  }

  onSearchFocus(): void {
    if (this.searchQuery().length >= 2) {
      this.showDropdown.set(true);
    }
  }

  onSearchBlur(): void {
    setTimeout(() => this.showDropdown.set(false), 150);
  }

  selectResult(result: SearchResult): void {
    if (result.type === 'product') {
      this.router.navigate(['/catalog', result.data.id]);
    } else {
      this.router.navigate(['/catalog'], { queryParams: { categoryId: result.data.id } });
    }
    this.close();
  }

  close(): void {
    this.showDropdown.set(false);
    this.searchQuery.set('');
    if (this.searchInput) {
      this.searchInput.nativeElement.value = '';
    }
  }

  private loadSearchData(): void {
    this.dataLoaded = true;
    this.catalogService.getCategories()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(categories => this.allCategories.set(categories));

    this.catalogService.getProductPage({ page: 0, size: 999, published: true })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(page => this.allProducts.set(page.items));
  }
}
