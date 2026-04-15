import { Routes } from '@angular/router';

export const PRODUCT_MANAGEMENT_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./product-management.component').then(m => m.ProductManagementComponent),
  },
];
