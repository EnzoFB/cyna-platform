import { Component } from '@angular/core';
import {NgOptimizedImage} from "@angular/common";
import {NavigationStart, Router, RouterLink, RouterLinkActive} from "@angular/router";
import {UserMenuComponent} from "../user-menu/user-menu.component";
import {TranslateService, TranslatePipe} from "@ngx-translate/core";
import {CartService} from "../../../core/services/cart.service";


@Component({
  selector: 'app-header',
  imports: [
    NgOptimizedImage,
    RouterLink,
    UserMenuComponent,
    TranslatePipe,
    RouterLinkActive
  ],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss',
})
export class HeaderComponent {

  menuOpen:boolean = false;
  currentLang:string;
  readonly cartItemsCount;

  constructor(private router: Router, private translate: TranslateService, private cartService: CartService) {
    this.currentLang = this.translate.getCurrentLang();
    this.cartItemsCount = this.cartService.totalItems;
    this.cartService.getOrCreateGuestToken();

    this.router.events.subscribe(event => {
      if (event instanceof  NavigationStart) {
        this.menuOpen = false;
      }
    });
  }

  toggleMenu() {
    this.menuOpen = !this.menuOpen;
  }

  closeMenu() {
    this.menuOpen = false;
  }

  changeLang() {
    const newLang = this.currentLang === 'fr' ? 'en' : 'fr';

    this.translate.use(newLang);
    localStorage.setItem('lang', newLang);

    this.currentLang = newLang;
  }
}
