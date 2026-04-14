import {inject, Injectable} from '@angular/core';
import {map, Observable} from 'rxjs';
import { HttpClient } from '@angular/common/http';

import {Product, ProductDetail} from '../models/product.model';
import {environment} from "../../../../environments/environment";
import {Category} from "../models/category.model";
import {ApiResponse, PagedResponse} from "../../../core/models/api-response.model";

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

  getCategories(): Observable<Category[]> {
    return this.http.get<ApiResponse<Category[]>>(`${environment.apiUrl}/categories`)
      .pipe(
        map(response => response.data)
      );
  }

  getProducts(): Observable<Product[]> {
    return this.http.get<ApiResponse<PagedResponse<Product>>>(`${environment.apiUrl}/products`)
      .pipe(
        map(response => response.data.items)
      );
  }

  getProductById(productId: string): Observable<ProductDetail | null> {
    return this.http.get<ApiResponse<ProductDetail>>(`${environment.apiUrl}/products/${productId}`)
      .pipe(
        map(response => response.data)
      );
  }

  // getSimilarProducts(productId: string, category: string, limit = 4): Observable<readonly Product[]> {
  //   const products = MOCK_PRODUCT_DETAILS
  //     .filter(product => product.id !== productId && product.category === category)
  //     .slice(0, limit);
  //
  //   return of(products);
  // }
}
