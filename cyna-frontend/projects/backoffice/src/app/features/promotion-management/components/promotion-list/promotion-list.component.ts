import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { AdminProduct, ProductService } from '../../../../core/services/product.service';
import {
  AdminPromotion,
  CreatePromotionPayload,
  PromotionService,
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
  private readonly productService = inject(ProductService);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly deletingId = signal<string | null>(null);
  protected readonly promotions = signal<AdminPromotion[]>([]);
  protected readonly products = signal<AdminProduct[]>([]);
  protected readonly modalOpen = signal(false);
  protected readonly editingPromotion = signal<AdminPromotion | null>(null);
  protected readonly form = signal<PromotionFormState>(this.defaultForm());
  protected readonly formError = signal<string | null>(null);
  protected readonly toast = signal<{ message: string; type: 'success' | 'error' } | null>(null);

  private toastTimer: ReturnType<typeof setTimeout> | null = null;

  protected readonly displayedPromotions = computed(() =>
    [...this.promotions()].sort((first, second) => {
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
      promotions: this.promotionService.getPromotions(),
      products: this.productService.getProducts(0, 500)
    }).subscribe({
      next: ({ promotions, products }) => {
        this.promotions.set(promotions.data ?? []);
        this.products.set(
          [...(products.data.items ?? [])].sort((first, second) =>
            first.name.localeCompare(second.name, 'fr')
          )
        );
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
    this.form.set({
      ...this.defaultForm(),
      productId: firstProductId
    });
    this.formError.set(null);
    this.modalOpen.set(true);
  }

  protected openEditModal(promotion: AdminPromotion): void {
    this.editingPromotion.set(promotion);
    this.form.set({
      productId: promotion.productId,
      discountPercent: promotion.discountPercent,
      marketingTextFr: promotion.marketingTextFr,
      marketingTextEn: promotion.marketingTextEn,
      startAtLocal: this.toLocalInputValue(promotion.startAt),
      endAtLocal: this.toLocalInputValue(promotion.endAt),
      enabled: promotion.enabled
    });
    this.formError.set(null);
    this.modalOpen.set(true);
  }

  protected closeModal(): void {
    this.modalOpen.set(false);
    this.editingPromotion.set(null);
    this.formError.set(null);
  }

  protected selectedProductName(): string {
    const productId = this.form().productId;
    return this.products().find(product => product.id === productId)?.name ?? 'Produit inconnu';
  }

  protected updateForm<K extends keyof PromotionFormState>(field: K, value: PromotionFormState[K]): void {
    this.form.update(current => ({ ...current, [field]: value }));
  }

  protected savePromotion(): void {
    const data = this.form();
    const validationError = this.validateForm(data);
    if (validationError) {
      this.formError.set(validationError);
      return;
    }

    this.saving.set(true);
    this.formError.set(null);

    const startAt = this.toIsoUtc(data.startAtLocal);
    const endAt = this.toIsoUtc(data.endAtLocal);
    if (!startAt || !endAt) {
      this.saving.set(false);
      this.formError.set('Les dates de debut/fin sont invalides.');
      return;
    }

    const editing = this.editingPromotion();
    if (editing) {
      const payload: UpdatePromotionPayload = {
        discountPercent: Number(data.discountPercent),
        marketingTextFr: data.marketingTextFr.trim(),
        marketingTextEn: data.marketingTextEn.trim(),
        startAt,
        endAt,
        enabled: data.enabled
      };
      this.promotionService.updatePromotion(editing.id, payload).subscribe({
        next: () => this.onSaveSuccess('Promotion modifiee avec succes.'),
        error: error => this.onSaveError(error)
      });
      return;
    }

    const payload: CreatePromotionPayload = {
      productId: data.productId,
      discountPercent: Number(data.discountPercent),
      marketingTextFr: data.marketingTextFr.trim(),
      marketingTextEn: data.marketingTextEn.trim(),
      startAt,
      endAt,
      enabled: data.enabled
    };
    this.promotionService.createPromotion(payload).subscribe({
      next: () => this.onSaveSuccess('Promotion creee avec succes.'),
      error: error => this.onSaveError(error)
    });
  }

  protected deletePromotion(promotion: AdminPromotion): void {
    const confirmed = window.confirm(
      `Supprimer la promotion de "${promotion.productName}" (${promotion.discountPercent}% ) ?`
    );
    if (!confirmed) {
      return;
    }

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
    if (!dateStr) {
      return '-';
    }
    return new Date(dateStr).toLocaleString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  protected formatPrice(amount: number, currency: string): string {
    return new Intl.NumberFormat('fr-FR', { style: 'currency', currency }).format(amount);
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

  private validateForm(data: PromotionFormState): string | null {
    if (!data.productId) {
      return 'Selectionnez un produit.';
    }
    if (!Number.isFinite(data.discountPercent) || data.discountPercent < 1 || data.discountPercent > 100) {
      return 'La reduction doit etre comprise entre 1 et 100.';
    }
    if (!data.marketingTextFr.trim() || !data.marketingTextEn.trim()) {
      return 'Les textes marketing FR/EN sont obligatoires.';
    }
    if (!data.startAtLocal || !data.endAtLocal) {
      return 'Renseignez la periode de promotion.';
    }
    const start = new Date(data.startAtLocal).getTime();
    const end = new Date(data.endAtLocal).getTime();
    if (!Number.isFinite(start) || !Number.isFinite(end) || start >= end) {
      return 'La date de fin doit etre apres la date de debut.';
    }
    return null;
  }

  private toIsoUtc(localDateTime: string): string | null {
    const parsed = new Date(localDateTime);
    if (!Number.isFinite(parsed.getTime())) {
      return null;
    }
    return parsed.toISOString();
  }

  private toLocalInputValue(isoDateTime: string): string {
    const date = new Date(isoDateTime);
    if (!Number.isFinite(date.getTime())) {
      return '';
    }
    const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
    return local.toISOString().slice(0, 16);
  }

  private defaultForm(): PromotionFormState {
    const now = new Date();
    const inSevenDays = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000);
    return {
      productId: '',
      discountPercent: 10,
      marketingTextFr: '',
      marketingTextEn: '',
      startAtLocal: this.toLocalInputValue(now.toISOString()),
      endAtLocal: this.toLocalInputValue(inSevenDays.toISOString()),
      enabled: true
    };
  }

  private extractErrorMessage(error: unknown): string {
    const payload = error as {
      error?: {
        error?: { message?: string };
        message?: string;
      };
      message?: string;
    };

    return payload?.error?.error?.message
      || payload?.error?.message
      || payload?.message
      || 'Erreur lors de l enregistrement de la promotion.';
  }

  private showToast(message: string, type: 'success' | 'error'): void {
    if (this.toastTimer) {
      clearTimeout(this.toastTimer);
    }
    this.toast.set({ message, type });
    this.toastTimer = setTimeout(() => this.toast.set(null), 3000);
  }
}
