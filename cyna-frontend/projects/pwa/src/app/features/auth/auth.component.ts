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

@Component({
  selector: 'app-auth',
  imports: [
    NgOptimizedImage,
    TranslatePipe,
    ReactiveFormsModule
  ],
  templateUrl: './auth.component.html',
  styleUrl: './auth.component.scss',
})
export class AuthComponent implements OnInit {
  mode: 'login' | 'register' = 'login';
  loginStep: 'credentials' | 'otp' = 'credentials';
  private challengeId: string | null = null;

  loginForm!: FormGroup;
  otpForm!: FormGroup;
  registerForm!: FormGroup;

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
    });
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
          void this.router.navigate(['/']);
          return;
        }
        // Otherwise the user must complete the OTP step.
        this.challengeId = res.data.challengeId ?? null;
        this.loginStep = 'otp';
      },
      error: () => {
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
        void this.router.navigate(['/']);
      },
      error: (err: HttpErrorResponse) => {
        const key = err.status === 401 ? 'auth.otp.error-invalid' : 'auth.login-failed';
        this.toastService.showError(this.translate.instant(key));
      }
    });
  }

  backToLogin() {
    this.loginStep = 'credentials';
    this.challengeId = null;
    this.otpForm.reset();
  }

  submitRegister() {
    if (this.registerForm.invalid) return;

    const { lastName, firstName, company, email, password, acceptTerms } = this.registerForm.value;
    this.authService.register({
      email, password, firstName, lastName, company,
      lang: this.translate.getCurrentLang(),
      acceptTerms: !!acceptTerms
    }).subscribe({
      next: res => {
        this.authService.setTokens(res.data);
        const successMessage = this.translate.instant('auth.register-success')
        this.toastService.showSuccess(successMessage);

        void this.router.navigate(['/']);
      },
      error: () => {
        const errorMessage = this.translate.instant('auth.register-failed');
        this.toastService.showError(errorMessage);
      }
    })
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
