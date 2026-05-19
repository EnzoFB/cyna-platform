import {inject, Injectable} from '@angular/core';
import {map, Observable} from 'rxjs';
import { HttpClient } from '@angular/common/http';

import {Product, ProductDetail} from '../models/product.model';
import {environment} from "../../../../environments/environment";
import {Category} from "../models/category.model";
import {ApiResponse, PagedResponse} from "../../../core/models/api-response.model";

interface ProductApiDto {
  readonly id: string;
  readonly name: string;
  readonly categoryId: string;
  readonly categoryName: string;
  readonly priorityLevel: number;
  readonly monthlyPrice: number;
  readonly annualPrice: number;
  readonly discountedMonthlyPrice?: number | null;
  readonly discountedAnnualPrice?: number | null;
  readonly promotionDiscountPercent?: number | null;
  readonly currency: string;
  readonly primaryImageBase64: string | null;
  readonly isPublished: boolean;
  readonly isAvailable: boolean;
}

interface ProductDetailApiDto extends ProductApiDto {
  readonly serviceDescription: string;
  readonly technicalDescription: string;
  readonly freeTrialDays: number;
  readonly highlightPoints: readonly string[];
  readonly images: readonly { id: string; base64: string }[];
}

// const DEFAULT_TIER_LABELS = ['Standard', 'Advanced', 'Ultimate', 'Monitoring'] as const;
// const DEFAULT_SECURITY_CATEGORIES: readonly string[] = ['SOC', 'EDR', 'XDR'];
// const DEFAULT_STARTING_PRICE = 300;
// const DEFAULT_PRICE_STEP = 50;
// const DEFAULT_CURRENCY = 'EUR';
// const DEFAULT_ANNUAL_DISCOUNT_RATIO = 0.9;

// interface CategoryContent {
//   readonly description: string;
//   readonly technicalDescription: string;
//   readonly highlightPoints: readonly string[];
// }

// const CATEGORY_CONTENT: Record<string, CategoryContent> = {
//   SOC: {
//     description:
//       'Supervision continue 24/7 avec correlation des evenements et traitement priorise des incidents pour votre SI.',
//     technicalDescription:
//       'Protection continue sur endpoints, reseau et cloud avec connecteurs SIEM/ITSM, mise en service rapide et accompagnement operationnel adapte a votre niveau de maturite.',
//     highlightPoints: [
//       'Surveillance en temps reel',
//       'Gestion guidee des alertes',
//       'Journalisation centralisee',
//       'Support analyste securite'
//     ]
//   },
//   EDR: {
//     description:
//       'Protection endpoint avancee avec isolation des postes, remediations et reduction du temps de reponse.',
//     technicalDescription:
//       'Detection comportementale anti-ransomware, agents multi-OS a faible impact, containment automatise et support expert pour accelerer la remediation des incidents.',
//     highlightPoints: [
//       'Detection endpoint intelligente',
//       'Containment automatise',
//       'Remediation assistee',
//       'Rapports de securite exploitables'
//     ]
//   },
//   XDR: {
//     description:
//       'Vision unifiee des menaces sur endpoints, identites, reseau et cloud avec investigation contextualisee.',
//     technicalDescription:
//       'Correlation multi-sources avec API ouvertes, automatisation SOAR et architecture elastique pour piloter la detection et la reponse a grande echelle.',
//     highlightPoints: [
//       'Correlation multi-sources',
//       'Priorisation des menaces',
//       'Automatisation SOAR',
//       'Pilotage securite centralise'
//     ]
//   }
// };

// const createCategoryPlaceholder = (label: string): string => {
//   const svg = `
//     <svg xmlns="http://www.w3.org/2000/svg" width="1280" height="720" viewBox="0 0 1280 720">
//       <rect width="1280" height="720" fill="#071530"/>
//       <rect x="160" y="140" width="960" height="440" fill="none" stroke="#0f67dd" stroke-width="4"/>
//       <circle cx="480" cy="280" r="44" fill="none" stroke="#0f67dd" stroke-width="4"/>
//       <path d="M160 520 L430 370 L570 470 L840 300 L1120 520" fill="none" stroke="#0f67dd" stroke-width="4"/>
//       <text x="640" y="650" text-anchor="middle" fill="#7fb0ff" font-size="64" font-family="Arial, sans-serif">
//         ${label}
//       </text>
//     </svg>
//   `;
//
//   return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;
// };

// const CATEGORY_PLACEHOLDER_IMAGE: Record<string, string> = {
//   SOC: createCategoryPlaceholder('SOC'),
//   EDR: createCategoryPlaceholder('EDR'),
//   XDR: createCategoryPlaceholder('XDR')
// };
//
// const toAnnualMonthlyPrice = (monthlyPrice: number): number =>
//   Math.round(monthlyPrice * DEFAULT_ANNUAL_DISCOUNT_RATIO);

