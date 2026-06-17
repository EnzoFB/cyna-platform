import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {NgOptimizedImage} from "@angular/common";
import {TranslatePipe, TranslateService} from "@ngx-translate/core";
import {
  AbstractControl, AsyncValidatorFn,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators
} from "@angular/forms";
import {AuthService} from "../../core/services/auth.service";
import {ToastService} from "../../core/services/toast.service";
import {catchError, debounceTime, map, of, switchMap} from "rxjs";
import {HttpErrorResponse} from "@angular/common/http";
import { IconComponent } from '../../shared/components/icon/icon.component';

@Component({
  selector: 'app-auth',
  imports: [
    NgOptimizedImage,
    TranslatePipe,
    ReactiveFormsModule,
    IconComponent
  ],
  templateUrl: './auth.component.html',
  styleUrl: './auth.component.scss',
})
export class AuthComponent implements OnInit {
  mode: 'login' | 'register' = 'login';
  loginStep: 'credentials' | 'otp' | 'forgot' = 'credentials';
  private challengeId: string | null = null;
  forgotSent = false;
  // After a successful registration we show a "check your email" panel instead
  // of logging in — the account is pending email verification.
  registrationPending = false;
  // Email to use when re-sending a confirmation (from register or a blocked
  // login of an unverified account).
  pendingEmail: string | null = null;
  // True when a login was refused because the account email isn't verified yet
  // (backend EMAIL_NOT_VERIFIED) — surfaces a "resend confirmation" action.
  loginEmailNotVerified = false;
  resendConfirmationSent = false;
  // Internal app path to return to after a successful login (set by authGuard
  // via the ?returnUrl= query param). Defaults to home.
  private returnUrl: string | null = null;

