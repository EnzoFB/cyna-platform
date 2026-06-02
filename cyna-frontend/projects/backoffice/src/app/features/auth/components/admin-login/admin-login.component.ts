import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../../core/services/auth.service';
import { TranslateService, TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-admin-login',
  standalone: true,
  imports: [ReactiveFormsModule, TranslatePipe],
  templateUrl: './admin-login.component.html',
  styleUrl: './admin-login.component.scss',
})
export class AdminLoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);

  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly step = signal<'credentials' | 'otp'>('credentials');

  private challengeId: string | null = null;

  protected readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  protected readonly otpForm = this.fb.group({
    otpCode: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
  });

  protected onSubmit(): void {
    if (this.form.invalid) return;

    this.loading.set(true);
    this.errorMessage.set(null);

    const { email, password } = this.form.value;

    this.authService.login(email!, password!).subscribe({
      next: (response) => {
        // Trusted-device fast path: backend skipped OTP because we have a
        // valid device_token cookie. AuthService.login already wired the
        // tokens into state — just navigate.
        if (response.data?.tokens) {
          this.loading.set(false);
          this.router.navigate(['/']);
          return;
        }
        this.challengeId = response.data.challengeId ?? null;
        this.step.set('otp');
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 403) {
          this.errorMessage.set(this.translate.instant('auth.errorForbidden'));
        } else {
          this.errorMessage.set(this.translate.instant('auth.errorInvalidCredentials'));
        }
        this.loading.set(false);
      },
    });
  }

  protected onVerifyOtp(): void {
    if (this.otpForm.invalid || !this.challengeId) return;

    this.loading.set(true);
    this.errorMessage.set(null);

    const { otpCode } = this.otpForm.value;

    this.authService.verifyOtp(this.challengeId, otpCode!).subscribe({
      next: () => {
        this.router.navigate(['/']);
      },
      error: () => {
        this.errorMessage.set(this.translate.instant('auth.errorInvalidOtp'));
        this.loading.set(false);
      },
    });
  }

  protected backToCredentials(): void {
    this.step.set('credentials');
    this.challengeId = null;
    this.errorMessage.set(null);
    this.otpForm.reset();
  }
}
