import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, of, tap, catchError, map, throwError, shareReplay, finalize } from 'rxjs';
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

/**
 * Refresh-token storage mirrors the PWA: HttpOnly cookie issued by the
 * backend, no localStorage. Access token stays in memory only. All
 * /auth/* calls are sent with {@code withCredentials: true} so the
 * cookie auto-attaches.
 *
 * Any legacy refresh token left over from the localStorage scheme is
 * replayed once on bootstrap then deleted, so the deploy doesn't force
 * a re-login.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private static readonly LEGACY_REFRESH_TOKEN_KEY = 'refreshToken';

  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly _user = signal<AuthUser | null>(null);
  private readonly _accessToken = signal<string | null>(null);
  private _initialized = false;
  private _refreshInFlight$: Observable<AuthTokens> | null = null;

  readonly user = this._user.asReadonly();
  readonly isAuthenticated = computed(() => this._accessToken() !== null);

  get accessToken(): string | null {
    return this._accessToken();
  }

  login(email: string, password: string): Observable<ApiResponse<LoginChallenge>> {
    return this.http
      .post<ApiResponse<LoginChallenge>>(`${environment.apiUrl}/auth/admin/login`, { email, password });
  }

  verifyOtp(challengeId: string, otpCode: string): Observable<ApiResponse<AuthTokens>> {
    return this.http
      .post<ApiResponse<AuthTokens>>(
        `${environment.apiUrl}/auth/login/verify-otp`,
        { challengeId, otpCode },
        { withCredentials: true },
      )
      .pipe(tap(res => this.setTokens(res.data)));
  }

  refreshToken(): Observable<AuthTokens> {
    return this.executeRefresh();
  }

  logout(): void {
    // Empty body — backend resolves the refresh token from the cookie.
    this.http
      .post(`${environment.apiUrl}/auth/logout`, {}, { withCredentials: true })
      .subscribe({ error: () => {} });
    this.clearSession();
    void this.router.navigate(['/login']);
  }

  setTokens(tokens: AuthTokens): void {
    this._accessToken.set(tokens.accessToken);
    this._user.set(this.decodeUser(tokens.accessToken));
    this._initialized = true;
  }

  restoreSession(): Observable<boolean> {
    if (this._initialized) {
      return of(this.isAuthenticated());
    }

    return this.executeRefresh().pipe(
      tap(() => { this._initialized = true; }),
      map(() => true),
      catchError(() => {
        this._initialized = true;
        return of(false);
      }),
    );
  }

  private executeRefresh(): Observable<AuthTokens> {
    if (this._refreshInFlight$) {
      return this._refreshInFlight$;
    }

    const legacyToken = localStorage.getItem(AuthService.LEGACY_REFRESH_TOKEN_KEY);
    const body = legacyToken ? { refreshToken: legacyToken } : {};

    this._refreshInFlight$ = this.http
      .post<ApiResponse<AuthTokens>>(`${environment.apiUrl}/auth/refresh`, body, { withCredentials: true })
      .pipe(
        map(res => res.data),
        tap(data => this.setTokens(data)),
        catchError(err => {
          this.clearSession();
          return throwError(() => err);
        }),
        finalize(() => {
          localStorage.removeItem(AuthService.LEGACY_REFRESH_TOKEN_KEY);
          this._refreshInFlight$ = null;
        }),
        shareReplay({ bufferSize: 1, refCount: false }),
      );

    return this._refreshInFlight$;
  }

  private clearSession(): void {
    this._user.set(null);
    this._accessToken.set(null);
    localStorage.removeItem(AuthService.LEGACY_REFRESH_TOKEN_KEY);
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
