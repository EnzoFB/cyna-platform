export interface Category {
  readonly id: string;
  readonly name: string;
  readonly fullName: string;
  readonly fullNameEn: string;
  readonly description: string;
  readonly descriptionEn: string;
  readonly imageBase64: string | null;
  readonly active: boolean;
  readonly createdAt: string;
  readonly updatedAt: string;
}
