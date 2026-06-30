import {
  Component,
  EventEmitter,
  HostListener,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslatePipe } from '@ngx-translate/core';
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
  ProductTranslation,
} from '../../../../core/services/product.service';
import { CategoryService, AdminCategory } from '../../../../core/services/category.service';
import { toImageSrc } from '../../../../core/utils/image.utils';
import { OverlayCloseDirective } from '../../../../shared/directives/overlay-close.directive';

interface ExistingImageSlot {
  kind: 'existing';
  id: string;
  base64: string;
}

interface PendingImageSlot {
  kind: 'pending';
  key: string;
  file: File;
  preview: string;
}

type ImageSlot = ExistingImageSlot | PendingImageSlot;

export interface ProductFormData {
  translations: Record<string, ProductTranslation>;
  categoryId: string;
  priorityLevel: number;
  monthlyPrice: number;
  annualPrice: number;
  currency: string;
  freeTrialDays: number;
  isPublished: boolean;
  isAvailable: boolean;
  deletedImageIds: string[];
  imageOrder: ({ kind: 'existing'; id: string } | { kind: 'pending'; file: File })[];
}

@Component({
  selector: 'app-product-form-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslatePipe, OverlayCloseDirective],
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

  categories:      AdminCategory[] = [];
  allImageSlots:   ImageSlot[]     = [];
  deletedImageIds: string[]        = [];
  dragSrcIndex:    number | null   = null;

  activeLocale: 'fr' | 'en' = 'fr';

  openDropdown: 'category' | 'published' | 'available' | null = null;
  categorySearch = '';

  lightboxSrc: string | null = null;

  openLightbox(src: string): void { this.lightboxSrc = src; }
  closeLightbox(): void { this.lightboxSrc = null; }

  @HostListener('document:keydown.escape')
  onEscape(): void { this.closeLightbox(); }

  protected readonly toImageSrc = toImageSrc;

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
    return this.form?.get('isPublished')?.value ? 'products.status.published' : 'products.status.draft';
  }

  get availabilityLabel(): string {
    return this.form?.get('isAvailable')?.value ? 'products.status.available' : 'products.status.unavailable';
  }

  get isEdit(): boolean { return this.product !== null; }

  get frGroup(): FormGroup { return this.form.get('fr') as FormGroup; }
  get enGroup(): FormGroup { return this.form.get('en') as FormGroup; }

  get activeGroup(): FormGroup {
    return this.activeLocale === 'fr' ? this.frGroup : this.enGroup;
  }

  /** Used by addHighlightPoint / removeHighlightPoint (operate on the active locale). */
  get highlightPointsArray(): FormArray {
    return this.activeGroup.get('highlightPoints') as FormArray;
  }

  /** Directly referenced by the FR locale block in the template. */
  get frHighlightPointsArray(): FormArray {
    return this.frGroup.get('highlightPoints') as FormArray;
  }

  /** Directly referenced by the EN locale block in the template. */
  get enHighlightPointsArray(): FormArray {
    return this.enGroup.get('highlightPoints') as FormArray;
  }

  isFrIncomplete(): boolean {
    const g = this.frGroup;
    return !g?.get('name')?.value || !g?.get('serviceDescription')?.value;
  }

  /**
   * Shows the EN warning badge whenever the EN group contains invalid fields.
   */
  isEnIncomplete(): boolean {
    return this.enGroup?.invalid ?? false;
  }

  /**
   * FR + EN + non-translatable root fields must all be valid before submitting.
   */
  get canSubmit(): boolean {
    if (this.submitting || this.detailLoading) return false;
    const rootKeys = ['categoryId', 'priorityLevel', 'monthlyPrice', 'annualPrice', 'currency', 'freeTrialDays'];
    const rootOk = rootKeys.every(k => this.form.get(k)?.valid === true);
    return rootOk && this.frGroup.valid && this.enGroup.valid;
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
    this.submitting      = false;
    this.detailLoading   = false;
    this.allImageSlots   = [];
    this.deletedImageIds = [];
    this.dragSrcIndex    = null;
    this.openDropdown    = null;
    this.categorySearch  = '';
    this.activeLocale    = 'fr';
    this.buildForm();
  }

  private buildForm(): void {
    this.form = this.fb.group({
      categoryId:    ['', Validators.required],
      priorityLevel: [0, [Validators.required, Validators.min(0)]],
      monthlyPrice:  [null, [Validators.required, Validators.min(0)]],
      annualPrice:   [null, [Validators.required, Validators.min(0)]],
      currency:      ['EUR', [Validators.required, Validators.pattern(/^[A-Z]{3}$/)]],
      freeTrialDays: [0, [Validators.required, Validators.min(0)]],
      isPublished:   [false],
      isAvailable:   [true],

      fr: this.fb.group({
        name:                 ['', [Validators.required, Validators.maxLength(200)]],
        serviceDescription:   ['', Validators.required],
        technicalDescription: ['', Validators.required],
        highlightPoints:      this.fb.array([]),
      }),

      en: this.fb.group({
        name:                 ['', [Validators.required, Validators.maxLength(200)]],
        serviceDescription:   ['', Validators.required],
        technicalDescription: ['', Validators.required],
        highlightPoints:      this.fb.array([]),
      }),
    });
  }

  private fillForm(detail: AdminProductDetail): void {
    const frT = detail.translations['fr'];
    const enT = detail.translations['en'];
    this.form.patchValue({
      categoryId:    detail.categoryId,
      priorityLevel: detail.priorityLevel,
      monthlyPrice:  detail.monthlyPrice,
      annualPrice:   detail.annualPrice,
      currency:      detail.currency,
      freeTrialDays: detail.freeTrialDays,
      isPublished:   detail.isPublished,
      isAvailable:   detail.isAvailable,
      fr: {
        name:                 frT?.name ?? '',
        serviceDescription:   frT?.serviceDescription ?? '',
        technicalDescription: frT?.technicalDescription ?? '',
      },
      en: {
        name:                 enT?.name ?? '',
        serviceDescription:   enT?.serviceDescription ?? '',
        technicalDescription: enT?.technicalDescription ?? '',
      },
    });

    const frArr = this.frGroup.get('highlightPoints') as FormArray;
    frArr.clear();
    (frT?.highlightPoints ?? []).forEach(p => frArr.push(this.fb.control(p, Validators.required)));

    const enArr = this.enGroup.get('highlightPoints') as FormArray;
    enArr.clear();
    (enT?.highlightPoints ?? []).forEach(p => enArr.push(this.fb.control(p)));

    this.allImageSlots = detail.images.map(img => ({ kind: 'existing' as const, id: img.id, base64: img.base64 }));
  }

  private loadCategories(): void {
    this.categoryService.getCategories().subscribe({
      next:  (r) => (this.categories = r.data),
      error: ()  => {},
    });
  }

  // ── Locale tabs ───────────────────────────────────────────────────────────

  switchLocale(locale: 'fr' | 'en'): void {
    this.activeLocale = locale;
  }

  // ── Highlight points ──────────────────────────────────────────────────────

  addHighlightPoint(): void {
    this.highlightPointsArray.push(this.fb.control('', [Validators.required]));
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
      const key = Math.random().toString(36).slice(2);
      const reader = new FileReader();
      reader.onload = () => {
        this.allImageSlots = [
          ...this.allImageSlots,
          { kind: 'pending', key, file, preview: reader.result as string },
        ];
      };
      reader.readAsDataURL(file);
    });
    (event.target as HTMLInputElement).value = '';
  }

  removeSlot(index: number): void {
    const slot = this.allImageSlots[index];
    if (slot.kind === 'existing') {
      this.deletedImageIds.push(slot.id);
    }
    this.allImageSlots = this.allImageSlots.filter((_, i) => i !== index);
    if (this.dragSrcIndex === index) {
      this.dragSrcIndex = null;
    } else if (this.dragSrcIndex !== null && this.dragSrcIndex > index) {
      this.dragSrcIndex--;
    }
  }

  // ── Drag & drop ───────────────────────────────────────────────────────────

  onDragStart(index: number, event: DragEvent): void {
    this.dragSrcIndex = index;
    event.dataTransfer!.effectAllowed = 'move';
    event.dataTransfer!.setData('text/plain', String(index));
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.dataTransfer!.dropEffect = 'move';
  }

  onDrop(event: DragEvent, targetIndex: number): void {
    event.preventDefault();
    if (this.dragSrcIndex === null || this.dragSrcIndex === targetIndex) {
      this.dragSrcIndex = null;
      return;
    }
    const slots = [...this.allImageSlots];
    const [moved] = slots.splice(this.dragSrcIndex, 1);
    slots.splice(targetIndex, 0, moved);
    this.allImageSlots = slots;
    this.dragSrcIndex = null;
  }

  onDragEnd(): void {
    this.dragSrcIndex = null;
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
    const frArr = (this.frGroup.get('highlightPoints') as FormArray).value as string[];
    const enArr = (this.enGroup.get('highlightPoints') as FormArray).value as string[];
    this.saved.emit({
      translations: {
        fr: {
          name:                 v.fr.name,
          serviceDescription:   v.fr.serviceDescription,
          technicalDescription: v.fr.technicalDescription,
          highlightPoints:      frArr,
        },
        en: {
          name:                 v.en.name,
          serviceDescription:   v.en.serviceDescription,
          technicalDescription: v.en.technicalDescription,
          highlightPoints:      enArr,
        },
      },
      categoryId:    v.categoryId,
      priorityLevel: +v.priorityLevel,
      monthlyPrice:  +v.monthlyPrice,
      annualPrice:   +v.annualPrice,
      currency:      v.currency,
      freeTrialDays: +v.freeTrialDays,
      isPublished:   v.isPublished,
      isAvailable:   v.isAvailable,
      deletedImageIds: [...this.deletedImageIds],
      imageOrder:    this.allImageSlots.map(slot =>
        slot.kind === 'existing'
          ? { kind: 'existing' as const, id: slot.id }
          : { kind: 'pending' as const, file: slot.file }
      ),
    });
  }

  close(): void { this.closed.emit(); }

}
