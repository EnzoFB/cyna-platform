import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import { AdminUser } from '../../../../core/services/user.service';

export interface UserFormData {
  email: string;
  password?: string;
  firstName: string;
  lastName: string;
  role: string;
  status?: string;
}

@Component({
  selector: 'app-user-form-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './user-form-modal.component.html',
  styleUrl: './user-form-modal.component.scss',
})
export class UserFormModalComponent implements OnChanges {
  @Input() open = false;
  @Input() user: AdminUser | null = null;
  @Output() closed = new EventEmitter<void>();
  @Output() saved = new EventEmitter<UserFormData>();

  form!: FormGroup;
  submitting = false;

  get isEdit(): boolean {
    return this.user !== null;
  }

  constructor(private fb: FormBuilder) {
    this.buildForm(false);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['open'] && this.open) {
      this.buildForm(this.isEdit);
      if (this.user) {
        this.form.patchValue({
          email: this.user.email,
          firstName: this.user.firstName,
          lastName: this.user.lastName,
          role: this.user.role,
          status: this.user.status,
        });
      }
      this.submitting = false;
    }
  }

  private buildForm(isEdit: boolean): void {
    if (isEdit) {
      this.form = this.fb.group({
        email: [{ value: '', disabled: true }],
        firstName: ['', [Validators.required, Validators.minLength(1)]],
        lastName: ['', [Validators.required, Validators.minLength(1)]],
        role: ['CUSTOMER', Validators.required],
        status: ['ACTIVE', Validators.required],
      });
    } else {
      this.form = this.fb.group({
        email: ['', [Validators.required, Validators.email]],
        password: ['', [Validators.required, Validators.minLength(8)]],
        firstName: ['', [Validators.required, Validators.minLength(1)]],
        lastName: ['', [Validators.required, Validators.minLength(1)]],
        role: ['CUSTOMER', Validators.required],
      });
    }
  }

  close(): void {
    this.closed.emit();
  }

  onOverlayClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal-overlay')) {
      this.close();
    }
  }

  onSubmit(): void {
    if (this.form.invalid || this.submitting) return;
    this.submitting = true;
    this.saved.emit(this.form.getRawValue());
  }
}
