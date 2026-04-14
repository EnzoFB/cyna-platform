import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, of, tap, catchError, map } from 'rxjs';
import { environment } from '../../../environments/environment';

interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}

export interface AuthUser {
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

  private _initialized = false;
  private _refreshInFlight$: Observable<boolean> | null = null;

  readonly user = this._user.asReadonly();
  readonly isAuthenticated = computed(() => this._accessToken() !== null);

  get accessToken(): string | null {
    return this._accessToken();
  }

  login(email: string, password: string) {
    return this.http.post<{ data: AuthTokens }>(`${environment.apiUrl}/auth/login`, { email, password });
  }

  logout(): void {
    this._user.set(null);
    this._accessToken.set(null);
    localStorage.removeItem('refreshToken');
    this.router.navigate(['/login']);
  }

  setTokens(tokens: AuthTokens): void {
    this._accessToken.set(tokens.accessToken);
    localStorage.setItem('refreshToken', tokens.refreshToken);
    this._user.set(this.decodeUser(tokens.accessToken));
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

    // Deduplicate in-flight requests
    if (this._refreshInFlight$) {
      return this._refreshInFlight$;
    }

    this._refreshInFlight$ = this.http
      .post<{ data: AuthTokens }>(`${environment.apiUrl}/auth/refresh`, { refreshToken })
      .pipe(
        tap((response) => {
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

  private decodeUser(token: string): AuthUser | null {
    try {
      const payload = token.split('.')[1];
      const decoded = JSON.parse(atob(payload));
      return {
        id: decoded.sub ?? decoded.id ?? '',
        email: decoded.email ?? '',
        role: decoded.role ?? 'ADMIN',
      };
    } catch {
      return null;
    }
  }
}
