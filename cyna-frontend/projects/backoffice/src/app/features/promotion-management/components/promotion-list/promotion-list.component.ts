import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, debounceTime, EMPTY, forkJoin, of, Subject, switchMap } from 'rxjs';
import { AdminCategory, CategoryService } from '../../../../core/services/category.service';
import { AdminProduct, ProductService } from '../../../../core/services/product.service';
import {
  AdminPromotion,
  CarouselSettingsTranslation,
  CreatePromotionPayload,
  OfferCarouselSettings,
  PromotionService,
  PromotionTranslation,
  UpdateOfferCarouselSettingsPayload,
  UpdatePromotionPayload,
} from '../../../../core/services/promotion.service';

interface PromotionFormState {
  productId: string;
  discountPercent: number;
  marketingTextFr: string;
  marketingTextEn: string;
  startAtLocal: string;
  endAtLocal: string;
  enabled: boolean;
}

interface CarouselSettingsDraft {
  fixedTextFr: string;
  fixedTextEn: string;
  maxSlides: number;
}

@Component({
  selector: 'app-promotion-list',
  standalone: true,
  imports: [FormsModule, TranslatePipe],
  templateUrl: './promotion-list.component.html',
  styleUrl: './promotion-list.component.scss',
})
export class PromotionListComponent {
  private readonly promotionService = inject(PromotionService);
  private readonly productService   = inject(ProductService);
  private readonly categoryService  = inject(CategoryService);
  private readonly destroyRef       = inject(DestroyRef);

  // Page state
  protected readonly loading    = signal(false);
  protected readonly saving     = signal(false);
  protected readonly deletingId = signal<string | null>(null);
  protected readonly promotions = signal<AdminPromotion[]>([]);
  protected readonly categories = signal<AdminCategory[]>([]);

  // Modal state
  protected readonly modalOpen        = signal(false);
  protected readonly editingPromotion = signal<AdminPromotion | null>(null);
  protected readonly form             = signal<PromotionFormState>(this.defaultForm());
  protected readonly formError        = signal<string | null>(null);

  // Product picker (create mode only)
  protected readonly selectedProduct     = signal<AdminProduct | null>(null);
  protected readonly searchCategoryId    = signal<string>('');
  protected readonly searchInputText     = signal<string>('');
  protected readonly searchResults       = signal<AdminProduct[]>([]);
  protected readonly searchLoading       = signal<boolean>(false);
  protected readonly searchDropdownOpen  = signal<boolean>(false);
  protected readonly categoryDropdownOpen = signal<boolean>(false);

  protected readonly selectedCategory = computed<AdminCategory | null>(() => {
    const id = this.searchCategoryId();
    if (!id) return null;
    return this.categories().find(c => c.id === id) ?? null;
  });

  private readonly searchSubject = new Subject<{ query: string; categoryId: string }>();

  // Carousel settings
  protected readonly carouselSettings      = signal<OfferCarouselSettings>({ translations: {}, maxSlides: 5 });
  protected readonly carouselSettingsDraft = signal<CarouselSettingsDraft>({ fixedTextFr: '', fixedTextEn: '', maxSlides: 5 });
  protected readonly carouselSettingsSaving = signal(false);
  protected readonly carouselSettingsError  = signal<string | null>(null);
  protected readonly carouselActionLoading  = signal(false);
  protected readonly toast = signal<{ message: string; type: 'success' | 'error' } | null>(null);

  // Locale tab state
  protected readonly activeLocaleModal    = signal<'fr' | 'en'>('fr');
  protected readonly activeLocaleSettings = signal<'fr' | 'en'>('fr');

  // Incomplete badge indicators
  protected readonly isModalFrIncomplete    = computed(() => !this.form().marketingTextFr.trim());
  protected readonly isModalEnIncomplete    = computed(() => !this.form().marketingTextEn.trim());
  protected readonly isSettingsFrIncomplete = computed(() => !(this.carouselSettingsDraft().fixedTextFr ?? '').trim());
  protected readonly isSettingsEnIncomplete = computed(() => !(this.carouselSettingsDraft().fixedTextEn ?? '').trim());

