import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';

interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}

interface AuthUser {
  id: string;
  email: string;
  role: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly _user = signal<AuthUser | null>(null);
  private readonly _accessToken = signal<string | null>(null);

  readonly user = this._user.asReadonly();
  readonly isAuthenticated = computed(() => this._accessToken() !== null);

  get accessToken(): string | null {
    return this._accessToken();
  }

  login(email: string, password: string) {
    return this.http.post<{ data: AuthTokens }>(`${environment.apiUrl}/auth/login`, { email, password });
  }

  register(payload: { email: string; password: string; firstName: string; lastName: string }) {
      return this.http.post<{ data: AuthTokens }>(`${environment.apiUrl}/auth/register`, payload);
  }

  logout(): void {
    this._user.set(null);
    this._accessToken.set(null);
    localStorage.removeItem('refreshToken');
    void this.router.navigate(['/auth/login']);
  }

  setTokens(tokens: AuthTokens): void {
    this._accessToken.set(tokens.accessToken);
    localStorage.setItem('refreshToken', tokens.refreshToken);
  }

  checkEmail(email: string) {
    return this.http.get<{ data: boolean }>(`${environment.apiUrl}/account/check-email`, {
      params: { email }
    });
  }
}
