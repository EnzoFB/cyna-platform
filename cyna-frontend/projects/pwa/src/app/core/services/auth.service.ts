import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, of, tap, catchError, map, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { AuthResponse, AuthUser, JwtPayload } from '../models/auth.model';

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

  login(email: string, password: string): Observable<ApiResponse<AuthResponse>> {
    return this.http
      .post<ApiResponse<AuthResponse>>(`${environment.apiUrl}/auth/login`, { email, password })
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

  refreshToken(): Observable<ApiResponse<AuthResponse>> {
    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token'));
    }

    this._refreshInProgress = true;
    return this.http
      .post<ApiResponse<AuthResponse>>(`${environment.apiUrl}/auth/refresh`, { refreshToken })
      .pipe(
        tap(res => {
          this.handleAuthResponse(res.data);
          this._refreshInProgress = false;
        }),
        catchError(err => {
          this._refreshInProgress = false;
          this.clearSession();
          return throwError(() => err);
        })
      );
  }

  get isRefreshing(): boolean {
    return this._refreshInProgress;
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
      .post<ApiResponse<AuthResponse>>(`${environment.apiUrl}/auth/refresh`, { refreshToken })
      .pipe(
        tap(response => {
          this.handleAuthResponse(response.data);
          this._initialized = true;
          this._refreshInFlight$ = null;
        }),
        map(() => true),
        catchError(() => {
          this.clearSession();
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
