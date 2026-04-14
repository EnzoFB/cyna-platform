import { Routes } from '@angular/router';

export const PRODUCT_MANAGEMENT_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./components/product-list/product-list.component').then(
        m => m.ProductListComponent
      ),
  },
];
