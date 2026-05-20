import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { AdminProduct, ProductService } from '../../../../core/services/product.service';
import {
  AdminPromotion,
  CreatePromotionPayload,
  OfferCarouselSettings,
  PromotionService,
  UpdateOfferCarouselSettingsPayload,
  UpdatePromotionPayload
} from '../../../../core/services/promotion.service';

interface PromotionFormState {
  productId: string;
  discountPercent: number;
  marketingTextFr: string;
  marketingTextEn: string;
  startAtLocal: string;
  endAtLocal: string;
  enabled: boolean;
  showInCarousel: boolean;
  carouselOrder: number | null;
}

@Component({
  selector: 'app-promotion-list',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './promotion-list.component.html',
  styleUrl: './promotion-list.component.scss',
})
export class PromotionListComponent {
  private readonly promotionService = inject(PromotionService);
  private readonly productService   = inject(ProductService);

  protected readonly loading               = signal(false);
  protected readonly saving                = signal(false);
  protected readonly deletingId            = signal<string | null>(null);
  protected readonly promotions            = signal<AdminPromotion[]>([]);
  protected readonly products              = signal<AdminProduct[]>([]);
  protected readonly modalOpen             = signal(false);
  protected readonly editingPromotion      = signal<AdminPromotion | null>(null);
  protected readonly form                  = signal<PromotionFormState>(this.defaultForm());
  protected readonly formError             = signal<string | null>(null);
  protected readonly carouselSettings      = signal<OfferCarouselSettings>({ fixedTextFr: '', fixedTextEn: '' });
  protected readonly carouselSettingsDraft = signal<OfferCarouselSettings>({ fixedTextFr: '', fixedTextEn: '' });
  protected readonly carouselSettingsSaving = signal(false);
  protected readonly carouselSettingsError  = signal<string | null>(null);
  protected readonly toast = signal<{ message: string; type: 'success' | 'error' } | null>(null);

  // ── Locale tab state ──────────────────────────────────────────────────────
  protected readonly activeLocaleModal    = signal<'fr' | 'en'>('fr');
  protected readonly activeLocaleSettings = signal<'fr' | 'en'>('fr');

  // ── Incomplete badge indicators ───────────────────────────────────────────
  protected readonly isModalFrIncomplete    = computed(() => !this.form().marketingTextFr.trim());
  protected readonly isModalEnIncomplete    = computed(() => !this.form().marketingTextEn.trim());
  protected readonly isSettingsFrIncomplete = computed(() => !this.carouselSettingsDraft().fixedTextFr.trim());
  protected readonly isSettingsEnIncomplete = computed(() => !this.carouselSettingsDraft().fixedTextEn.trim());

