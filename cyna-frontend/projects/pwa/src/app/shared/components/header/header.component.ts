import { Component, DestroyRef, ElementRef, ViewChild, inject, signal, computed } from '@angular/core';
import { NgOptimizedImage } from "@angular/common";
import { NavigationStart, Router, RouterLink, RouterLinkActive } from "@angular/router";
import { UserMenuComponent } from "../user-menu/user-menu.component";
import { TranslateService, TranslatePipe } from "@ngx-translate/core";
import { CartService } from "../../../core/services/cart.service";
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { CatalogService } from '../../../features/catalog/services/catalog.service';
import { Product } from '../../../features/catalog/models/product.model';
import { Category } from '../../../features/catalog/models/category.model';

type SearchResult =
  | { type: 'product'; data: Product }
  | { type: 'category'; data: Category };

@Component({
  selector: 'app-header',
  imports: [
    NgOptimizedImage,
    RouterLink,
    UserMenuComponent,
    TranslatePipe,
    RouterLinkActive
  ],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss',
})
export class HeaderComponent {
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);
  private readonly cartService = inject(CartService);
  private readonly catalogService = inject(CatalogService);
  private readonly destroyRef = inject(DestroyRef);

  menuOpen = false;
  currentLang: string;
  readonly cartItemsCount;

  @ViewChild('burgerButton') burgerButton?: ElementRef<HTMLButtonElement>;
  @ViewChild(UserMenuComponent) userMenu?: UserMenuComponent;
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
    this.currentLang = this.translate.getCurrentLang();
    this.cartItemsCount = this.cartService.totalItems;
    this.cartService.getOrCreateGuestToken();

    this.router.events.pipe(takeUntilDestroyed()).subscribe(event => {
      if (event instanceof NavigationStart) {
        this.menuOpen = false;
        this.closeDropdown();
      }
    });

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
    this.closeDropdown();
  }

  toggleMenu() {
    this.menuOpen = !this.menuOpen;
    if (this.menuOpen) {
      queueMicrotask(() => this.userMenu?.focusFirstInteractiveElement());
    }
  }

  closeMenu() {
    this.menuOpen = false;
    this.burgerButton?.nativeElement.focus();
  }

  onMenuEscape() {
    this.closeMenu();
  }

  changeLang() {
    const newLang = this.currentLang === 'fr' ? 'en' : 'fr';
    this.translate.use(newLang);
    localStorage.setItem('lang', newLang);
    this.currentLang = newLang;
  }

  private closeDropdown(): void {
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
