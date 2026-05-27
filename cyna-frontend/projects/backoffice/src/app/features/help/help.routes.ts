import { Routes } from '@angular/router';

export const HELP_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./components/help-guide-page/help-guide-page.component').then(
        m => m.HelpGuidePageComponent
      ),
  },
];