// const MOCK_PRODUCT_DETAILS: readonly ProductDetail[] = DEFAULT_TIER_LABELS.flatMap((tierLabel, tierIndex) => {
//   const monthlyPrice = DEFAULT_STARTING_PRICE + DEFAULT_PRICE_STEP * tierIndex;
//
//   return DEFAULT_SECURITY_CATEGORIES.map(category => {
//     const categoryContent = CATEGORY_CONTENT[category];
//     return {
//       id: `${category}-${tierLabel.toLowerCase()}`,
//       name: `${category.toUpperCase()} ${tierLabel}`,
//       imageUrl: CATEGORY_PLACEHOLDER_IMAGE[category],
//       category,
//       monthlyPrice,
//       annualMonthlyPrice: toAnnualMonthlyPrice(monthlyPrice),
//       annualBillingAvailable: true,
//       availableImmediately: true,
//       description: categoryContent.description,
//       technicalDescription: categoryContent.technicalDescription,
//       highlightPoints: categoryContent.highlightPoints,
//       status: 'ok',
//       priority: 'ok'
//     };
//   });
// });

@Injectable({ providedIn: 'root' })
export class CatalogService {
  private readonly http = inject(HttpClient);

  getProductPage(params: {
    readonly page: number;
    readonly size: number;
    readonly categoryId?: string;
    readonly search?: string;
    readonly sort?: string;
    readonly published?: boolean;
    readonly available?: boolean;
  }): Observable<PagedResponse<Product>> {
    const queryParams: Record<string, string | number | boolean> = {
      page: params.page,
      size: params.size,
      published: params.published ?? true
    };

    if (params.available !== undefined) {
      queryParams['available'] = params.available;
    }
    if (params.categoryId) {
      queryParams['categoryId'] = params.categoryId;
    }
    if (params.search) {
      queryParams['search'] = params.search;
    }
    if (params.sort) {
      queryParams['sort'] = params.sort;
    }

    return this.http
      .get<ApiResponse<PagedResponse<ProductApiDto>>>(`${environment.apiUrl}/products`, {
        params: queryParams,
        headers: { 'Cache-Control': 'no-cache' }
      })
      .pipe(map(response => this.mapProductPage(response.data)));
  }

  getCategories(): Observable<Category[]> {
    return this.http.get<ApiResponse<Category[]>>(`${environment.apiUrl}/categories`)
      .pipe(
        map(response => response.data)
      );
  }

  getProductById(productId: string): Observable<ProductDetail | null> {
    return this.http.get<ApiResponse<ProductDetailApiDto>>(`${environment.apiUrl}/products/${productId}`, {
      headers: { 'Cache-Control': 'no-cache' }
    })
      .pipe(
        map(response => this.mapProductDetail(response.data))
      );
  }

  private mapProductPage(page: PagedResponse<ProductApiDto>): PagedResponse<Product> {
    return {
      ...page,
      items: page.items.map(item => this.mapProduct(item))
    };
  }

  private mapProductDetail(product: ProductDetailApiDto | null | undefined): ProductDetail | null {
    if (!product) {
      return null;
    }
    return {
      ...this.mapProduct(product),
      serviceDescription: product.serviceDescription,
      technicalDescription: product.technicalDescription,
      freeTrialDays: product.freeTrialDays,
      highlightPoints: product.highlightPoints,
      images: product.images
    };
  }

  private mapProduct(product: ProductApiDto): Product {
    const hasMonthlyPromotion = this.isDiscounted(product.monthlyPrice, product.discountedMonthlyPrice);
    const hasAnnualPromotion = this.isDiscounted(product.annualPrice, product.discountedAnnualPrice);

    return {
      id: product.id,
      name: product.name,
      categoryId: product.categoryId,
      categoryName: product.categoryName,
      priorityLevel: product.priorityLevel,
      monthlyPrice: hasMonthlyPromotion ? (product.discountedMonthlyPrice as number) : product.monthlyPrice,
      annualPrice: hasAnnualPromotion ? (product.discountedAnnualPrice as number) : product.annualPrice,
      originalMonthlyPrice: hasMonthlyPromotion ? product.monthlyPrice : null,
      originalAnnualPrice: hasAnnualPromotion ? product.annualPrice : null,
      promotionDiscountPercent: product.promotionDiscountPercent ?? null,
      currency: product.currency,
      primaryImageBase64: product.primaryImageBase64,
      isPublished: product.isPublished,
      isAvailable: product.isAvailable
    };
  }

  private isDiscounted(originalPrice: number, discountedPrice?: number | null): boolean {
    return typeof discountedPrice === 'number' && discountedPrice >= 0 && discountedPrice < originalPrice;
  }

  // getSimilarProducts(productId: string, category: string, limit = 4): Observable<readonly Product[]> {
  //   const products = MOCK_PRODUCT_DETAILS
  //     .filter(product => product.id !== productId && product.category === category)
  //     .slice(0, limit);
  //
  //   return of(products);
  // }
}
