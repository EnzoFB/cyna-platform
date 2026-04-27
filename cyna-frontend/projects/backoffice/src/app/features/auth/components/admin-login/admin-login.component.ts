import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-admin-login',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <div class="login-page">
      <!-- Branding -->
      <div class="login-branding">
        <span class="login-branding__logo">LOGO</span>
        <span class="login-branding__name">CYNA</span>
      </div>
      <p class="login-tagline">Plateforme SaaS de Cybersécurité</p>

      <!-- Card -->
      <div class="login-card">
        <h2 class="login-card__title">Connexion Administrateur</h2>
        <p class="login-card__subtitle">Connectez-vous au back-office de CYNA</p>

        @if (errorMessage()) {
          <div class="login-card__error">{{ errorMessage() }}</div>
        }

        <form [formGroup]="form" (ngSubmit)="onSubmit()" class="login-card__form">
          <div class="form-group">
            <label for="email">Email professionnel</label>
            <div class="input-wrapper">
              <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <rect x="2" y="4" width="20" height="16" rx="2"/>
                <path d="M22 4L12 13 2 4"/>
              </svg>
              <input id="email" type="email" formControlName="email" placeholder="email&#64;entreprise.com" />
            </div>
          </div>
          <div class="form-group">
            <label for="password">Mot de passe</label>
            <div class="input-wrapper">
              <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <rect x="3" y="11" width="18" height="11" rx="2"/>
                <path d="M7 11V7a5 5 0 0110 0v4"/>
              </svg>
              <input id="password" type="password" formControlName="password" placeholder="••••••••" />
            </div>
          </div>
          <button type="submit" class="btn-primary" [disabled]="form.invalid || loading()">
            {{ loading() ? 'Connexion...' : 'Se connecter' }}
          </button>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .login-page {
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      background: #d5dbe5;
    }

    .login-branding {
      display: flex;
      align-items: baseline;
      gap: 8px;
      margin-bottom: 8px;
    }

    .login-branding__logo {
      font-size: 16px;
      font-weight: 700;
      color: #36c5ce;
      letter-spacing: 1px;
    }

    .login-branding__name {
      font-size: 32px;
      font-weight: 800;
      color: #1a2d52;
      letter-spacing: 2px;
    }

    .login-tagline {
      font-size: 14px;
      color: #6b7a99;
      margin: 0 0 32px;
    }

    .login-card {
      background: #eaecf1;
      border-radius: 12px;
      padding: 32px 40px 40px;
      width: 100%;
      max-width: 420px;
      box-shadow: 6px 6px 16px rgba(0, 0, 0, 0.08), -4px -4px 12px rgba(255, 255, 255, 0.6);
    }

    .login-card__title {
      font-size: 16px;
      font-weight: 600;
      color: #1a2d52;
      margin: 0 0 4px;
      text-align: center;
    }

    .login-card__subtitle {
      font-size: 13px;
      color: #8b9bc7;
      margin: 0 0 24px;
    }

    .login-card__error {
      background: #fee2e2;
      color: #b91c1c;
      border-radius: 8px;
      padding: 10px 14px;
      font-size: 13px;
      margin-bottom: 16px;
    }

    .form-group {
      margin-bottom: 18px;

      label {
        display: block;
        font-size: 13px;
        font-weight: 600;
        color: #1a2d52;
        margin-bottom: 8px;
      }
    }

    .input-wrapper {
      position: relative;
      display: flex;
      align-items: center;

      .input-icon {
        position: absolute;
        left: 14px;
        width: 18px;
        height: 18px;
        color: #9ca3af;
        pointer-events: none;
      }

      input {
        width: 100%;
        padding: 12px 14px 12px 42px;
        border: 1px solid #d1d5db;
        border-radius: 8px;
        font-size: 14px;
        color: #374151;
        background: #f7f8fa;
        outline: none;
        box-sizing: border-box;
        transition: border-color 0.15s, box-shadow 0.15s;

        &::placeholder {
          color: #9ca3af;
        }

        &:focus {
          border-color: #36c5ce;
          box-shadow: 0 0 0 3px rgba(54, 197, 206, 0.12);
        }
      }
    }

    .btn-primary {
      width: 100%;
      padding: 13px;
      background: linear-gradient(135deg, #36c5ce 0%, #5de0e6 100%);
      color: #ffffff;
      border: none;
      border-radius: 8px;
      font-size: 15px;
      font-weight: 600;
      cursor: pointer;
      margin-top: 6px;
      transition: opacity 0.15s, box-shadow 0.15s;

      &:hover:not(:disabled) {
        opacity: 0.92;
        box-shadow: 0 4px 14px rgba(54, 197, 206, 0.35);
      }

      &:disabled {
        opacity: 0.55;
        cursor: not-allowed;
      }
    }
  `],
})
export class AdminLoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  protected onSubmit(): void {
    if (this.form.invalid) return;

    this.loading.set(true);
    this.errorMessage.set(null);

    const { email, password } = this.form.value;

    this.authService.login(email!, password!).subscribe({
      next: (response) => {
        this.authService.setTokens(response.data);
        this.router.navigate(['/']);
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 403) {
          this.errorMessage.set('Accès réservé aux administrateurs.');
        } else {
          this.errorMessage.set('Email ou mot de passe invalide.');
        }
        this.loading.set(false);
      },
    });
  }
}