  // Submit guards
  protected readonly canSavePromotion = computed(() => {
    if (this.saving()) return false;
    const f = this.form();
    return (
      !!f.productId &&
      Number.isFinite(f.discountPercent) &&
      f.discountPercent >= 1 &&
      f.discountPercent <= 100 &&
      !!f.marketingTextFr.trim() &&
      !!f.marketingTextEn.trim() &&
      !!f.startAtLocal &&
      !!f.endAtLocal
    );
  });

  protected readonly canSaveSettings = computed(() => {
    if (this.carouselSettingsSaving()) return false;
    const d = this.carouselSettingsDraft();
    return !!(d.fixedTextFr ?? '').trim() && !!(d.fixedTextEn ?? '').trim();
  });

  private toastTimer: ReturnType<typeof setTimeout> | null = null;

  // Carousel slides sorted by order
  protected readonly carouselSlides = computed(() =>
    [...this.promotions()]
      .filter(p => p.showInCarousel)
      .sort((a, b) => (a.carouselOrder ?? 99) - (b.carouselOrder ?? 99))
  );

  protected readonly carouselMaxSlides = computed(() => this.carouselSettings().maxSlides);

  protected readonly carouselIsFull = computed(() =>
    this.carouselSlides().length >= this.carouselMaxSlides()
  );

  /** Promos éligibles à l'ajout : pas déjà dans le carrousel */
  protected readonly promotionsNotInCarousel = computed(() =>
    this.promotions().filter(p => !p.showInCarousel)
  );

  /** Contrôle l'ouverture du dropdown d'ajout rapide */
  protected readonly carouselAddOpen  = signal(false);
  protected readonly carouselAddQuery = signal('');

  protected readonly carouselAddFiltered = computed(() => {
    const q = this.carouselAddQuery().trim().toLowerCase();
    const list = this.promotionsNotInCarousel();
    if (!q) return list;
    return list.filter(p =>
      p.productName.toLowerCase().includes(q) ||
      p.productCategoryName.toLowerCase().includes(q)
    );
  });

  protected toggleCarouselAdd(): void {
    const next = !this.carouselAddOpen();
    this.carouselAddOpen.set(next);
    if (!next) this.carouselAddQuery.set('');
  }

  // ── Tri de la table ───────────────────────────────────────────────────────
  protected readonly sortColumn    = signal<'productName' | 'discountPercent' | 'startAt' | 'activeNow' | null>(null);
  protected readonly sortDirection = signal<'asc' | 'desc'>('asc');

  protected toggleSort(col: 'productName' | 'discountPercent' | 'startAt' | 'activeNow'): void {
    if (this.sortColumn() === col) {
      if (this.sortDirection() === 'asc') {
        this.sortDirection.set('desc');
      } else {
        this.sortColumn.set(null);
        this.sortDirection.set('asc');
      }
    } else {
      this.sortColumn.set(col);
      this.sortDirection.set('asc');
    }
  }

  protected isSortActive(col: string): boolean {
    return this.sortColumn() === col;
  }

  protected sortIndicator(col: string): string {
    if (this.sortColumn() !== col) return '⇅';
    return this.sortDirection() === 'asc' ? '↑' : '↓';
  }

  protected readonly displayedPromotions = computed(() => {
    const col = this.sortColumn();
    const dir = this.sortDirection();

    return [...this.promotions()].sort((a, b) => {
      if (!col) {
        // Tri par défaut : actives d'abord, puis par date de début desc
        // Le groupe "dans le carrousel" n'est PAS trié par carouselOrder ici
        // pour éviter que le réordonnancement du carrousel bouge la liste.
        if (a.activeNow !== b.activeNow) return a.activeNow ? -1 : 1;
        return new Date(b.startAt).getTime() - new Date(a.startAt).getTime();
      }

      let valA: string | number;
      let valB: string | number;
      switch (col) {
        case 'productName':
          valA = a.productName.toLowerCase();
          valB = b.productName.toLowerCase();
          break;
        case 'discountPercent':
          valA = a.discountPercent;
          valB = b.discountPercent;
          break;
        case 'startAt':
          valA = new Date(a.startAt).getTime();
          valB = new Date(b.startAt).getTime();
          break;
        case 'activeNow':
          valA = a.activeNow ? 1 : 0;
          valB = b.activeNow ? 1 : 0;
          break;
        default:
          return 0;
      }
      if (valA < valB) return dir === 'asc' ? -1 : 1;
      if (valA > valB) return dir === 'asc' ? 1 : -1;
      return 0;
    });
  });

