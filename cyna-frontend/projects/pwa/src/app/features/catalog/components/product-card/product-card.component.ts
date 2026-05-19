import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CurrencyPipe, UpperCasePipe } from '@angular/common';
import { TranslatePipe } from '@ngx-translate/core';
import { Product } from '../../models/product.model';

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [CurrencyPipe, UpperCasePipe, TranslatePipe],
  templateUrl: './product-card.component.html',
  styleUrl: './product-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProductCardComponent {
  readonly product = input.required<Product>();

  protected hasMonthlyPromotion(): boolean {
    const current = this.product();
    return typeof current.originalMonthlyPrice === 'number'
      && current.originalMonthlyPrice > current.monthlyPrice;
  }

  protected toImageSrc(base64: string): string {
    let mime = 'image/jpeg';
    if (base64.startsWith('iVBOR')) mime = 'image/png';
    else if (base64.startsWith('PHN2') || base64.startsWith('PD94')) mime = 'image/svg+xml';
    return `data:${mime};base64,${base64}`;
  }
}
