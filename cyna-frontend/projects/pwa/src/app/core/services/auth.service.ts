import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, of, tap, catchError, map, throwError, shareReplay, finalize } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { AuthResponse, AuthUser, JwtPayload } from '../models/auth.model';

export interface LoginChallenge {
  challengeId: string;
  expiresInSeconds: number;
}

/**
 * Refresh-token storage: the refresh token lives in an HttpOnly cookie
 * issued by the backend, unreachable from any JS context (XSS containment).
 * The access token stays in memory only (the {@code _accessToken} signal);
 * the cookie auto-attaches on the next /auth/* call because every such
 * call is sent with {@code withCredentials: true}.
 *
 * Legacy migration: a refresh token left over from the previous
 * localStorage scheme is replayed once on bootstrap (so users stay
 * logged in across the deploy) and then deleted. After that single
 * round-trip the cookie is authoritative.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private static readonly LEGACY_REFRESH_TOKEN_KEY = 'refreshToken';

  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly _user = signal<AuthUser | null>(null);
  private readonly _accessToken = signal<string | null>(null);
  private _initialized = false;
  // Single in-flight /auth/refresh shared by every caller (bootstrap
  // initializer + any on-401 retry from the auth interceptor). shareReplay
  // multicasts the result; finalize clears the slot so subsequent calls
  // trigger a fresh HTTP rather than replaying a stale cached payload.
  private _refreshInFlight$: Observable<AuthResponse> | null = null;

  readonly user = this._user.asReadonly();
  readonly isAuthenticated = computed(() => this._accessToken() !== null);

  get accessToken(): string | null {
    return this._accessToken();
  }

  login(email: string, password: string, lang: string): Observable<ApiResponse<LoginChallenge>> {
    return this.http
      .post<ApiResponse<LoginChallenge>>(`${environment.apiUrl}/auth/login`, { email, password, lang });
  }

  verifyOtp(challengeId: string, otpCode: string): Observable<ApiResponse<AuthResponse>> {
    return this.http
      .post<ApiResponse<AuthResponse>>(
        `${environment.apiUrl}/auth/login/verify-otp`,
        { challengeId, otpCode },
        { withCredentials: true },
      )
      .pipe(tap(res => this.handleAuthResponse(res.data)));
  }

  register(payload: {
    email: string;
    password: string;
    firstName: string;
    lastName: string;
    lang: string;
  }): Observable<ApiResponse<AuthResponse>> {
    return this.http
      .post<ApiResponse<AuthResponse>>(
        `${environment.apiUrl}/auth/register`,
        payload,
        { withCredentials: true },
      )
      .pipe(tap(res => this.handleAuthResponse(res.data)));
  }

  refreshToken(): Observable<AuthResponse> {
    return this.executeRefresh();
  }

  logout(): void {
    // Empty body — backend resolves the refresh token from the cookie.
    // The response's Set-Cookie wipes it client-side; clearSession()
    // wipes the in-memory access token and (defensively) any legacy
    // localStorage entry.
    this.http
      .post(`${environment.apiUrl}/auth/logout`, {}, { withCredentials: true })
      .subscribe({ error: () => {} });
    this.clearSession();
    void this.router.navigate(['/auth/login']);
  }

  setTokens(authResponse: AuthResponse): void {
    this.handleAuthResponse(authResponse);
  }

  checkEmail(email: string): Observable<ApiResponse<boolean>> {
    return this.http.get<ApiResponse<boolean>>(`${environment.apiUrl}/account/check-email`, {
      params: { email },
    });
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

  /**
   * Issues POST /auth/refresh. Body is normally empty (backend reads the
   * refresh_token cookie); as a one-shot migration courtesy, any legacy
   * refresh token still sitting in localStorage from the old scheme is
   * replayed in the body, then wiped — so existing sessions survive the
   * deploy without forcing a re-login.
   */
  private executeRefresh(): Observable<AuthResponse> {
    if (this._refreshInFlight$) {
      return this._refreshInFlight$;
    }

    const legacyToken = localStorage.getItem(AuthService.LEGACY_REFRESH_TOKEN_KEY);
    const body = legacyToken ? { refreshToken: legacyToken } : {};

    this._refreshInFlight$ = this.http
      .post<ApiResponse<AuthResponse>>(`${environment.apiUrl}/auth/refresh`, body, { withCredentials: true })
      .pipe(
        map(res => res.data),
        tap(data => this.handleAuthResponse(data)),
        catchError(err => {
          this.clearSession();
          return throwError(() => err);
        }),
        finalize(() => {
          // Legacy entry has done its job (or the call failed anyway).
          // Either way, never replay it.
          localStorage.removeItem(AuthService.LEGACY_REFRESH_TOKEN_KEY);
          this._refreshInFlight$ = null;
        }),
        shareReplay({ bufferSize: 1, refCount: false }),
      );

    return this._refreshInFlight$;
  }

  private handleAuthResponse(response: AuthResponse): void {
    this._accessToken.set(response.accessToken);
    this._initialized = true;

    const payload = this.decodeJwt(response.accessToken);
    if (payload) {
      this._user.set({
        id: payload.sub,
        email: payload.email,
        roles: payload.roles,
        firstName: payload.firstName,
        lastName: payload.lastName,
      });
    }
  }

  private clearSession(): void {
    this._user.set(null);
    this._accessToken.set(null);
    localStorage.removeItem(AuthService.LEGACY_REFRESH_TOKEN_KEY);
  }

  private decodeJwt(token: string): JwtPayload | null {
    try {
      const parts = token.split('.');
      if (parts.length !== 3) {
        return null;
      }
      const payload = parts[1].replace(/-/g, '+').replace(/_/g, '/');
      const decoded = atob(payload);
      return JSON.parse(decoded) as JwtPayload;
    } catch {
      return null;
    }
  }
}
