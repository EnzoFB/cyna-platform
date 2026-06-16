import {Component, EventEmitter, HostListener, Output} from '@angular/core';
import {RouterLink} from "@angular/router";
import {TranslatePipe} from "@ngx-translate/core";

@Component({
  selector: 'app-user-menu',
  imports: [
    RouterLink,
    TranslatePipe
  ],
  templateUrl: './user-menu.component.html',
  styleUrl: './user-menu.component.scss',
})
export class UserMenuComponent {

  @Output() close = new EventEmitter<void>();

  isLogged = false;
//   TODO verifier la connexion du user

  @HostListener('document:click', ['$event'])
  onClick(event: MouseEvent) {
    const target = event.target as HTMLElement;

    if (!target.closest('.user-menu') && !target.closest('.burger')) {
      this.close.emit();
    }
  }

}