  // ── Submit guards (both locales required) ─────────────────────────────────
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
    return !!d.fixedTextFr.trim() && !!d.fixedTextEn.trim();
  });

  private toastTimer: ReturnType<typeof setTimeout> | null = null;

  protected readonly displayedPromotions = computed(() =>
    [...this.promotions()].sort((first, second) => {
      if (first.showInCarousel !== second.showInCarousel) {
        return first.showInCarousel ? -1 : 1;
      }
      if (first.showInCarousel && second.showInCarousel) {
        const firstOrder  = first.carouselOrder  ?? Number.MAX_SAFE_INTEGER;
        const secondOrder = second.carouselOrder ?? Number.MAX_SAFE_INTEGER;
        if (firstOrder !== secondOrder) return firstOrder - secondOrder;
      }
      if (first.activeNow !== second.activeNow) {
        return first.activeNow ? -1 : 1;
      }
      return new Date(second.startAt).getTime() - new Date(first.startAt).getTime();
    })
  );

  constructor() {
    this.loadData();
  }

  protected loadData(): void {
    this.loading.set(true);
    forkJoin({
      promotions:       this.promotionService.getPromotions(),
      products:         this.productService.getProducts(0, 500),
      carouselSettings: this.promotionService.getCarouselSettings()
    }).subscribe({
      next: ({ promotions, products, carouselSettings }) => {
        this.promotions.set(promotions.data ?? []);
        this.products.set(
          [...(products.data.items ?? [])].sort((first, second) =>
            first.name.localeCompare(second.name, 'fr')
          )
        );
        const settings = carouselSettings.data ?? { fixedTextFr: '', fixedTextEn: '' };
        this.carouselSettings.set(settings);
        this.carouselSettingsDraft.set({ ...settings });
        this.carouselSettingsError.set(null);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.showToast('Erreur lors du chargement des promotions.', 'error');
      }
    });
  }

  protected openCreateModal(): void {
    const firstProductId = this.products()[0]?.id ?? '';
    this.editingPromotion.set(null);
    this.form.set({ ...this.defaultForm(), productId: firstProductId });
    this.formError.set(null);
    this.activeLocaleModal.set('fr');
    this.modalOpen.set(true);
  }

  protected openEditModal(promotion: AdminPromotion): void {
    const normalizedOrder = promotion.showInCarousel
      ? this.normalizeCarouselOrder(promotion.carouselOrder, promotion.id)
      : null;
    this.editingPromotion.set(promotion);
    this.form.set({
      productId:       promotion.productId,
      discountPercent: promotion.discountPercent,
      marketingTextFr: promotion.marketingTextFr,
      marketingTextEn: promotion.marketingTextEn,
      startAtLocal:    this.toLocalInputValue(promotion.startAt),
      endAtLocal:      this.toLocalInputValue(promotion.endAt),
      enabled:         promotion.enabled,
      showInCarousel:  promotion.showInCarousel,
      carouselOrder:   normalizedOrder
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

  protected selectedProductName(): string {
    const productId = this.form().productId;
    return this.products().find(product => product.id === productId)?.name ?? 'Produit inconnu';
  }

  protected updateForm<K extends keyof PromotionFormState>(field: K, value: PromotionFormState[K]): void {
    this.form.update(current => ({ ...current, [field]: value }));
  }

  protected toggleShowInCarousel(checked: boolean): void {
    const editingPromotionId = this.editingPromotion()?.id ?? null;
    this.form.update(current => ({
      ...current,
      showInCarousel: checked,
      carouselOrder: checked
        ? this.normalizeCarouselOrder(current.carouselOrder, editingPromotionId)
        : null
    }));
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

    if (editing) {
      const payload: UpdatePromotionPayload = {
        discountPercent: Number(data.discountPercent),
        marketingTextFr: data.marketingTextFr.trim(),
        marketingTextEn: data.marketingTextEn.trim(),
        startAt,
        endAt,
        enabled:        data.enabled,
        showInCarousel: data.showInCarousel,
        carouselOrder:  data.showInCarousel ? data.carouselOrder : null
      };
      this.promotionService.updatePromotion(editing.id, payload).subscribe({
        next:  () => this.onSaveSuccess('Promotion modifiee avec succes.'),
        error: error => this.onSaveError(error)
      });
      return;
    }

    const payload: CreatePromotionPayload = {
      productId:       data.productId,
      discountPercent: Number(data.discountPercent),
      marketingTextFr: data.marketingTextFr.trim(),
      marketingTextEn: data.marketingTextEn.trim(),
      startAt,
      endAt,
      enabled:        data.enabled,
      showInCarousel: data.showInCarousel,
      carouselOrder:  data.showInCarousel ? data.carouselOrder : null
    };
    this.promotionService.createPromotion(payload).subscribe({
      next:  () => this.onSaveSuccess('Promotion creee avec succes.'),
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
        this.showToast('Promotion supprimee.', 'success');
        this.loadData();
      },
      error: () => {
        this.deletingId.set(null);
        this.showToast('Erreur lors de la suppression.', 'error');
      }
    });
  }

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

  protected updateCarouselSettingsDraft<K extends keyof OfferCarouselSettings>(
    field: K,
    value: OfferCarouselSettings[K]
  ): void {
    this.carouselSettingsDraft.update(current => ({ ...current, [field]: value }));
  }

  protected saveCarouselSettings(): void {
    const draft = this.carouselSettingsDraft();
    this.carouselSettingsSaving.set(true);
    this.carouselSettingsError.set(null);

    const payload: UpdateOfferCarouselSettingsPayload = {
      fixedTextFr: draft.fixedTextFr.trim(),
      fixedTextEn: draft.fixedTextEn.trim()
    };

    this.promotionService.updateCarouselSettings(payload).subscribe({
      next: () => {
        this.carouselSettingsSaving.set(false);
        this.carouselSettings.set(payload);
        this.carouselSettingsDraft.set({ ...payload });
        this.showToast('Texte fixe du carrousel enregistre.', 'success');
      },
      error: () => {
        this.carouselSettingsSaving.set(false);
        this.carouselSettingsError.set('Erreur lors de l enregistrement du texte fixe du carrousel.');
      }
    });
  }

  protected isDeleteLoading(promotionId: string): boolean {
    return this.deletingId() === promotionId;
  }

  protected maxCarouselOrderForForm(): number {
    return this.maxAllowedCarouselOrder(this.editingPromotion()?.id ?? null);
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
    if (!data.productId) return 'Selectionnez un produit.';
    if (!Number.isFinite(data.discountPercent) || data.discountPercent < 1 || data.discountPercent > 100)
      return 'La reduction doit etre comprise entre 1 et 100.';
    if (!data.marketingTextFr.trim() || !data.marketingTextEn.trim())
      return 'Les textes marketing FR/EN sont obligatoires.';
    if (!data.startAtLocal || !data.endAtLocal)
      return 'Renseignez la periode de promotion.';
    const start = new Date(data.startAtLocal).getTime();
    const end   = new Date(data.endAtLocal).getTime();
    if (!Number.isFinite(start) || !Number.isFinite(end) || start >= end)
      return 'La date de fin doit etre apres la date de debut.';
    if (data.showInCarousel && (!Number.isFinite(data.carouselOrder) || (data.carouselOrder ?? 0) < 1))
      return 'L ordre du carrousel doit etre superieur ou egal a 1.';
    const maxAllowedCarouselOrder = this.maxAllowedCarouselOrder(editingPromotionId);
    if (data.showInCarousel && (data.carouselOrder ?? 0) > maxAllowedCarouselOrder)
      return `L ordre du carrousel doit etre compris entre 1 et ${maxAllowedCarouselOrder}.`;
    if (data.showInCarousel && this.isCarouselOrderUsed(data.carouselOrder ?? 0, editingPromotionId))
      return `L ordre ${data.carouselOrder} est deja utilise dans le carrousel.`;
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
    const now        = new Date();
    const inSevenDays = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000);
    return {
      productId:       '',
      discountPercent: 10,
      marketingTextFr: '',
      marketingTextEn: '',
      startAtLocal:    this.toLocalInputValue(now.toISOString()),
      endAtLocal:      this.toLocalInputValue(inSevenDays.toISOString()),
      enabled:         true,
      showInCarousel:  false,
      carouselOrder:   null
    };
  }

  private normalizeCarouselOrder(order: number | null, editingPromotionId: string | null): number {
    const maxAllowedOrder = this.maxAllowedCarouselOrder(editingPromotionId);
    if (
      Number.isFinite(order) &&
      (order ?? 0) >= 1 &&
      (order ?? 0) <= maxAllowedOrder &&
      !this.isCarouselOrderUsed(order ?? 0, editingPromotionId)
    ) {
      return order as number;
    }
    return this.nextAvailableCarouselOrder(editingPromotionId);
  }

  private nextAvailableCarouselOrder(editingPromotionId: string | null): number {
    const usedOrders = new Set(
      this.promotions()
        .filter(p => p.showInCarousel && p.id !== editingPromotionId)
        .map(p => p.carouselOrder)
        .filter((o): o is number => typeof o === 'number' && o >= 1)
    );
    let candidate = 1;
    while (usedOrders.has(candidate)) candidate++;
    return candidate;
  }

  private isCarouselOrderUsed(order: number, editingPromotionId: string | null): boolean {
    if (!Number.isFinite(order) || order < 1) return false;
    return this.promotions().some(
      p => p.showInCarousel && p.id !== editingPromotionId && p.carouselOrder === order
    );
  }

  private maxAllowedCarouselOrder(editingPromotionId: string | null): number {
    return this.promotions().filter(p => p.showInCarousel && p.id !== editingPromotionId).length + 1;
  }

  private extractErrorMessage(error: unknown): string {
    const payload = error as {
      error?: { error?: { message?: string }; message?: string };
      message?: string;
    };
    return payload?.error?.error?.message
      || payload?.error?.message
      || payload?.message
      || 'Erreur lors de l enregistrement de la promotion.';
  }

  private showToast(message: string, type: 'success' | 'error'): void {
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toast.set({ message, type });
    this.toastTimer = setTimeout(() => this.toast.set(null), 3000);
  }
}
