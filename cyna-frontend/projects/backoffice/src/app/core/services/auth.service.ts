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

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly _user = signal<AuthUser | null>(null);
  private readonly _accessToken = signal<string | null>(null);
  private _initialized = false;
  // Single in-flight POST /auth/refresh shared by every caller — both the
  // app-bootstrap restoreSession() and the on-401 refresh from the auth
  // interceptor. Presenting the same refresh token twice trips the backend's
  // reuse-detection (revokes every session + sends a "suspicious activity"
  // email), which used to fire on plain F5 because the two code paths each
  // had their own dedup flag and didn't see each other.
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
      .post<ApiResponse<AuthTokens>>(`${environment.apiUrl}/auth/login/verify-otp`, { challengeId, otpCode })
      .pipe(tap(res => this.setTokens(res.data)));
  }

  refreshToken(): Observable<AuthTokens> {
    return this.executeRefresh();
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
   * Single source of truth for issuing POST /auth/refresh. Any concurrent
   * caller while a refresh is in flight gets back the SAME observable
   * (shareReplay), guaranteeing exactly one network call per rotation.
   * Without this dedup the backend sees the stored refresh token presented
   * twice — the second presentation looks like a stolen-token replay and
   * triggers session-wide revocation.
   *
   * The captured {@code refreshTokenAtStart} guards both the success and
   * failure handlers against a parallel login: if verifyOtp wrote a
   * brand-new refresh token to localStorage while this request was in
   * flight, applying our (possibly stale) result would either overwrite
   * the fresh session or — worse — clearSession() would wipe a perfectly
   * valid login on a stale 401.
   */
  private executeRefresh(): Observable<AuthTokens> {
    if (this._refreshInFlight$) {
      return this._refreshInFlight$;
    }

    const refreshTokenAtStart = localStorage.getItem('refreshToken');
    if (!refreshTokenAtStart) {
      return throwError(() => new Error('No refresh token'));
    }

    this._refreshInFlight$ = this.http
      .post<ApiResponse<AuthTokens>>(`${environment.apiUrl}/auth/refresh`, { refreshToken: refreshTokenAtStart })
      .pipe(
        map(res => res.data),
        tap(data => {
          if (localStorage.getItem('refreshToken') === refreshTokenAtStart) {
            this.setTokens(data);
          }
        }),
        catchError(err => {
          if (localStorage.getItem('refreshToken') === refreshTokenAtStart) {
            this.clearSession();
          }
          return throwError(() => err);
        }),
        finalize(() => { this._refreshInFlight$ = null; }),
        shareReplay({ bufferSize: 1, refCount: false }),
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
