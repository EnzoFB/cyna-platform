export interface CategoryTranslation {
  readonly fullName: string;
  readonly description: string;
}

export interface Category {
  readonly id: string;
  readonly name: string;
  readonly translations: Record<string, CategoryTranslation>;
  readonly imageBase64: string | null;
  readonly active: boolean;
  readonly createdAt: string;
  readonly updatedAt: string;
}
