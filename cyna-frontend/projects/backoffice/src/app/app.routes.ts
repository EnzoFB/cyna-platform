import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  {
    path: '',
    canActivate: [authGuard, roleGuard],
    data: { role: 'ADMIN' },
    loadComponent: () => import('./shared/layout/shell/shell.component').then(m => m.ShellComponent),
    children: [
      {
        path: '',
        loadChildren: () => import('./features/dashboard/dashboard.routes').then(m => m.DASHBOARD_ROUTES),
      },
      {
        path: 'products',
        loadChildren: () => import('./features/product-management/product-management.routes').then(m => m.PRODUCT_MANAGEMENT_ROUTES),
      },
      {
        path: 'orders',
        loadChildren: () => import('./features/order-management/order-management.routes').then(m => m.ORDER_MANAGEMENT_ROUTES),
      },
      {
        path: 'users',
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