  loginForm!: FormGroup;
  otpForm!: FormGroup;
  registerForm!: FormGroup;
  forgotForm!: FormGroup;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder,
    private authService: AuthService,
    private toastService: ToastService,
    private translate: TranslateService
  ) {}

  ngOnInit() {
    this.initForms();

    this.route.queryParams.subscribe(params => {
      const modeParam = params['mode'];
      if (modeParam === 'login' || modeParam === 'register') {
        this.mode = modeParam;
      }
      this.returnUrl = this.sanitizeReturnUrl(params['returnUrl']);
    });
  }

  // Only honour internal, absolute app paths. Rejects external URLs (open
  // redirect protection) and loops back to the auth pages.
  private sanitizeReturnUrl(value: unknown): string | null {
    if (typeof value !== 'string' || !value.startsWith('/') || value.startsWith('//')) {
      return null;
    }
    if (value.startsWith('/auth')) {
      return null;
    }
    return value;
  }

  // Send the freshly-authenticated user to the page they originally requested,
  // falling back to home.
  private navigateAfterAuth() {
    void this.router.navigateByUrl(this.returnUrl ?? '/');
  }

  submitLogin() {
    if (this.loginForm.invalid) return;

    const { email, password } = this.loginForm.value;

    this.authService.login(email, password, this.translate.currentLang ?? this.translate.defaultLang).subscribe({
      next: res => {
        // Trusted-device fast path: backend skipped OTP because we have a
        // valid device_token cookie. AuthService.login already wired the
        // tokens into state via handleAuthResponse — just navigate.
        if (res.data?.tokens) {
          const successMessage = this.translate.instant('auth.login-success');
          this.toastService.showSuccess(successMessage);
          this.navigateAfterAuth();
          return;
        }
        // Otherwise the user must complete the OTP step.
        this.challengeId = res.data.challengeId ?? null;
        this.loginStep = 'otp';
      },
      error: (err: HttpErrorResponse) => {
        // Account exists but the email hasn't been verified yet — offer a resend
        // instead of the generic failure toast.
        if (err.status === 403 && err.error?.error?.code === 'EMAIL_NOT_VERIFIED') {
          this.loginEmailNotVerified = true;
          this.resendConfirmationSent = false;
          this.pendingEmail = email;
          this.toastService.showWarning(this.translate.instant('auth.email-not-verified'));
          return;
        }
        const errorMessage = this.translate.instant('auth.login-failed');
        this.toastService.showError(errorMessage);
      }
    });
  }

  submitOtp() {
    if (this.otpForm.invalid || !this.challengeId) return;

    const { otpCode } = this.otpForm.value;

    this.authService.verifyOtp(this.challengeId, otpCode).subscribe({
      next: () => {
        const successMessage = this.translate.instant('auth.login-success');
        this.toastService.showSuccess(successMessage);
        this.navigateAfterAuth();
      },
      error: (err: HttpErrorResponse) => {
        const key = err.status === 401 ? 'auth.otp.error-invalid' : 'auth.login-failed';
        this.toastService.showError(this.translate.instant(key));
      }
    });
  }

  // Switch between the login and register tabs, clearing any transient
  // verification state so a stale "check your email" / "not verified" panel
  // doesn't linger.
  setMode(target: 'login' | 'register') {
    this.mode = target;
    this.loginEmailNotVerified = false;
    this.registrationPending = false;
    this.resendConfirmationSent = false;
  }

  backToLogin() {
    this.loginStep = 'credentials';
    this.challengeId = null;
    this.forgotSent = false;
    this.loginEmailNotVerified = false;
    this.otpForm.reset();
    this.forgotForm.reset();
  }

  showForgotPassword() {
    this.loginStep = 'forgot';
    this.forgotSent = false;
    this.forgotForm.reset();
    // Pre-fill with whatever the user already typed in the login form.
    this.forgotForm.patchValue({ email: this.loginForm.get('email')?.value ?? '' });
  }

  submitForgot() {
    if (this.forgotForm.invalid) return;

    const { email } = this.forgotForm.value;

    this.authService
      .requestPasswordReset(email, this.translate.getCurrentLang())
      .subscribe({
        // Backend always answers 200 (anti-enumeration). Show the same
        // neutral confirmation whether or not the address exists.
        next: () => { this.forgotSent = true; },
        error: () => { this.forgotSent = true; }
      });
  }

  submitRegister() {
    if (this.registerForm.invalid) return;

    const { lastName, firstName, company, email, password, acceptTerms } = this.registerForm.value;
    this.authService.register({
      email, password, firstName, lastName, company,
      lang: this.translate.getCurrentLang(),
      acceptTerms: !!acceptTerms
    }).subscribe({
      next: () => {
        // Account created but pending verification — no auto-login. Show the
        // "check your email" panel and keep the address for a possible resend.
        this.pendingEmail = email;
        this.registrationPending = true;
        this.resendConfirmationSent = false;
        this.toastService.showSuccess(this.translate.instant('auth.register-success'));
      },
      error: () => {
        const errorMessage = this.translate.instant('auth.register-failed');
        this.toastService.showError(errorMessage);
      }
    })
  }

  // Re-sends the confirmation email for the pending account (from the register
  // panel or a blocked login). Backend always answers 200 (anti-enumeration).
  resendConfirmation() {
    if (!this.pendingEmail) return;
    this.authService
      .resendConfirmation(this.pendingEmail, this.translate.getCurrentLang())
      .subscribe({
        next: () => { this.resendConfirmationSent = true; },
        error: () => { this.resendConfirmationSent = true; }
      });
  }

  get passwordCtrl() {
    return this.registerForm.get('password');
  }

  get passwordValue(): string {
    return this.passwordCtrl?.value || '';
  }

  hasMinLength(): boolean {
    return this.passwordValue.length >= 8;
  }

  hasLowerAndUpper(): boolean {
    return /[a-z]/.test(this.passwordValue) && /[A-Z]/.test(this.passwordValue);
  }

  hasNumber(): boolean {
    return /\d/.test(this.passwordValue);
  }

  hasSpecialChar(): boolean {
    return /[!@#$%^&*]/.test(this.passwordValue);
  }

  private initForms(){
    this.loginForm = this.fb.group({
      email: ['', [
        Validators.required,
        Validators.email,
      ]],
      password: ['', [Validators.required, Validators.minLength(8)]]
    });

    this.otpForm = this.fb.group({
      otpCode: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]]
    });

    this.forgotForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });

    this.registerForm = this.fb.group({
      lastName: ['', Validators.required],
      firstName: ['', Validators.required],
      company: ['', Validators.required],
      email: ['',
        {
          validators: [Validators.required, Validators.email, Validators.pattern(/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-z]{2,}$/)
      ],
          asyncValidators: [emailExistsValidator(this.authService)],
          updateOn: 'blur'
    }],
      password: ['', [
        Validators.required,
        Validators.minLength(8),
        Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*])[A-Za-z\d!@#$%^&*]{8,}$/)
      ]],
      confirmPassword: ['', Validators.required],
      // RGPD Art. 7 — explicit, mandatory acceptance. requiredTrue blocks
      // submit until ticked; the backend re-validates and records the proof.
      acceptTerms: [false, Validators.requiredTrue]
    }, { validators: passwordMatchValidator
    });
  }
}

export const passwordMatchValidator: ValidatorFn = (form: AbstractControl): ValidationErrors | null => {
  const password = form.get('password')?.value;
  const confirmPassword = form.get('confirmPassword')?.value;

  if (!password || !confirmPassword) return null;

  return password === confirmPassword ? null : { passwordMismatch: true };
};

export function emailExistsValidator(authService: AuthService): AsyncValidatorFn {
  return (control: AbstractControl) => {
    if (!control.value) return of(null);

    return of(control.value).pipe(
      debounceTime(300),
      switchMap(email =>
        authService.checkEmail(email).pipe(
          map(res => (res.data ? { emailTaken: true } : null)),
          catchError(() => of(null))
        )
      )
    );
  };
}
