import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, of, tap, catchError, map, throwError, shareReplay, finalize, switchMap } from 'rxjs';
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

/**
 * Union shape returned by POST /auth/admin/login. Either the user has to
 * complete the OTP step ({@code challengeId} set) or the browser carries a
 * trusted device cookie and is logged in directly ({@code tokens} set).
 * Fields are optional so test fixtures returning only the OTP shape
 * remain assignable.
 */
export interface LoginResponseBody {
  challengeId?: string | null;
  expiresInSeconds?: number | null;
  tokens?: AuthTokens | null;
}

export interface CsrfTokenResponse {
  token: string;
  headerName: string;
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
  private _csrfToken: string | null = null;
  private _csrfHeaderName: string | null = null;
  private _initialized = false;
  private _refreshInFlight$: Observable<AuthTokens> | null = null;

  readonly user = this._user.asReadonly();
  readonly isAuthenticated = computed(() => this._accessToken() !== null);

  get accessToken(): string | null {
    return this._accessToken();
  }

  login(email: string, password: string): Observable<ApiResponse<LoginResponseBody>> {
    return this.http
      .post<ApiResponse<LoginResponseBody>>(
        `${environment.apiUrl}/auth/admin/login`,
        { email, password },
        // withCredentials so the device_token cookie (if present) reaches
        // the backend — enables the trusted-device fast path that skips OTP.
        { withCredentials: true },
      )
      .pipe(tap(res => {
        if (res.data?.tokens) {
          this.setTokens(res.data.tokens);
        }
      }));
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
    this.ensureCsrfToken()
      .pipe(
        switchMap(csrf =>
          this.http.post(`${environment.apiUrl}/auth/logout`, {}, {
            withCredentials: true,
            headers: new HttpHeaders({ [csrf.headerName]: csrf.token }),
          }),
        ),
      )
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

    this._refreshInFlight$ = this.ensureCsrfToken()
      .pipe(
        switchMap(csrf =>
          this.http.post<ApiResponse<AuthTokens>>(`${environment.apiUrl}/auth/refresh`, body, {
            withCredentials: true,
            headers: new HttpHeaders({ [csrf.headerName]: csrf.token }),
          }),
        ),
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

  private ensureCsrfToken(): Observable<CsrfTokenResponse> {
    if (this._csrfToken && this._csrfHeaderName) {
      return of({ token: this._csrfToken, headerName: this._csrfHeaderName });
    }

    return this.http
      .get<ApiResponse<CsrfTokenResponse>>(`${environment.apiUrl}/auth/csrf`, { withCredentials: true })
      .pipe(
        map(res => res.data),
        tap(csrf => {
          this._csrfToken = csrf.token;
          this._csrfHeaderName = csrf.headerName;
        }),
      );
  }

  private clearSession(): void {
    this._user.set(null);
    this._accessToken.set(null);
    this._csrfToken = null;
    this._csrfHeaderName = null;
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
