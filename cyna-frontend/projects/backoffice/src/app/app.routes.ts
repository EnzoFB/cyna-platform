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
        pathMatch: 'full',
        data: { title: 'shell.nav.dashboard' },
        loadChildren: () => import('./features/dashboard/dashboard.routes').then(m => m.DASHBOARD_ROUTES),
      },
      {
        path: 'products',
        data: { title: 'shell.nav.products' },
        loadChildren: () => import('./features/product-management/product-management.routes').then(m => m.PRODUCT_MANAGEMENT_ROUTES),
      },
      {
        path: 'categories',
        data: { title: 'shell.nav.categories' },
        loadChildren: () => import('./features/category-management/category-management.routes').then(m => m.CATEGORY_MANAGEMENT_ROUTES),
      },
      {
        path: 'promotions',
        data: { title: 'shell.nav.promotions' },
        loadChildren: () => import('./features/promotion-management/promotion-management.routes').then(m => m.PROMOTION_MANAGEMENT_ROUTES),
      },
      {
        path: 'users',
        data: { title: 'shell.nav.users' },
        loadChildren: () => import('./features/user-management/user-management.routes').then(m => m.USER_MANAGEMENT_ROUTES),
      },
      {
        path: 'orders',
        data: { title: 'shell.nav.orders' },
        loadChildren: () => import('./features/order-management/order-management.routes').then(m => m.ORDER_MANAGEMENT_ROUTES),
      },
      {
        path: 'help',
        data: { title: 'help.pageTitle' },
        loadChildren: () => import('./features/help/help.routes').then(m => m.HELP_ROUTES),
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
