import { Component, DestroyRef, ElementRef, ViewChild, inject, signal, computed } from '@angular/core';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, catchError, debounceTime, distinctUntilChanged, map, of, switchMap, tap } from 'rxjs';
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
  readonly isSearching = signal(false);
  private readonly productResults = signal<readonly Product[]>([]);
  private readonly allCategories = signal<readonly Category[]>([]);
  private readonly searchSubject = new Subject<string>();

  readonly searchResults = computed<SearchResult[]>(() => {
    const query = this.searchQuery().toLowerCase().trim();
    if (query.length < 2) {
      return [];
    }

    const categoryResults: SearchResult[] = this.allCategories()
      .filter(c => c.name.toLowerCase().includes(query))
      .map(c => ({ type: 'category' as const, data: c }));

    const productResults: SearchResult[] = this.productResults()
      .map(p => ({ type: 'product' as const, data: p }));

    return [...categoryResults, ...productResults];
  });

  constructor() {
    this.catalogService
      .getCategories()
      .pipe(
        catchError(() => of([])),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(categories => this.allCategories.set(categories));

    this.searchSubject
      .pipe(
        map(value => value.trim()),
        debounceTime(250),
        distinctUntilChanged(),
        tap(query => {
          this.searchQuery.set(query);
          const shouldShowDropdown = query.length >= 2;
          this.showDropdown.set(shouldShowDropdown);
          if (!shouldShowDropdown) {
            this.isSearching.set(false);
            this.productResults.set([]);
          }
        }),
        switchMap(query => {
          if (query.length < 2) {
            return of([] as readonly Product[]);
          }

          this.isSearching.set(true);
          return this.catalogService
            .getProductPage({
              page: 0,
              size: 8,
              published: true,
              search: query,
              sort: 'priority,desc'
            })
            .pipe(
              map(page => page.items),
              catchError(() => of([] as readonly Product[]))
            );
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(products => {
        this.productResults.set(products);
        this.isSearching.set(false);
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

  onSearchEnter(event: Event): void {
    const query = (event.target as HTMLInputElement).value.trim();
    if (query.length < 2) {
      return;
    }

    event.preventDefault();
    this.router.navigate(['/catalog'], { queryParams: { search: query } });
    this.close();
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
    this.searchSubject.next('');
    this.productResults.set([]);
    this.isSearching.set(false);
    if (this.searchInput) {
      this.searchInput.nativeElement.value = '';
    }
  }
}
