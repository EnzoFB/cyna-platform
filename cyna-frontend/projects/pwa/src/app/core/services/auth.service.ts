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
      .post<ApiResponse<AuthResponse>>(`${environment.apiUrl}/auth/login/verify-otp`, { challengeId, otpCode })
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
      .post<ApiResponse<AuthResponse>>(`${environment.apiUrl}/auth/register`, payload)
      .pipe(tap(res => this.handleAuthResponse(res.data)));
  }

  refreshToken(): Observable<AuthResponse> {
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
   * failure handlers against a parallel login: if verifyOtp / register
   * wrote a brand-new refresh token to localStorage while this request
   * was in flight, applying our (possibly stale) result would either
   * overwrite the fresh session or — worse — clearSession() would wipe
   * a perfectly valid login on a stale 401.
   */
  private executeRefresh(): Observable<AuthResponse> {
    if (this._refreshInFlight$) {
      return this._refreshInFlight$;
    }

    const refreshTokenAtStart = localStorage.getItem('refreshToken');
    if (!refreshTokenAtStart) {
      return throwError(() => new Error('No refresh token'));
    }

    this._refreshInFlight$ = this.http
      .post<ApiResponse<AuthResponse>>(`${environment.apiUrl}/auth/refresh`, { refreshToken: refreshTokenAtStart })
      .pipe(
        map(res => res.data),
        tap(data => {
          if (localStorage.getItem('refreshToken') === refreshTokenAtStart) {
            this.handleAuthResponse(data);
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

  private handleAuthResponse(response: AuthResponse): void {
    this._accessToken.set(response.accessToken);
    localStorage.setItem('refreshToken', response.refreshToken);
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
    localStorage.removeItem('refreshToken');
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
