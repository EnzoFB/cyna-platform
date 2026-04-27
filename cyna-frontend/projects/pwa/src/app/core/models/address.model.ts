export interface AddressResponse {
  readonly id: string;
  readonly label: string;
  readonly address: string;
  readonly address2: string | null;
  readonly zipCode: string;
  readonly city: string;
  readonly region: string;
  readonly countryCode: string;
  readonly phone: string;
  readonly isDefault: boolean;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface AddressPayload {
  label: string;
  address: string;
  address2?: string | null;
  zipCode: string;
  city: string;
  region: string;
  countryCode: string;
  phone: string;
}
