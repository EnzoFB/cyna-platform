import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule,
  ValidationErrors, ValidatorFn, Validators
} from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';

const passwordMatch: ValidatorFn = (form: AbstractControl): ValidationErrors | null => {
  const p = form.get('password')?.value;
  const c = form.get('confirmPassword')?.value;
  if (!p || !c) return null;
  return p === c ? null : { passwordMismatch: true };
};

/**
 * Landing page for the reset link e-mailed by the backend
 * ({@code <APP_PUBLIC_URL>/reset-password?token=...}). Reads the one-shot
 * token from the query string and lets the user set a new password.
 */
@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [ReactiveFormsModule, TranslatePipe, RouterLink],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ResetPasswordComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly translate = inject(TranslateService);

  readonly loading = signal(false);
  readonly token = signal<string | null>(null);

  readonly form: FormGroup = this.fb.group({
    password: ['', [
      Validators.required,
      Validators.minLength(8),
      Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*])[A-Za-z\d!@#$%^&*]{8,}$/)
    ]],
    confirmPassword: ['', Validators.required]
  }, { validators: passwordMatch });

  ngOnInit(): void {
    const t = this.route.snapshot.queryParamMap.get('token');
    this.token.set(t && t.trim().length > 0 ? t : null);
  }

  get passwordValue(): string {
    return this.form.get('password')?.value || '';
  }

  hasMinLength(): boolean { return this.passwordValue.length >= 8; }
  hasLowerAndUpper(): boolean { return /[a-z]/.test(this.passwordValue) && /[A-Z]/.test(this.passwordValue); }
  hasNumber(): boolean { return /\d/.test(this.passwordValue); }
  hasSpecialChar(): boolean { return /[!@#$%^&*]/.test(this.passwordValue); }

  submit(): void {
    const token = this.token();
    if (!token || this.form.invalid || this.loading()) return;

    this.loading.set(true);
    this.authService.resetPassword(token, this.form.value.password).subscribe({
      next: () => {
        this.toast.showSuccess(this.translate.instant('auth.reset.success'));
        void this.router.navigate(['/auth/login']);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        const key = err.status === 400 ? 'auth.reset.error-invalid' : 'auth.reset.error-generic';
        this.toast.showError(this.translate.instant(key));
      }
    });
  }
}
