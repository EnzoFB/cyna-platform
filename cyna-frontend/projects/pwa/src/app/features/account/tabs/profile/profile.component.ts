import {
  ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter,
  inject, Input, OnChanges, Output, SimpleChanges
} from '@angular/core';
import {
  AbstractControl, AsyncValidatorFn, FormBuilder, ReactiveFormsModule,
  ValidatorFn, ValidationErrors, Validators
} from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { catchError, debounceTime, map, of, switchMap } from 'rxjs';
import { UserResponse } from '../../../../core/models/user.model';
import { UserService } from '../../../../core/services/user.service';
import { ToastService } from '../../../../core/services/toast.service';
import { AuthService } from '../../../../core/services/auth.service';

const passwordMatchValidator: ValidatorFn = (form: AbstractControl): ValidationErrors | null => {
  const pw = form.get('newPassword')?.value;
  const confirm = form.get('confirmPassword')?.value;
  if (!pw || !confirm) return null;
  return pw === confirm ? null : { passwordMismatch: true };
};

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [TranslatePipe, ReactiveFormsModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProfileComponent implements OnChanges {
  @Input() profile: UserResponse | null = null;
  @Output() profileUpdated = new EventEmitter<void>();

  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);
  private readonly authService = inject(AuthService);
  private readonly toastService = inject(ToastService);
  private readonly translateService = inject(TranslateService);
  private readonly cdr = inject(ChangeDetectorRef);

  // ── Info form ─────────────────────────────────────────────────────────────
  readonly form = this.fb.group({
    lastName:  ['', Validators.required],
    firstName: ['', Validators.required],
    email:     ['', {
      validators: [
        Validators.email,
        Validators.pattern(/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-z]{2,}$/)
      ],
      asyncValidators: [this.emailAvailableValidator()],
      updateOn: 'blur'
    }],
    company:   [''],
  });

  // ── Password form ──────────────────────────────────────────────────────────
  readonly pwForm = this.fb.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [
      Validators.required,
      Validators.minLength(8),
      Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*])[A-Za-z\d!@#$%^&*]{8,}$/)
    ]],
    confirmPassword: ['', Validators.required],
  }, { validators: passwordMatchValidator });

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['profile'] && this.profile) {
      this.form.patchValue({
        lastName:  this.profile.lastName  ?? '',
        firstName: this.profile.firstName ?? '',
        email:     this.profile.email     ?? '',
        company:   this.profile.company   ?? '',
      }, { emitEvent: false });
      this.form.markAsPristine();
    }
  }

  // ── Info form helpers ──────────────────────────────────────────────────────
  get canSubmit(): boolean { return this.form.dirty && this.form.valid; }
  get emailCtrl()     { return this.form.get('email'); }
  get firstNameCtrl() { return this.form.get('firstName'); }
  get lastNameCtrl()  { return this.form.get('lastName'); }

  submit(): void {
    if (!this.canSubmit || !this.profile) return;

    const { lastName, firstName, email, company } = this.form.value;
    const currentEmail = this.profile.email;
    const newEmail = email?.trim() ?? '';
    const emailChanged = newEmail && newEmail !== currentEmail;

    if (emailChanged) {
      const lang = this.translateService.getCurrentLang() ?? 'fr';
      this.userService.requestEmailChange({ newEmail, lang }).subscribe({
        next: () => {
          this.toastService.showSuccess(this.translateService.instant('account.profile.emailChangeSent'));
          this.form.patchValue({ email: currentEmail }, { emitEvent: false });
          this.form.markAsPristine();
          this.cdr.markForCheck();
        },
        error: () => this.toastService.showError(this.translateService.instant('account.profile.emailChangeError'))
      });
    }

    const hasProfileChange =
      (firstName?.trim() !== this.profile.firstName) ||
      (lastName?.trim()  !== this.profile.lastName)  ||
      ((company ?? '') !== (this.profile.company ?? ''));

    if (hasProfileChange) {
      this.userService.updateProfile({
        firstName: firstName?.trim() || undefined,
        lastName:  lastName?.trim()  || undefined,
        company:   company ?? null,
      }).subscribe({
        next: () => {
          this.toastService.showSuccess(this.translateService.instant('account.profile.saveSuccess'));
          this.form.markAsPristine();
          this.profileUpdated.emit();
          this.cdr.markForCheck();
        },
        error: () => this.toastService.showError(this.translateService.instant('account.profile.saveError'))
      });
    }
  }

  // ── Password form helpers ──────────────────────────────────────────────────
  get canChangePassword(): boolean { return this.pwForm.valid; }
  get newPwCtrl()      { return this.pwForm.get('newPassword'); }
  get newPwValue(): string { return this.newPwCtrl?.value ?? ''; }

  hasMinLength():    boolean { return this.newPwValue.length >= 8; }
  hasLowerUpper():   boolean { return /[a-z]/.test(this.newPwValue) && /[A-Z]/.test(this.newPwValue); }
  hasNumber():       boolean { return /\d/.test(this.newPwValue); }
  hasSpecialChar():  boolean { return /[!@#$%^&*]/.test(this.newPwValue); }

  submitPassword(): void {
    if (!this.canChangePassword) return;

    const { currentPassword, newPassword } = this.pwForm.value;
    this.userService.changePassword(currentPassword!, newPassword!).subscribe({
      next: () => {
        this.toastService.showSuccess(this.translateService.instant('account.profile.passwordSuccess'));
        this.pwForm.reset();
        this.cdr.markForCheck();
      },
      error: () => this.toastService.showError(this.translateService.instant('account.profile.passwordError'))
    });
  }

  private emailAvailableValidator(): AsyncValidatorFn {
    return (control: AbstractControl) => {
      const value = control.value?.trim();
      if (!value || value === this.profile?.email) return of(null);

      return of(value).pipe(
        debounceTime(300),
        switchMap(email =>
          this.authService.checkEmail(email).pipe(
            map(res => (res.data ? { emailTaken: true } : null)),
            catchError(() => of(null))
          )
        )
      );
    };
  }
}
