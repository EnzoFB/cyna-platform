import { Routes } from '@angular/router';

export const USER_MANAGEMENT_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./user-management.component').then(m => m.UserManagementComponent),
  },
];
