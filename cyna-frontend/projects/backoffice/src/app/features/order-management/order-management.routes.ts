import { Routes } from '@angular/router';

export const ORDER_MANAGEMENT_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./order-management.component').then(m => m.OrderManagementComponent),
  },
];
