import { Component, ElementRef, ViewChild, inject } from '@angular/core';
import { NgOptimizedImage } from "@angular/common";
import { NavigationStart, Router, RouterLink, RouterLinkActive } from "@angular/router";
import { UserMenuComponent } from "../user-menu/user-menu.component";
import { TranslateService, TranslatePipe } from "@ngx-translate/core";
import { CartService } from "../../../core/services/cart.service";
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { SearchBarComponent } from '../search-bar/search-bar.component';

@Component({
  selector: 'app-header',
  imports: [
    NgOptimizedImage,
    RouterLink,
    UserMenuComponent,
    TranslatePipe,
    RouterLinkActive,
    SearchBarComponent,
  ],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss',
})
export class HeaderComponent {
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);
  private readonly cartService = inject(CartService);

  menuOpen = false;
  currentLang: string;
  readonly cartItemsCount;

  @ViewChild('burgerButton') burgerButton?: ElementRef<HTMLButtonElement>;
  @ViewChild(UserMenuComponent) userMenu?: UserMenuComponent;
  @ViewChild(SearchBarComponent) searchBar?: SearchBarComponent;

  constructor() {
    this.currentLang = this.translate.getCurrentLang();
    this.cartItemsCount = this.cartService.totalItems;
    this.cartService.getOrCreateGuestToken();

    this.router.events.pipe(takeUntilDestroyed()).subscribe(event => {
      if (event instanceof NavigationStart) {
        this.menuOpen = false;
        this.searchBar?.close();
      }
    });
  }

  toggleMenu() {
    this.menuOpen = !this.menuOpen;
    if (this.menuOpen) {
      queueMicrotask(() => this.userMenu?.focusFirstInteractiveElement());
    }
  }

  closeMenu() {
    this.menuOpen = false;
    this.burgerButton?.nativeElement.focus();
  }

  onMenuEscape() {
    this.closeMenu();
  }

  changeLang() {
    const newLang = this.currentLang === 'fr' ? 'en' : 'fr';
    this.translate.use(newLang);
    localStorage.setItem('lang', newLang);
    this.currentLang = newLang;
  }
}
