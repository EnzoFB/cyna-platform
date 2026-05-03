export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  tokenType: string;
}

export interface AuthUser {
  id: string;
  email: string;
  roles: string[];
  firstName?: string;
  lastName?: string;
}

export interface JwtPayload {
  sub: string;
  email: string;
  roles: string[];
  firstName?: string;
  lastName?: string;
  iss: string;
  iat: number;
  exp: number;
}
