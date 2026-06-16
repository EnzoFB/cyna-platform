import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-order-management',
  standalone: true,
  template: `<h1>Order Management</h1>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrderManagementComponent {}
