import { Routes } from '@angular/router';

export const PROMOTION_MANAGEMENT_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./components/promotion-list/promotion-list.component').then(
        m => m.PromotionListComponent
      ),
  },
];
