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
import { AdminCategory } from '../../../../core/services/category.service';

export interface CategoryFormData {
  name: string;
  fullName: string;
  description: string;
  active: boolean;
  imageFile: File | null;
}

@Component({
  selector: 'app-category-form-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './category-form-modal.component.html',
  styleUrl: './category-form-modal.component.scss',
})
export class CategoryFormModalComponent implements OnChanges {
  @Input() open = false;
  @Input() category: AdminCategory | null = null;
  @Output() closed = new EventEmitter<void>();
  @Output() saved = new EventEmitter<CategoryFormData>();

  form!: FormGroup;
  submitting = false;
  imagePreview: string | null = null;
  selectedFile: File | null = null;

  get isEdit(): boolean {
    return this.category !== null;
  }

  constructor(private fb: FormBuilder) {
    this.buildForm();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['open'] && this.open) {
      this.buildForm();
      this.selectedFile = null;
      this.imagePreview = null;
      this.submitting = false;

      if (this.category) {
        this.form.patchValue({
          fullName:    this.category.fullName,
          name:        this.category.name,
          description: this.category.description ?? '',
          active:      this.category.active,
        });
        if (this.category.imageBase64) {
          this.imagePreview = 'data:image/png;base64,' + this.category.imageBase64;
        }
      }
    }
  }

  private buildForm(): void {
    this.form = this.fb.group({
      fullName:    ['', [Validators.required, Validators.maxLength(255)]],
      name:        ['', [Validators.required, Validators.maxLength(255)]],
      description: ['', Validators.required],
      active:      [true],
    });
  }

  onFileChange(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.selectedFile = file;
    const reader = new FileReader();
    reader.onload = () => { this.imagePreview = reader.result as string; };
    reader.readAsDataURL(file);
  }

  clearSelectedFile(event: MouseEvent): void {
    event.stopPropagation();
    this.selectedFile = null;
    this.imagePreview = this.isEdit && this.category?.imageBase64
      ? 'data:image/png;base64,' + this.category.imageBase64
      : null;
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
    this.saved.emit({ ...this.form.value, imageFile: this.selectedFile });
  }
}
