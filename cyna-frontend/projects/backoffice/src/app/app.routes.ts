import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    canActivate: [authGuard],
    data: { role: 'ADMIN' },
    loadComponent: () => import('./shared/layout/shell/shell.component').then(m => m.ShellComponent),
    children: [
      {
        path: '',
        data: { title: 'Dashboard' },
        loadChildren: () => import('./features/dashboard/dashboard.routes').then(m => m.DASHBOARD_ROUTES),
      },
      {
        path: 'products',
        data: { title: 'Produits' },
        loadChildren: () => import('./features/product-management/product-management.routes').then(m => m.PRODUCT_MANAGEMENT_ROUTES),
      },
      {
        path: 'categories',
        data: { title: 'Catégories' },
        loadChildren: () => import('./features/category-management/category-management.routes').then(m => m.CATEGORY_MANAGEMENT_ROUTES),
      },
      {
        path: 'orders',
        data: { title: 'Commandes' },
        loadChildren: () => import('./features/order-management/order-management.routes').then(m => m.ORDER_MANAGEMENT_ROUTES),
      },
      {
        path: 'users',
        data: { title: 'Utilisateurs' },
        loadChildren: () => import('./features/user-management/user-management.routes').then(m => m.USER_MANAGEMENT_ROUTES),
      },
    ],
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/components/admin-login/admin-login.component').then(m => m.AdminLoginComponent),
  },
  {
    path: '**',
    redirectTo: '',
  },
];
