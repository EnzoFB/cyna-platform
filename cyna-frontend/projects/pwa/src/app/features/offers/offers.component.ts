import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, of } from 'rxjs';
import { ProductCardComponent } from '../catalog/components/product-card/product-card.component';
import { CatalogService } from '../catalog/services/catalog.service';
import { Category } from '../catalog/models/category.model';
import { Product } from '../catalog/models/product.model';

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
  private readonly destroyRef = inject(DestroyRef);

  readonly categories = signal<readonly Category[]>([]);
  readonly topProducts = signal<readonly Product[]>([]);
  readonly isLoadingCategories = signal(true);
  readonly isLoadingProducts = signal(true);

  constructor() {
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

  scroll(el: HTMLElement, direction: 'prev' | 'next'): void {
    el.scrollBy({ left: direction === 'prev' ? -360 : 360, behavior: 'smooth' });
  }
}
