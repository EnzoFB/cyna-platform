import { Routes } from '@angular/router';

export const ORDER_MANAGEMENT_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./components/order-list/order-list.component').then(
        m => m.OrderListComponent
      ),
  },
];
