import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, distinctUntilChanged, interval, map, of, startWith, switchMap } from 'rxjs';
import { ProductCardComponent } from '../catalog/components/product-card/product-card.component';
import { CatalogService } from '../catalog/services/catalog.service';
import { Category } from '../catalog/models/category.model';
import { Product } from '../catalog/models/product.model';
import { OfferPromotion } from './models/offer-promotion.model';
import { OfferPromotionService } from './services/offer-promotion.service';

@Component({
  selector: 'app-offers',
  standalone: true,
  imports: [ProductCardComponent, RouterLink, TranslatePipe],
  templateUrl: './offers.component.html',
  styleUrl: './offers.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OffersComponent {
  private readonly catalogService = inject(CatalogService);
  private readonly promotionService = inject(OfferPromotionService);
  private readonly translate = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);

  readonly promotions = signal<readonly OfferPromotion[]>([]);
  readonly isLoadingPromotions = signal(true);
  readonly activePromotionIndex = signal(0);
  readonly categories = signal<readonly Category[]>([]);
  readonly topProducts = signal<readonly Product[]>([]);
  readonly isLoadingCategories = signal(true);
  readonly isLoadingProducts = signal(true);

  readonly activePromotion = computed(() => {
    const items = this.promotions();
    if (items.length === 0) {
      return null;
    }

    const safeIndex = Math.max(0, Math.min(this.activePromotionIndex(), items.length - 1));
    return items[safeIndex] ?? null;
  });

  constructor() {
    this.translate.onLangChange
      .pipe(
        map(event => event.lang),
        startWith(this.translate.currentLang || this.translate.getDefaultLang() || 'fr'),
        distinctUntilChanged(),
        switchMap(language => {
          this.isLoadingPromotions.set(true);
          return this.promotionService.getPromotions(language);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(promotions => {
        this.promotions.set(promotions);
        this.activePromotionIndex.set(0);
        this.isLoadingPromotions.set(false);
      });

    interval(7000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.nextPromotion());

    this.catalogService
      .getCategories()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(categories => {
        this.categories.set(categories.filter(c => c.active));
        this.isLoadingCategories.set(false);
      });

    this.catalogService
      .getProductPage({ page: 0, size: 9, sort: 'priority,desc', published: true, available: true })
      .pipe(
        catchError(() => of({
          items: [],
          page: 0,
          size: 9,
          totalElements: 0,
          totalPages: 0,
          first: true,
          last: true
        })),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(page => {
        this.topProducts.set(page.items);
        this.isLoadingProducts.set(false);
      });
  }

  previousPromotion(): void {
    const count = this.promotions().length;
    if (count <= 1) {
      return;
    }

    this.activePromotionIndex.update(index => (index - 1 + count) % count);
  }

  nextPromotion(): void {
    const count = this.promotions().length;
    if (count <= 1) {
      return;
    }

    this.activePromotionIndex.update(index => (index + 1) % count);
  }

  goToPromotion(index: number): void {
    const count = this.promotions().length;
    if (count === 0 || index < 0 || index >= count) {
      return;
    }

    this.activePromotionIndex.set(index);
  }

  scroll(el: HTMLElement, direction: 'prev' | 'next'): void {
    el.scrollBy({ left: direction === 'prev' ? -360 : 360, behavior: 'smooth' });
  }

  scrollByPage(el: HTMLElement, direction: 'prev' | 'next'): void {
    const pageWidth = el.clientWidth;
    if (pageWidth <= 0) {
      return;
    }

    el.scrollBy({ left: direction === 'prev' ? -pageWidth : pageWidth, behavior: 'smooth' });
  }
}
