// a11y: color-contrast fixes applied to .scss (2026-06-04)
import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, input, signal } from '@angular/core';
import { CurrencyPipe, UpperCasePipe } from '@angular/common';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Product } from '../../models/product.model';
import { toImageSrc } from '../../../../core/utils/image.utils';

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [CurrencyPipe, UpperCasePipe, TranslatePipe],
  templateUrl: './product-card.component.html',
  styleUrl: './product-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProductCardComponent {
  private readonly translate = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);

  readonly product = input.required<Product>();

  private readonly lang = signal(this.translate.currentLang ?? 'fr');

  readonly localizedName = computed(() => {
    const p = this.product();
    const lang = this.lang();
    return p.translations[lang]?.name ?? p.translations['fr']?.name ?? '';
  });

  constructor() {
    this.translate.onLangChange
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(e => this.lang.set(e.lang));
  }

  protected hasMonthlyPromotion(): boolean {
    const current = this.product();
    return typeof current.originalMonthlyPrice === 'number'
      && current.originalMonthlyPrice > current.monthlyPrice;
  }

  protected readonly toImageSrc = toImageSrc;
}
