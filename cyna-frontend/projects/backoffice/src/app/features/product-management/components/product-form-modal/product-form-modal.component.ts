import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  inject,
  HostListener,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  FormArray,
  Validators,
  AbstractControl,
} from '@angular/forms';
import {
  AdminProduct,
  AdminProductDetail,
  ProductService,
} from '../../../../core/services/product.service';
import { CategoryService, AdminCategory } from '../../../../core/services/category.service';

export interface ProductFormData {
  name: string;
  categoryId: string;
  priorityLevel: number;
  serviceDescription: string;
  technicalDescription: string;
  monthlyPrice: number;
  annualPrice: number;
  currency: string;
  freeTrialDays: number;
  highlightPoints: string[];
  isPublished: boolean;
  isAvailable: boolean;
  newImageFiles: File[];
  deletedImageIds: string[];
}

@Component({
  selector: 'app-product-form-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './product-form-modal.component.html',
  styleUrl: './product-form-modal.component.scss',
})
export class ProductFormModalComponent implements OnChanges {
  private readonly fb             = inject(FormBuilder);
  private readonly productService = inject(ProductService);
  private readonly categoryService = inject(CategoryService);

  @Input() open = false;
  @Input() product: AdminProduct | null = null;
  @Output() closed = new EventEmitter<void>();
  @Output() saved  = new EventEmitter<ProductFormData>();

  form!: FormGroup;
  submitting    = false;
  detailLoading = false;

  categories:           AdminCategory[]                 = [];
  existingImages:       { id: string; base64: string }[] = [];
  deletedImageIds:      string[]                        = [];
  pendingImageFiles:    File[]                          = [];
  pendingImagePreviews: string[]                        = [];

  openDropdown: 'category' | 'published' | 'available' | null = null;
  categorySearch = '';

  get filteredCategories(): AdminCategory[] {
    if (this.form?.get('categoryId')?.value) return this.categories;
    const q = this.categorySearch.toLowerCase().trim();
    if (!q) return this.categories;
    return this.categories.filter(c =>
      c.fullName.toLowerCase().includes(q) || c.name.toLowerCase().includes(q)
    );
  }

  get selectedCategoryName(): string {
    const id = this.form?.get('categoryId')?.value;
    if (id) return this.categories.find(c => c.id === id)?.fullName ?? '';
    return this.categorySearch;
  }

  get publicationLabel(): string {
    return this.form?.get('isPublished')?.value ? 'Publié' : 'Brouillon';
  }

  get availabilityLabel(): string {
    return this.form?.get('isAvailable')?.value ? 'Disponible' : 'Indisponible';
  }

  get isEdit(): boolean { return this.product !== null; }

