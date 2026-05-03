import {
  Component,
  EventEmitter,
  Input,
  Output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminOrderDetail } from '../../../../core/services/order.service';

@Component({
  selector: 'app-order-detail-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './order-detail-modal.component.html',
  styleUrl: './order-detail-modal.component.scss',
})
export class OrderDetailModalComponent {
  @Input() open = false;
  @Input() order: AdminOrderDetail | null = null;
  @Input() loading = false;
  @Output() closed = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<string>();

  get canCancel(): boolean {
    return this.order?.status === 'PENDING' || this.order?.status === 'CONFIRMED';
  }

  get shortId(): string {
    if (!this.order) return '';
    return this.order.id.substring(0, 8).toUpperCase();
  }

  get statusLabel(): string {
    return this.getStatusLabel(this.order?.status ?? '');
  }

  get statusStyle(): { color: string; background: string } {
    return this.getStatusStyle(this.order?.status ?? '');
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'PENDING':    return 'En attente';
      case 'CONFIRMED':  return 'Confirmée';
      case 'PAID':       return 'Payée';
      case 'FULFILLED':  return 'Livrée';
      case 'CANCELLED':  return 'Annulée';
      default:           return status;
    }
  }

  getStatusStyle(status: string): { color: string; background: string } {
    switch (status) {
      case 'PENDING':   return { color: '#f59e0b', background: '#fef3c7' };
      case 'CONFIRMED': return { color: '#3b82f6', background: '#dbeafe' };
      case 'PAID':      return { color: '#10b981', background: '#d1fae5' };
      case 'FULFILLED': return { color: '#8b5cf6', background: '#ede9fe' };
      case 'CANCELLED': return { color: '#6b7280', background: '#f3f4f6' };
      default:          return { color: '#6b7280', background: '#f3f4f6' };
    }
  }

  getBillingCycleLabel(cycle: string): string {
    switch (cycle) {
      case 'MONTHLY': return 'Mensuel';
      case 'ANNUAL':  return 'Annuel';
      default:        return cycle;
    }
  }

  formatCurrency(amount: number, currency: string = 'EUR'): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency,
    }).format(amount);
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  lineTotal(unitPrice: number, quantity: number): number {
    return unitPrice * quantity;
  }

  close(): void {
    this.closed.emit();
  }

  onOverlayClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal-overlay')) {
      this.close();
    }
  }

  onCancel(): void {
    if (this.order) {
      this.cancelled.emit(this.order.id);
    }
  }
}
