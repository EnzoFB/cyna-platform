import {
  Component,
  EventEmitter,
  HostListener,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslatePipe } from '@ngx-translate/core';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import { AdminUser } from '../../../../core/services/user.service';
import { OverlayCloseDirective } from '../../../../shared/directives/overlay-close.directive';

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
  imports: [CommonModule, ReactiveFormsModule, TranslatePipe, OverlayCloseDirective],
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
  openDropdown: string | null = null;

  get isEdit(): boolean {
    return this.user !== null;
  }

  constructor(private fb: FormBuilder) {
    this.buildForm(false);
  }

  get roleLabel(): string {
    const v = this.form.get('role')?.value;
    if (!v) return 'users.form.roleSelect';
    return v;
  }

  get statusLabel(): string {
    const v = this.form.get('status')?.value;
    if (v === 'ACTIVE') return 'users.form.statusActive';
    if (v === 'INACTIVE') return 'users.form.statusInactive';
    return 'users.form.roleSelect';
  }

  roleDotClass(): string {
    const v = this.form.get('role')?.value;
    if (v === 'ADMIN')   return 'status-dot--blue';
    if (v === 'SUPPORT') return 'status-dot--purple';
    return 'status-dot--gray';
  }

  @HostListener('document:click')
  onDocumentClick(): void {
    this.openDropdown = null;
  }

  toggleDropdown(name: string, event: Event): void {
    event.stopPropagation();
    this.openDropdown = this.openDropdown === name ? null : name;
  }

  selectRole(value: string): void {
    this.form.get('role')?.setValue(value);
    this.openDropdown = null;
  }

  selectStatus(value: string): void {
    this.form.get('status')?.setValue(value);
    this.openDropdown = null;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['open'] && this.open) {
      this.openDropdown = null;
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


  onSubmit(): void {
    if (this.form.invalid || this.submitting) return;
    this.submitting = true;
    this.saved.emit(this.form.getRawValue());
  }
}
