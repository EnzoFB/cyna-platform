import { Component } from '@angular/core';
import {Event, NavigationEnd, Router, RouterOutlet} from '@angular/router';
import { HeaderComponent } from './shared/components/header/header.component';
import { CommonModule } from '@angular/common';
import {FooterComponent} from "./shared/components/footer/footer.component";
import {TranslateService} from "@ngx-translate/core";

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, HeaderComponent, CommonModule, FooterComponent],
  template: `
    <app-header />

    <router-outlet />

    @if (showFooter) {
      <app-footer />
    }
  `,
})
export class AppComponent {
  showFooter = true;

  constructor(private router: Router, translate: TranslateService) {
    const savedLang = localStorage.getItem('lang') || 'fr';

    translate.addLangs(['fr','en']);
    translate.setDefaultLang('fr');
    translate.use(savedLang);

    this.router.events.subscribe((event: Event) => {
      if (event instanceof NavigationEnd) {
        const hiddenRoutes = ['/home'];
        this.showFooter = !hiddenRoutes.includes(event.urlAfterRedirects);
      }
    });
  }
}
