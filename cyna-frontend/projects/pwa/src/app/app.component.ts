import {Component, OnInit} from '@angular/core';
import {Event, NavigationEnd, Router, RouterOutlet} from '@angular/router';
import { HeaderComponent } from './shared/components/header/header.component';
import { CommonModule } from '@angular/common';
import {FooterComponent} from "./shared/components/footer/footer.component";
import {TranslatePipe, TranslateService} from "@ngx-translate/core";
import {ToastService} from "./core/services/toast.service";
import {ToastrService} from "ngx-toastr";

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, HeaderComponent, CommonModule, FooterComponent, TranslatePipe],
  template: `
    <button type="button" class="skip-link" (click)="skipToContent()">
      {{ 'global.skipToContent' | translate }}
    </button>

    <div class="app-layout">
      <app-header />

      <main id="main-content" class="app-content" tabindex="-1">
        <router-outlet />
      </main>

      @if (showFooter) {
        <app-footer />
      }
    </div>
  `,
})
export class AppComponent implements OnInit {
  showFooter = false;

  options = {
    timeOut: 3000,
    positionClass: 'custom-toast-position',
  };

  constructor(
    private router: Router,
    private toastService: ToastService,
    private toastr: ToastrService,
    translate: TranslateService) {
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

  skipToContent(): void {
    // Defer one tick so Angular's click handling completes before we move focus
    setTimeout(() => {
      const main = document.getElementById('main-content');
      if (main) main.focus();
    }, 0);
  }

  ngOnInit() {
    // Session restoration is wired via provideAppInitializer in app.config.ts
    // so the auth state is settled before the first route is mounted.
    this.toastService.toast$.subscribe(toast => {
      switch (toast.type) {
        case 'success':
          this.toastr.success(toast.message, '', this.options);
          break;
        case 'error':
          this.toastr.error(toast.message, '', this.options);
          break;
        case 'warning':
          this.toastr.warning(toast.message, '', this.options);
          break;
        case 'info':
          this.toastr.info(toast.message, '', this.options);
          break;
      }
    });
  }
}
