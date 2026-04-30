import { Routes } from '@angular/router';
import { OffersComponent } from './offers.component';

export const OFFERS_ROUTES: Routes = [
  { path: '', component: OffersComponent, pathMatch: 'full' }
];