  constructor() {
    this.loadData();
    this.setupProductSearch();
  }

  private setupProductSearch(): void {
    this.searchSubject.pipe(
      debounceTime(250),
      switchMap(({ query, categoryId }) => {
        const trimmed = query.trim();
        if (!trimmed) {
          this.searchResults.set([]);
          this.searchDropdownOpen.set(false);
          this.searchLoading.set(false);
          return EMPTY;
        }
        this.searchLoading.set(true);
        return this.productService.getProducts(0, 10, {
          search: trimmed,
          categoryId: categoryId || undefined,
        }).pipe(
          catchError(() => of({
            success: true,
            data: { items: [] as AdminProduct[], pageNumber: 0, pageSize: 10, totalElements: 0 },
            timestamp: ''
          }))
        );
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(response => {
      this.searchResults.set(response.data?.items ?? []);
      this.searchLoading.set(false);
      this.searchDropdownOpen.set(true);
    });
  }

  protected loadData(): void {
    this.loading.set(true);
    forkJoin({
      promotions:       this.promotionService.getPromotions(),
      categories:       this.categoryService.getCategories(),
      carouselSettings: this.promotionService.getCarouselSettings()
    }).subscribe({
      next: ({ promotions, categories, carouselSettings }) => {
        this.promotions.set(promotions.data ?? []);
        this.categories.set(
          [...(categories.data ?? [])].sort((a, b) => a.name.localeCompare(b.name, 'fr'))
        );
        const settings = carouselSettings.data ?? { translations: {}, maxSlides: 5 };
        this.carouselSettings.set(settings);
        this.carouselSettingsDraft.set({
          fixedTextFr: settings.translations?.['fr']?.fixedText ?? '',
          fixedTextEn: settings.translations?.['en']?.fixedText ?? '',
          maxSlides:   settings.maxSlides ?? 5,
        });
        this.carouselSettingsError.set(null);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.showToast('Erreur lors du chargement des promotions.', 'error');  // pas d'accent intentionnel (message technique)
      }
    });
  }

  protected openCreateModal(): void {
    this.editingPromotion.set(null);
    this.selectedProduct.set(null);
    this.searchCategoryId.set('');
    this.searchInputText.set('');
    this.searchResults.set([]);
    this.searchDropdownOpen.set(false);
    this.categoryDropdownOpen.set(false);
    this.form.set(this.defaultForm());
    this.formError.set(null);
    this.activeLocaleModal.set('fr');
    this.modalOpen.set(true);
  }

  protected openEditModal(promotion: AdminPromotion): void {
    this.editingPromotion.set(promotion);
    this.form.set({
      productId:       promotion.productId,
      discountPercent: promotion.discountPercent,
      marketingTextFr: promotion.translations?.['fr']?.marketingText ?? '',
      marketingTextEn: promotion.translations?.['en']?.marketingText ?? '',
      startAtLocal:    this.toLocalInputValue(promotion.startAt),
      endAtLocal:      this.toLocalInputValue(promotion.endAt),
      enabled:         promotion.enabled,
    });
    this.formError.set(null);
    this.activeLocaleModal.set('fr');
    this.modalOpen.set(true);
  }

  protected closeModal(): void {
    this.modalOpen.set(false);
    this.editingPromotion.set(null);
    this.formError.set(null);
    this.activeLocaleModal.set('fr');
  }

  // Category dropdown handlers
  protected toggleCategoryDropdown(): void {
    this.categoryDropdownOpen.update(v => !v);
  }

  protected closeCategoryDropdown(): void {
    this.categoryDropdownOpen.set(false);
  }

  protected selectSearchCategory(cat: AdminCategory | null): void {
    const categoryId = cat?.id ?? '';
    this.categoryDropdownOpen.set(false);
    this.onSearchCategoryChange(categoryId);
  }

  // Product picker handlers
  protected onProductSearchInput(value: string): void {
    this.searchInputText.set(value);
    this.searchSubject.next({ query: value, categoryId: this.searchCategoryId() });
  }

  protected onSearchCategoryChange(categoryId: string): void {
    this.searchCategoryId.set(categoryId);
    const query = this.searchInputText().trim();
    if (query) {
      this.searchSubject.next({ query, categoryId });
    }
  }

  protected selectProduct(product: AdminProduct): void {
    this.selectedProduct.set(product);
    this.form.update(f => ({ ...f, productId: product.id }));
    this.searchDropdownOpen.set(false);
    this.searchInputText.set('');
    this.searchResults.set([]);
  }

  protected clearSelectedProduct(): void {
    this.selectedProduct.set(null);
    this.form.update(f => ({ ...f, productId: '' }));
  }

  protected onSearchBlur(): void {
    setTimeout(() => this.searchDropdownOpen.set(false), 150);
  }

  protected updateForm<K extends keyof PromotionFormState>(field: K, value: PromotionFormState[K]): void {
    this.form.update(current => ({ ...current, [field]: value }));
  }

  protected savePromotion(): void {
    const data    = this.form();
    const editing = this.editingPromotion();
    const validationError = this.validateForm(data, editing?.id ?? null);
    if (validationError) {
      this.formError.set(validationError);
      return;
    }

    this.saving.set(true);
    this.formError.set(null);

    const startAt = this.toIsoUtc(data.startAtLocal);
    const endAt   = this.toIsoUtc(data.endAtLocal);
    if (!startAt || !endAt) {
      this.saving.set(false);
      this.formError.set('Les dates de debut/fin sont invalides.');
      return;
    }

    const translations: Record<string, PromotionTranslation> = {
      fr: { marketingText: data.marketingTextFr.trim() },
      en: { marketingText: data.marketingTextEn.trim() },
    };

    if (editing) {
      // Preserve carousel state — managed separately
      const payload: UpdatePromotionPayload = {
        discountPercent: Number(data.discountPercent),
        translations,
        startAt,
        endAt,
        enabled:        data.enabled,
        showInCarousel: editing.showInCarousel,
        carouselOrder:  editing.carouselOrder,
      };
      this.promotionService.updatePromotion(editing.id, payload).subscribe({
        next:  () => this.onSaveSuccess('Promotion modifiée avec succès.'),
        error: error => this.onSaveError(error)
      });
      return;
    }

    // New promotions never enter carousel directly
    const payload: CreatePromotionPayload = {
      productId:       data.productId,
      discountPercent: Number(data.discountPercent),
      translations,
      startAt,
      endAt,
      enabled:        data.enabled,
      showInCarousel: false,
      carouselOrder:  null,
    };
    this.promotionService.createPromotion(payload).subscribe({
      next:  () => this.onSaveSuccess('Promotion créée avec succès.'),
      error: error => this.onSaveError(error)
    });
  }

  protected deletePromotion(promotion: AdminPromotion): void {
    const confirmed = window.confirm(
      `Supprimer la promotion de "${promotion.productName}" (${promotion.discountPercent}% ) ?`
    );
    if (!confirmed) return;

    this.deletingId.set(promotion.id);
    this.promotionService.deletePromotion(promotion.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.showToast('Promotion supprimée.', 'success');
        this.loadData();
      },
      error: () => {
        this.deletingId.set(null);
        this.showToast('Erreur lors de la suppression.', 'error');
      }
    });
  }

  // ── Carousel management ────────────────────────────────────────────────────

  protected addToCarousel(promotion: AdminPromotion): void {
    if (this.carouselIsFull()) return;
    const nextOrder = this.carouselSlides().length + 1;
    const payload: UpdatePromotionPayload = {
      discountPercent: promotion.discountPercent,
      translations:    promotion.translations,
      startAt:         promotion.startAt,
      endAt:           promotion.endAt,
      enabled:         promotion.enabled,
      showInCarousel:  true,
      carouselOrder:   nextOrder,
    };
    this.carouselActionLoading.set(true);
    this.promotionService.updatePromotion(promotion.id, payload).subscribe({
      next: () => {
        this.carouselActionLoading.set(false);
        this.showToast('Ajoutée au carrousel.', 'success');
        this.loadData();
      },
      error: (err) => {
        this.carouselActionLoading.set(false);
        this.showToast(this.extractErrorMessage(err), 'error');
      }
    });
  }

  protected removeFromCarousel(promotion: AdminPromotion): void {
    const payload: UpdatePromotionPayload = {
      discountPercent: promotion.discountPercent,
      translations:    promotion.translations,
      startAt:         promotion.startAt,
      endAt:           promotion.endAt,
      enabled:         promotion.enabled,
      showInCarousel:  false,
      carouselOrder:   null,
    };
    this.carouselActionLoading.set(true);
    this.promotionService.updatePromotion(promotion.id, payload).subscribe({
      next: () => {
        this.carouselActionLoading.set(false);
        this.showToast('Retirée du carrousel.', 'success');
        this.loadData();
      },
      error: () => {
        this.carouselActionLoading.set(false);
        this.showToast('Erreur lors du retrait.', 'error');
      }
    });
  }

  protected moveCarouselSlide(promotionId: string, direction: 'up' | 'down'): void {
    const slides = [...this.carouselSlides()];
    const idx = slides.findIndex(s => s.id === promotionId);
    if (idx < 0) return;
    const targetIdx = direction === 'up' ? idx - 1 : idx + 1;
    if (targetIdx < 0 || targetIdx >= slides.length) return;

    [slides[idx], slides[targetIdx]] = [slides[targetIdx], slides[idx]];
    const orderedIds = slides.map(s => s.id);

    this.carouselActionLoading.set(true);
    this.promotionService.reorderCarousel(orderedIds).subscribe({
      next: () => {
        this.carouselActionLoading.set(false);
        this.loadData();
      },
      error: () => {
        this.carouselActionLoading.set(false);
        this.showToast('Erreur lors du réordonnancement.', 'error');
      }
    });
  }

  protected carouselSlideStatus(promotion: AdminPromotion): 'active' | 'scheduled' | 'masked' | 'disabled' {
    if (!promotion.productAvailable || !promotion.productPublished) return 'masked';
    if (promotion.activeNow) return 'active';
    if (promotion.enabled) return 'scheduled'; // future start date
    return 'disabled';
  }

  protected carouselSlideIsShown(promotion: AdminPromotion): boolean {
    return this.carouselSlideStatus(promotion) === 'active';
  }

  protected carouselSlideStatusReason(promotion: AdminPromotion): string {
    const status = this.carouselSlideStatus(promotion);
    if (status === 'masked') return 'Produit indisponible';
    if (status === 'scheduled') {
      const d = new Date(promotion.startAt).toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' });
      return `Dès le ${d}`;
    }
    if (status === 'disabled') return 'Promotion désactivée';
    return '';
  }

  protected carouselSlideStatusLabel(promotion: AdminPromotion): string {
    const status = this.carouselSlideStatus(promotion);
    if (status === 'active')    return 'Active';
    if (status === 'scheduled') {
      const d = new Date(promotion.startAt).toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' });
      return `Dès le ${d}`;
    }
    if (status === 'masked')    return 'Produit indisponible';
    return 'Désactivée';
  }


  // ── Carousel settings ──────────────────────────────────────────────────────

  protected formatDate(dateStr: string): string {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleString('fr-FR', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  protected formatPrice(amount: number, currency: string): string {
    return new Intl.NumberFormat('fr-FR', { style: 'currency', currency }).format(amount);
  }

  protected updateCarouselSettingsDraft<K extends keyof CarouselSettingsDraft>(
    field: K,
    value: CarouselSettingsDraft[K]
  ): void {
    this.carouselSettingsDraft.update(current => ({ ...current, [field]: value }));
  }

  protected saveCarouselSettings(): void {
    const draft = this.carouselSettingsDraft();
    this.carouselSettingsSaving.set(true);
    this.carouselSettingsError.set(null);

    const translations: Record<string, CarouselSettingsTranslation> = {
      fr: { fixedText: draft.fixedTextFr.trim() },
      en: { fixedText: draft.fixedTextEn.trim() },
    };
    const payload: UpdateOfferCarouselSettingsPayload = { translations, maxSlides: draft.maxSlides };

    this.promotionService.updateCarouselSettings(payload).subscribe({
      next: () => {
        this.carouselSettingsSaving.set(false);
        this.carouselSettings.set({ translations, maxSlides: draft.maxSlides });
        this.carouselSettingsDraft.set({
          fixedTextFr: draft.fixedTextFr.trim(),
          fixedTextEn: draft.fixedTextEn.trim(),
          maxSlides:   draft.maxSlides,
        });
        this.showToast('Paramètres du carrousel enregistrés.', 'success');
      },
      error: () => {
        this.carouselSettingsSaving.set(false);
        this.carouselSettingsError.set("Erreur lors de l'enregistrement des paramètres du carrousel.");
      }
    });
  }

  protected isDeleteLoading(promotionId: string): boolean {
    return this.deletingId() === promotionId;
  }

  private onSaveSuccess(message: string): void {
    this.saving.set(false);
    this.closeModal();
    this.showToast(message, 'success');
    this.loadData();
  }



  private onSaveError(error: unknown): void {
    this.saving.set(false);
    this.formError.set(this.extractErrorMessage(error));
  }

  private validateForm(data: PromotionFormState, editingPromotionId: string | null): string | null {
    if (!editingPromotionId && !data.productId) {
      return 'Sélectionnez un produit.';
    }
    if (!Number.isFinite(data.discountPercent) || data.discountPercent < 1 || data.discountPercent > 100) {
      return 'La réduction doit être comprise entre 1 et 100.';
    }
    if (!data.marketingTextFr.trim() || !data.marketingTextEn.trim())
      return 'Les textes marketing FR/EN sont obligatoires.';
    if (!data.startAtLocal || !data.endAtLocal)
      return 'Renseignez la période de promotion.';
    const start = new Date(data.startAtLocal).getTime();
    const end   = new Date(data.endAtLocal).getTime();
    if (!Number.isFinite(start) || !Number.isFinite(end) || start >= end)
      return 'La date de fin doit être après la date de début.';
    return null;
  }

  private toIsoUtc(localDateTime: string): string | null {
    const parsed = new Date(localDateTime);
    if (!Number.isFinite(parsed.getTime())) return null;
    return parsed.toISOString();
  }

  private toLocalInputValue(isoDateTime: string): string {
    const date = new Date(isoDateTime);
    if (!Number.isFinite(date.getTime())) return '';
    const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
    return local.toISOString().slice(0, 16);
  }

  private defaultForm(): PromotionFormState {
    const now         = new Date();
    const inSevenDays = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000);
    return {
      productId:       '',
      discountPercent: 10,
      marketingTextFr: '',
      marketingTextEn: '',
      startAtLocal:    this.toLocalInputValue(now.toISOString()),
      endAtLocal:      this.toLocalInputValue(inSevenDays.toISOString()),
      enabled:         true,
    };
  }

  private extractErrorMessage(error: unknown): string {
    const payload = error as {
      error?: { error?: { message?: string }; message?: string };
      message?: string;
    };
    return payload?.error?.error?.message
      || payload?.error?.message
      || payload?.message
      || "Erreur lors de l'enregistrement de la promotion.";
  }

  private showToast(message: string, type: 'success' | 'error'): void {
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toast.set({ message, type });
    this.toastTimer = setTimeout(() => this.toast.set(null), 3000);
  }
}
