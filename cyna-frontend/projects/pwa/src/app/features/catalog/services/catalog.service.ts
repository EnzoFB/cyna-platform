import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { Product, ProductSecurityCategory } from '../models/product.model';

const DEFAULT_TIER_LABELS = ['Standard', 'Advanced', 'Ultimate', 'Monitoring'] as const;
const DEFAULT_SECURITY_CATEGORIES: readonly ProductSecurityCategory[] = ['soc', 'edr', 'xdr'];
const DEFAULT_STARTING_PRICE = 300;
const DEFAULT_PRICE_STEP = 50;
const DEFAULT_CURRENCY = 'EUR';

const createCategoryPlaceholder = (label: string): string => {
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" width="640" height="360" viewBox="0 0 640 360">
      <rect width="640" height="360" fill="#e7edf4"/>
      <text x="320" y="190" text-anchor="middle" fill="#4f76b4" font-size="48" font-family="Arial, sans-serif">
        ${label}
      </text>
    </svg>
  `;
  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;
};

const CATEGORY_PLACEHOLDER_IMAGE: Record<ProductSecurityCategory, string> = {
  soc: createCategoryPlaceholder('SOC'),
  edr: createCategoryPlaceholder('EDR'),
  xdr: createCategoryPlaceholder('XDR')
};

const MOCK_PRODUCTS: readonly Product[] = DEFAULT_TIER_LABELS.flatMap((tierLabel, tierIndex) => {
  const monthlyPrice = DEFAULT_STARTING_PRICE + DEFAULT_PRICE_STEP * tierIndex;

  return DEFAULT_SECURITY_CATEGORIES.map(category => ({
    id: `${category}-${tierLabel.toLowerCase()}`,
    name: `${category.toUpperCase()} ${tierLabel}`,
    imageUrl: CATEGORY_PLACEHOLDER_IMAGE[category],
    category,
    monthlyPrice,
    currency: DEFAULT_CURRENCY
  }));
});

@Injectable({ providedIn: 'root' })
export class CatalogService {
  getProducts(): Observable<readonly Product[]> {
    return of(MOCK_PRODUCTS);
  }
}
