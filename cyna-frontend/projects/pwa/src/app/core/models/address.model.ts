export interface AddressResponse {
  readonly id: string;
  readonly firstName: string;
  readonly lastName: string;
  readonly label: string;
  readonly address: string;
  readonly address2: string | null;
  readonly zipCode: string;
  readonly city: string;
  readonly region: string;
  readonly countryCode: string;
  readonly phone: string;
  readonly company: string | null;
  readonly vatNumber: string | null;
  readonly isDefault: boolean;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface AddressPayload {
  firstName: string;
  lastName: string;
  label: string;
  address: string;
  address2?: string | null;
  zipCode: string;
  city: string;
  region: string;
  countryCode: string;
  phone: string;
  company?: string | null;
  vatNumber?: string | null;
}
