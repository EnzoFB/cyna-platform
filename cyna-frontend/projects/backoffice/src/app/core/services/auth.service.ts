import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, of, tap, catchError, map, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthTokens } from '../../../../../pwa/src/app/core/models/auth.model';

interface ApiResponse<T> {
  success: boolean;
  data: T;
  timestamp: string;
}

export interface LoginChallenge {
  challengeId: string;
  expiresInSeconds: number;
}

export interface AuthUser {
  id: string;
  email: string;
  roles: string[];
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly _user = signal<AuthUser | null>(null);
  private readonly _accessToken = signal<string | null>(null);
  private _refreshInProgress = false;
  private _initialized = false;
  private _refreshInFlight$: Observable<boolean> | null = null;

  readonly user = this._user.asReadonly();
  readonly isAuthenticated = computed(() => this._accessToken() !== null);

  get accessToken(): string | null {
    return this._accessToken();
  }

  get isRefreshing(): boolean {
    return this._refreshInProgress;
  }

  login(email: string, password: string): Observable<ApiResponse<LoginChallenge>> {
    return this.http
      .post<ApiResponse<LoginChallenge>>(`${environment.apiUrl}/auth/admin/login`, { email, password });
  }

  verifyOtp(challengeId: string, otpCode: string): Observable<ApiResponse<AuthTokens>> {
    return this.http
      .post<ApiResponse<AuthTokens>>(`${environment.apiUrl}/auth/login/verify-otp`, { challengeId, otpCode })
      .pipe(tap(res => this.setTokens(res.data)));
  }

  refreshToken(): Observable<ApiResponse<AuthTokens>> {
    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token'));
    }

    this._refreshInProgress = true;
    return this.http
      .post<ApiResponse<AuthTokens>>(`${environment.apiUrl}/auth/refresh`, { refreshToken })
      .pipe(
        tap(res => {
          this.setTokens(res.data);
          this._refreshInProgress = false;
        }),
        catchError(err => {
          this._refreshInProgress = false;
          this.clearSession();
          return throwError(() => err);
        })
      );
  }

  logout(): void {
    const refreshToken = localStorage.getItem('refreshToken');
    if (refreshToken) {
      this.http
        .post(`${environment.apiUrl}/auth/logout`, { refreshToken })
        .subscribe({ error: () => {} });
    }
    this.clearSession();
    void this.router.navigate(['/login']);
  }

  setTokens(tokens: AuthTokens): void {
    this._accessToken.set(tokens.accessToken);
    localStorage.setItem('refreshToken', tokens.refreshToken);
    this._user.set(this.decodeUser(tokens.accessToken));
    this._initialized = true;
  }

  restoreSession(): Observable<boolean> {
    if (this._initialized) {
      return of(this.isAuthenticated());
    }

    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) {
      this._initialized = true;
      return of(false);
    }

    if (this._refreshInFlight$) {
      return this._refreshInFlight$;
    }

    this._refreshInFlight$ = this.http
      .post<ApiResponse<AuthTokens>>(`${environment.apiUrl}/auth/refresh`, { refreshToken })
      .pipe(
        tap(response => {
          this.setTokens(response.data);
          this._initialized = true;
          this._refreshInFlight$ = null;
        }),
        map(() => true),
        catchError(() => {
          localStorage.removeItem('refreshToken');
          this._initialized = true;
          this._refreshInFlight$ = null;
          return of(false);
        }),
      );

    return this._refreshInFlight$;
  }

  private clearSession(): void {
    this._user.set(null);
    this._accessToken.set(null);
    localStorage.removeItem('refreshToken');
  }

  private decodeUser(token: string): AuthUser | null {
    try {
      const parts = token.split('.');
      if (parts.length !== 3) return null;
      const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
      const decoded = JSON.parse(atob(base64));
      const roles = Array.isArray(decoded.roles) ? decoded.roles : [decoded.roles ?? 'ADMIN'];
      return {
        id: decoded.sub ?? decoded.id ?? '',
        email: decoded.email ?? '',
        roles,
      };
    } catch {
      return null;
    }
  }
}