  get highlightPointsArray(): FormArray {
    return this.form.get('highlightPoints') as FormArray;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['open'] && this.open) {
      this.reset();
      this.loadCategories();

      if (this.product) {
        this.detailLoading = true;
        this.productService.getProductDetail(this.product.id).subscribe({
          next:  (r) => { this.fillForm(r.data); this.detailLoading = false; },
          error: ()  => { this.detailLoading = false; },
        });
      }
    }
  }

  private reset(): void {
    this.submitting    = false;
    this.detailLoading = false;
    this.existingImages       = [];
    this.deletedImageIds      = [];
    this.pendingImageFiles    = [];
    this.pendingImagePreviews = [];
    this.openDropdown  = null;
    this.categorySearch = '';
    this.buildForm();
  }

  private buildForm(): void {
    this.form = this.fb.group({
      name:                 ['', [Validators.required, Validators.maxLength(200)]],
      categoryId:           ['', Validators.required],
      priorityLevel:        [0, [Validators.required, Validators.min(0)]],
      serviceDescription:   ['', Validators.required],
      technicalDescription: ['', Validators.required],
      monthlyPrice:         [null, [Validators.required, Validators.min(0)]],
      annualPrice:          [null, [Validators.required, Validators.min(0)]],
      currency:             ['EUR', [Validators.required, Validators.pattern(/^[A-Z]{3}$/)]],
      freeTrialDays:        [0, [Validators.required, Validators.min(0)]],
      highlightPoints:      this.fb.array([]),
      isPublished:          [false],
      isAvailable:          [true],
    });
  }

  private fillForm(detail: AdminProductDetail): void {
    this.form.patchValue({
      name:                 detail.name,
      categoryId:           detail.categoryId,
      priorityLevel:        detail.priorityLevel,
      serviceDescription:   detail.serviceDescription,
      technicalDescription: detail.technicalDescription,
      monthlyPrice:         detail.monthlyPrice,
      annualPrice:          detail.annualPrice,
      currency:             detail.currency,
      freeTrialDays:        detail.freeTrialDays,
      isPublished:          detail.isPublished,
      isAvailable:          detail.isAvailable,
    });
    const arr = this.highlightPointsArray;
    arr.clear();
    detail.highlightPoints.forEach(p => arr.push(this.fb.control(p, Validators.required)));
    this.existingImages = [...detail.images];
  }

  private loadCategories(): void {
    this.categoryService.getCategories().subscribe({
      next:  (r) => (this.categories = r.data),
      error: ()  => {},
    });
  }

  // ── Highlight points ──────────────────────────────────────────────────────

  addHighlightPoint(): void {
    this.highlightPointsArray.push(this.fb.control('', Validators.required));
  }

  removeHighlightPoint(index: number): void {
    this.highlightPointsArray.removeAt(index);
  }

  getHighlightControl(index: number): AbstractControl {
    return this.highlightPointsArray.at(index);
  }

  // ── Images ────────────────────────────────────────────────────────────────

  onImageFilesChange(event: Event): void {
    const files = Array.from((event.target as HTMLInputElement).files ?? []);
    files.forEach(file => {
      this.pendingImageFiles.push(file);
      const reader = new FileReader();
      reader.onload = () => this.pendingImagePreviews.push(reader.result as string);
      reader.readAsDataURL(file);
    });
    (event.target as HTMLInputElement).value = '';
  }

  removePendingImage(index: number): void {
    this.pendingImageFiles.splice(index, 1);
    this.pendingImagePreviews.splice(index, 1);
  }

  removeExistingImage(id: string): void {
    this.deletedImageIds.push(id);
    this.existingImages = this.existingImages.filter(img => img.id !== id);
  }

  // ── Custom dropdowns ──────────────────────────────────────────────────────

  onCategoryInputFocus(): void {
    if (this.form.get('categoryId')?.value) {
      this.categorySearch = this.selectedCategoryName;
      this.form.get('categoryId')?.setValue('');
    }
    this.openDropdown = 'category';
  }

  onCategorySearch(value: string): void {
    this.categorySearch = value;
    this.form.get('categoryId')?.setValue('');
    this.openDropdown = 'category';
  }

  selectCategory(cat: AdminCategory): void {
    this.form.get('categoryId')?.setValue(cat.id);
    this.form.get('categoryId')?.markAsTouched();
    this.categorySearch = '';
    this.openDropdown = null;
  }

  clearCategory(): void {
    this.form.get('categoryId')?.setValue('');
    this.categorySearch = '';
    this.openDropdown = null;
  }

  toggleDropdown(name: 'published' | 'available'): void {
    this.openDropdown = this.openDropdown === name ? null : name;
  }

  selectPublished(value: boolean): void {
    this.form.get('isPublished')?.setValue(value);
    this.openDropdown = null;
  }

  selectAvailable(value: boolean): void {
    this.form.get('isAvailable')?.setValue(value);
    this.openDropdown = null;
  }

  @HostListener('document:mousedown', ['$event'])
  onDocumentMouseDown(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.custom-select')) {
      this.openDropdown = null;
    }
  }

  // ── Submit ────────────────────────────────────────────────────────────────

  onSubmit(): void {
    if (this.form.invalid || this.submitting || this.detailLoading) return;
    this.submitting = true;
    const v = this.form.value;
    this.saved.emit({
      name:                 v.name,
      categoryId:           v.categoryId,
      priorityLevel:        +v.priorityLevel,
      serviceDescription:   v.serviceDescription,
      technicalDescription: v.technicalDescription,
      monthlyPrice:         +v.monthlyPrice,
      annualPrice:          +v.annualPrice,
      currency:             v.currency,
      freeTrialDays:        +v.freeTrialDays,
      highlightPoints:      v.highlightPoints,
      isPublished:          v.isPublished,
      isAvailable:          v.isAvailable,
      newImageFiles:        [...this.pendingImageFiles],
      deletedImageIds:      [...this.deletedImageIds],
    });
  }

  close(): void { this.closed.emit(); }

  onOverlayClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal-overlay')) {
      this.close();
    }
  }
}
