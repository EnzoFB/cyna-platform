export interface UserResponse {
  readonly id: string;
  readonly email: string;
  readonly firstName: string;
  readonly lastName: string;
  readonly company: string | null;
  readonly role: string;
  readonly createdAt: string;
}
