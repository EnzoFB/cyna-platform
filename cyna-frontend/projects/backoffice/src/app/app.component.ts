import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: `<router-outlet />`,
})
export class AppComponent {
  private readonly translate = inject(TranslateService);

  constructor() {
    const savedLang = localStorage.getItem('lang');
    const initialLang = savedLang === 'en' || savedLang === 'fr' ? savedLang : 'fr';

    this.translate.addLangs(['fr', 'en']);
    this.translate.setDefaultLang('fr');
    this.translate.use(initialLang);
  }
}
