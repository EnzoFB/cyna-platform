import { Routes } from '@angular/router';

export const CATEGORY_MANAGEMENT_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./components/category-list/category-list.component').then(
        m => m.CategoryListComponent
      ),
  },
];
