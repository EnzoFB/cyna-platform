import { Component, inject } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router, NavigationEnd, ActivatedRoute } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, TranslatePipe],
  template: `
    <div class="shell">
      <!-- Sidebar -->
      <aside class="sidebar">
        <div class="sidebar__logo">
          <img src="logo.png" alt="Cyna" class="sidebar__logo-img" />
        </div>

        <nav class="sidebar__nav">
          <p class="sidebar__section-label">{{ 'shell.nav.menu' | translate }}</p>
          <ul class="sidebar__menu">
            <li>
              <a
                routerLink="/"
                routerLinkActive="sidebar__link--active"
                [routerLinkActiveOptions]="{ exact: true }"
                class="sidebar__link"
              >
                <svg class="sidebar__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/>
                  <rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/>
                </svg>
                {{ 'shell.nav.dashboard' | translate }}
              </a>
            </li>
            <li>
              <a routerLink="/products" routerLinkActive="sidebar__link--active" class="sidebar__link">
                <svg class="sidebar__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M6 2L3 6v14a2 2 0 002 2h14a2 2 0 002-2V6l-3-4z"/>
                  <line x1="3" y1="6" x2="21" y2="6"/>
                  <path d="M16 10a4 4 0 01-8 0"/>
                </svg>
                {{ 'shell.nav.products' | translate }}
              </a>
            </li>
            <li>
              <a routerLink="/categories" routerLinkActive="sidebar__link--active" class="sidebar__link">
                <svg class="sidebar__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/>
                  <line x1="8" y1="18" x2="21" y2="18"/>
                  <line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/>
                  <line x1="3" y1="18" x2="3.01" y2="18"/>
                </svg>
                {{ 'shell.nav.categories' | translate }}
              </a>
            </li>
            <li>
              <a routerLink="/promotions" routerLinkActive="sidebar__link--active" class="sidebar__link">
                <svg class="sidebar__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M20.59 13.41 11 3.83a2 2 0 0 0-1.41-.59H4a2 2 0 0 0-2 2v5.59a2 2 0 0 0 .59 1.41l9.58 9.59a2 2 0 0 0 2.83 0l5.59-5.59a2 2 0 0 0 0-2.83z"/>
                  <line x1="7" y1="7" x2="7.01" y2="7"/>
                </svg>
                {{ 'shell.nav.promotions' | translate }}
              </a>
            </li>
            <li>
              <a routerLink="/users" routerLinkActive="sidebar__link--active" class="sidebar__link">
                <svg class="sidebar__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/>
                  <circle cx="9" cy="7" r="4"/>
                  <path d="M23 21v-2a4 4 0 00-3-3.87"/>
                  <path d="M16 3.13a4 4 0 010 7.75"/>
                </svg>
                {{ 'shell.nav.users' | translate }}
              </a>
            </li>
            <li>
              <a routerLink="/orders" routerLinkActive="sidebar__link--active" class="sidebar__link">
                <svg class="sidebar__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2"/>
                  <rect x="9" y="3" width="6" height="4" rx="1"/>
                  <line x1="9" y1="12" x2="15" y2="12"/><line x1="9" y1="16" x2="12" y2="16"/>
                </svg>
                {{ 'shell.nav.orders' | translate }}
              </a>
            </li>
          </ul>
        </nav>

        <div class="sidebar__footer">
          <a routerLink="/help" routerLinkActive="sidebar__link--active" class="sidebar__link sidebar__link--muted">
            <svg class="sidebar__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"/>
              <path d="M9.09 9a3 3 0 015.83 1c0 2-3 3-3 3"/>
              <line x1="12" y1="17" x2="12.01" y2="17"/>
            </svg>
            {{ 'shell.nav.help' | translate }}
          </a>
        </div>
      </aside>

      <!-- Main area -->
      <div class="main">
        <!-- Header -->
        <header class="header">
          <div class="header__left">
            <h1 class="header__title">{{ pageTitle() | translate }}</h1>
          </div>
          <div class="header__right">
            <button
              class="lang-btn"
              type="button"
              [attr.aria-label]="(currentLang() === 'fr' ? 'shell.header.switchToEn' : 'shell.header.switchToFr') | translate"
              [attr.title]="(currentLang() === 'fr' ? 'shell.header.switchToEn' : 'shell.header.switchToFr') | translate"
              (click)="changeLang()"
            >
              <img [src]="'flags/' + currentLang() + '.svg'" [alt]="currentLang()" class="flag-icon" />
            </button>
            <div class="header__user">
              <div class="header__avatar">
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 12c2.7 0 4.8-2.1 4.8-4.8S14.7 2.4 12 2.4 7.2 4.5 7.2 7.2 9.3 12 12 12zm0 2.4c-3.2 0-9.6 1.6-9.6 4.8v2.4h19.2v-2.4c0-3.2-6.4-4.8-9.6-4.8z"/>
                </svg>
              </div>
              <div class="header__user-info">
                <span class="header__user-name">{{ auth.user()?.email ?? 'Admin' }}</span>
                <span class="header__user-role">{{ 'shell.header.role' | translate }}</span>
              </div>
              <button class="header__logout-btn" (click)="auth.logout()" [title]="'shell.header.logout' | translate">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"/>
                  <polyline points="16 17 21 12 16 7"/>
                  <line x1="21" y1="12" x2="9" y2="12"/>
                </svg>
              </button>
            </div>
          </div>
        </header>

        <!-- Content -->
        <main class="content">
          <router-outlet />
        </main>
      </div>
    </div>
  `,
  styles: [`
    :host { display: block; height: 100vh; }

    .shell {
      display: flex;
      height: 100vh;
      overflow: hidden;
    }

    /* ── Sidebar ──────────────────────────────────────────────── */
    .sidebar {
      width: 220px;
      min-width: 220px;
      background: #141b2d;
      display: flex;
      flex-direction: column;
      color: #8b9bc7;
    }

    .sidebar__logo {
      padding: 28px 24px 20px;
      border-bottom: 1px solid rgba(255,255,255,0.06);
    }

    .sidebar__logo-img {
      height: 32px;
      width: auto;
      display: block;
    }

    .sidebar__nav {
      flex: 1;
      padding: 20px 0;
      overflow-y: auto;
    }

    .sidebar__section-label {
      font-size: 10px;
      font-weight: 600;
      letter-spacing: 1.2px;
      color: #4a5a8a;
      padding: 0 24px;
      margin: 0 0 8px;
      text-transform: uppercase;
    }

    .sidebar__menu {
      list-style: none;
      margin: 0;
      padding: 0;
    }

    .sidebar__menu li { margin: 2px 0; }

    .sidebar__link {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 10px 24px;
      font-size: 14px;
      color: #8b9bc7;
      text-decoration: none;
      border-radius: 0;
      transition: background 0.15s, color 0.15s;
      cursor: pointer;

      &:hover {
        background: rgba(255,255,255,0.06);
        color: #c8d4f0;
      }

      &--active {
        background: rgba(79,115,245,0.15);
        color: #ffffff;
        border-left: 3px solid #4f73f5;
        padding-left: 21px;
      }

      &--muted {
        color: #4a5a8a;
        &:hover { color: #8b9bc7; }
      }
    }

    .sidebar__icon {
      width: 18px;
      height: 18px;
      flex-shrink: 0;
    }

    .sidebar__footer {
      padding: 16px 0;
      border-top: 1px solid rgba(255,255,255,0.06);
    }

    /* ── Main ─────────────────────────────────────────────────── */
    .main {
      flex: 1;
      display: flex;
      flex-direction: column;
      overflow: hidden;
    }

    /* ── Header ───────────────────────────────────────────────── */
    .header {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 0 32px;
      height: 72px;
      min-height: 72px;
      background: #1a2d52;
      color: #ffffff;
    }

    .header__left { flex: 1; }

    .header__title {
      font-size: 22px;
      font-weight: 600;
      color: #ffffff;
      margin: 0;
    }

    .header__right { display: flex; align-items: center; gap: 16px; }

    .header__user {
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .header__avatar {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      background: rgba(255,255,255,0.15);
      display: flex;
      align-items: center;
      justify-content: center;
      overflow: hidden;

      svg { width: 26px; height: 26px; color: rgba(255,255,255,0.8); }
    }

    .header__user-info {
      display: flex;
      flex-direction: column;
      line-height: 1.3;
    }

    .header__user-name { font-size: 13px; font-weight: 600; color: #ffffff; }
    .header__user-role { font-size: 11px; color: rgba(255,255,255,0.6); }

    .lang-btn {
      width: 40px;
      height: 40px;
      border-radius: 12px;
      border: 1px solid rgba(255, 255, 255, 0.25);
      background: rgba(255, 255, 255, 0.08);
      display: inline-flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      padding: 0;
      transition: background-color 0.18s ease, border-color 0.18s ease, transform 0.18s ease;

      &:hover {
        background: rgba(255, 255, 255, 0.16);
        border-color: rgba(255, 255, 255, 0.4);
        transform: translateY(-1px);
      }
    }

    .flag-icon {
      width: 24px;
      height: 16px;
      object-fit: cover;
      border-radius: 4px;
      display: block;
    }

    .header__logout-btn {
      background: transparent;
      border: none;
      cursor: pointer;
      padding: 6px;
      border-radius: 6px;
      display: flex;
      color: rgba(255,255,255,0.6);
      transition: color 0.15s;

      svg { width: 18px; height: 18px; }
      &:hover { color: #ffffff; }
    }

    /* ── Content ──────────────────────────────────────────────── */
    .content {
      flex: 1;
      overflow-y: auto;
      background: #f0f3f9;
    }
  `],
})
export class ShellComponent {
  protected readonly auth = inject(AuthService);

  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly translate = inject(TranslateService);

  protected readonly currentLang = toSignal(
    this.translate.onLangChange.pipe(
      map(event => event.lang),
    ),
    { initialValue: this.translate.getCurrentLang() || this.translate.getDefaultLang() || 'fr' }
  );

  readonly pageTitle = toSignal(
    this.router.events.pipe(
      filter(e => e instanceof NavigationEnd),
      map(() => this.resolveTitle())
    ),
    { initialValue: 'Dashboard' }
  );

  private resolveTitle(): string {
    let route = this.activatedRoute;
    while (route.firstChild) route = route.firstChild;
    return route.snapshot.data['title'] ?? 'Dashboard';
  }

  protected changeLang(): void {
    const newLang = this.currentLang() === 'fr' ? 'en' : 'fr';
    this.translate.use(newLang);
    localStorage.setItem('lang', newLang);
  }
}
