import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-product-management',
  standalone: true,
  template: `<h1>Product Management</h1>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductManagementComponent {}
