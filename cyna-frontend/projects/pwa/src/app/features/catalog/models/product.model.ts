export type ProductCategory = 'all' | 'soc' | 'edr' | 'xdr';
export type ProductSort = 'default' | 'price-asc' | 'price-desc';

export type ProductSecurityCategory = Exclude<ProductCategory, 'all'>;

export interface Product {
  readonly id: string;
  readonly name: string;
  readonly imageUrl: string | null;
  readonly category: ProductSecurityCategory;
  readonly monthlyPrice: number;
  readonly currency: string;
}
