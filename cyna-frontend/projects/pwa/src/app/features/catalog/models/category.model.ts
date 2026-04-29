export interface Category {
  readonly id: string;
  readonly name: string;
  readonly fullName: string;
  readonly description: string;
  readonly imageBase64: string | null;
  readonly active: boolean;
  readonly createdAt: string;
  readonly updatedAt: string;
}
