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
import { AdminCategory, CategoryTranslation } from '../../../../core/services/category.service';
import { toImageSrc } from '../../../../core/utils/image.utils';
import { OverlayCloseDirective } from '../../../../shared/directives/overlay-close.directive';

export interface CategoryFormData {
  name: string;
  translations: Record<string, CategoryTranslation>;
  active: boolean;
  imageFile: File | null;
}

@Component({
  selector: 'app-category-form-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslatePipe, OverlayCloseDirective],
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
  openDropdown: string | null = null;
  activeLocale: 'fr' | 'en' = 'fr';

  get isEdit(): boolean {
    return this.category !== null;
  }

  get frGroup(): FormGroup { return this.form.get('fr') as FormGroup; }
  get enGroup(): FormGroup { return this.form.get('en') as FormGroup; }

  isFrIncomplete(): boolean {
    const g = this.frGroup;
    return !g?.get('fullName')?.value;
  }

  isEnIncomplete(): boolean {
    return this.enGroup?.invalid ?? false;
  }

  get canSubmit(): boolean {
    if (this.submitting) return false;
    return this.form.get('name')?.valid === true && this.frGroup.valid && this.enGroup.valid;
  }

  constructor(private fb: FormBuilder) {
    this.buildForm();
  }

  get activeLabel(): string {
    return this.form.get('active')?.value === true ? 'categories.status.active' : 'categories.status.inactive';
  }

  @HostListener('document:click')
  onDocumentClick(): void {
    this.openDropdown = null;
  }

  toggleDropdown(name: string, event: Event): void {
    event.stopPropagation();
    this.openDropdown = this.openDropdown === name ? null : name;
  }

  selectActive(value: boolean): void {
    this.form.get('active')?.setValue(value);
    this.openDropdown = null;
  }

  switchLocale(locale: 'fr' | 'en'): void {
    this.activeLocale = locale;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['open'] && this.open) {
      this.openDropdown = null;
      this.activeLocale = 'fr';
      this.buildForm();
      this.selectedFile = null;
      this.imagePreview = null;
      this.submitting = false;

      if (this.category) {
        const frT = this.category.translations['fr'];
        const enT = this.category.translations['en'];
        this.form.patchValue({
          name:   this.category.name,
          active: this.category.active,
          fr: {
            fullName:    frT?.fullName ?? '',
            description: frT?.description ?? '',
          },
          en: {
            fullName:    enT?.fullName ?? '',
            description: enT?.description ?? '',
          },
        });
        if (this.category.imageBase64) {
          this.imagePreview = toImageSrc(this.category.imageBase64);
        }
      }
    }
  }

  private buildForm(): void {
    this.form = this.fb.group({
      name:   ['', [Validators.required, Validators.maxLength(255)]],
      active: [true],

      fr: this.fb.group({
        fullName:    ['', [Validators.required, Validators.maxLength(255)]],
        description: [''],
      }),

      en: this.fb.group({
        fullName:    ['', [Validators.required, Validators.maxLength(255)]],
        description: [''],
      }),
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
      ? toImageSrc(this.category.imageBase64)
      : null;
  }

  close(): void {
    this.closed.emit();
  }


  onSubmit(): void {
    if (this.form.invalid || this.submitting) return;
    this.submitting = true;
    const v = this.form.value;
    this.saved.emit({
      name:   v.name,
      translations: {
        fr: { fullName: v.fr.fullName, description: v.fr.description ?? '' },
        en: { fullName: v.en.fullName, description: v.en.description ?? '' },
      },
      active:    v.active,
      imageFile: this.selectedFile,
    });
  }
}
