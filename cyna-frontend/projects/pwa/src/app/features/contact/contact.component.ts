import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ContactService } from '../../core/services/contact.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-contact',
  standalone: true,
  imports: [ReactiveFormsModule, TranslatePipe],
  templateUrl: './contact.component.html',
  styleUrl: './contact.component.scss'
})
export class ContactComponent {
  private readonly fb = inject(FormBuilder);
  private readonly contactService = inject(ContactService);
  private readonly toastService = inject(ToastService);
  private readonly translate = inject(TranslateService);

  readonly form: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email]],
    subject: ['', [Validators.required, Validators.maxLength(200)]],
    message: ['', [Validators.required, Validators.maxLength(5000)]]
  });

  isSubmitting = false;
  submittedSuccessfully = false;

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;

    const payload = {
      ...this.form.value,
      lang: this.translate.getCurrentLang() ?? this.translate.defaultLang
    };

    this.contactService.sendContactForm(payload).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.submittedSuccessfully = true;
        this.form.reset();
        const message = this.translate.instant('contact.form.success');
        this.toastService.showSuccess(message);
      },
      error: () => {
        this.isSubmitting = false;
        const message = this.translate.instant('contact.form.error');
        this.toastService.showError(message);
      }
    });
  }

  hasError(controlName: string, errorCode: string): boolean {
    const control = this.form.get(controlName);
    return !!control && control.hasError(errorCode) && control.touched;
  }
}
