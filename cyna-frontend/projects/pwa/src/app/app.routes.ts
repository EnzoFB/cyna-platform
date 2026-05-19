import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'home',
    pathMatch: 'full'
  },{
    path: 'home',
    loadChildren: () => import('./features/home/home.routes').then(m => m.HOME_ROUTES),
  },
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.routes').then(m => m.AUTH_ROUTES),
  },
  {
    path: 'catalog',
    loadChildren: () => import('./features/catalog/catalog.routes').then(m => m.CATALOG_ROUTES),
  },
  {
    path: 'offers',
    loadChildren: () => import('./features/offers/offers.routes').then(m => m.OFFERS_ROUTES),
  },
  {
    path: 'cart',
    loadChildren: () => import('./features/cart/cart.routes').then(m => m.CART_ROUTES),
  },
  {
    path: 'checkout',
    canActivate: [authGuard],
    data: {
      showAuthToast: true,
      toastKey: 'cartPage.toastAuthRequired'
    },
    loadChildren: () => import('./features/checkout/checkout.routes').then(m => m.CHECKOUT_ROUTES),
  },
  {
    path: 'account',
    canActivate: [authGuard],
    data: {
      showAuthToast: true,
      toastKey: 'account.toastAuthRequired'
    },
    loadChildren: () => import('./features/account/account.routes').then(m => m.ACCOUNT_ROUTES),
  },
  {
    path: 'terms',
    loadChildren: () => import('./features/terms/terms.routes').then(m => m.TERMS_ROUTES),
  },
  {
    path: 'legal-notice',
    loadChildren: () => import('./features/legal-notice/legal-notice.routes').then(m => m.LEGAL_NOTICE_ROUTES),
  },
  {
    path: 'about',
    loadChildren: () => import('./features/about/about.routes').then(m => m.ABOUT_ROUTES),
  },
  {
    path: 'contact',
    loadChildren: () => import('./features/contact/contact.routes').then(m => m.CONTACT_ROUTES),
  },
  {
    path: '**',
    redirectTo: 'home',
  },
];
