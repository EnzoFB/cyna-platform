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

  loginForm!: FormGroup;
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

    this.authService.login(email, password).subscribe({
      next: res => {
        this.authService.setTokens(res.data);
        const successMessage = this.translate.instant('auth.login-success')
        this.toastService.showSuccess(successMessage);

        void this.router.navigate(['/']);
      },
      error: () => {
        const errorMessage = this.translate.instant('auth.login-failed')
        this.toastService.showError(errorMessage);
      }
    })
  }

  submitRegister() {
    if (this.registerForm.invalid) return;
    console.log(this.registerForm.value)

    const { lastName, firstName, company, email, password } = this.registerForm.value;
    console.log(company);
    //TODO utiliser company
    this.authService.register({email, password, firstName, lastName}).subscribe({
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
      confirmPassword: ['', Validators.required]
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
