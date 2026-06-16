import { Component } from '@angular/core';
import {NgOptimizedImage} from "@angular/common";
import {Router, RouterLink} from "@angular/router";
import {UserMenuComponent} from "../user-menu/user-menu.component";
import {TranslateService, TranslatePipe} from "@ngx-translate/core";



@Component({
  selector: 'app-header',
  imports: [
    NgOptimizedImage,
    RouterLink,
    UserMenuComponent,
    TranslatePipe
  ],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss',
})
export class HeaderComponent {

  menuOpen:boolean = false;
  currentLang:string;

  constructor(private router: Router, private translate: TranslateService) {this.currentLang = this.translate.getCurrentLang()}

  isActive(route: string): boolean {
    return this.router.url === route;
  }

  navigate(event: Event, route: string) {
    if (this.isActive(route)) {
      event.preventDefault();
    }
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
