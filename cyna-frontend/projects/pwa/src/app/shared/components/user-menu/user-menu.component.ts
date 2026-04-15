import {Component, effect, ElementRef, EventEmitter, HostListener, Output, ViewChild} from '@angular/core';
import {RouterLink} from "@angular/router";
import {TranslatePipe, TranslateService} from "@ngx-translate/core";
import {AuthService} from "../../../core/services/auth.service";
import {ToastService} from "../../../core/services/toast.service";

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
  @ViewChild('menuRoot') menuRoot?: ElementRef<HTMLElement>;

  isLogged = false;

  constructor(private authService: AuthService, private toastService: ToastService, private translate: TranslateService) {
    effect(() => {
      this.isLogged = this.authService.isAuthenticated();
    });
  }

  @HostListener('document:click', ['$event'])
  onClick(event: MouseEvent) {
    const target = event.target as HTMLElement;

    if (!target.closest('.user-menu') && !target.closest('.burger')) {
      this.close.emit();
    }
  }

  @HostListener('document:keydown.escape')
  onEscape() {
    this.close.emit();
  }

  logout() {
    const successMessage = this.translate.instant('auth.logout-success')
    this.toastService.showSuccess(successMessage);
    this.authService.logout();
  }

  focusFirstInteractiveElement(): void {
    const root = this.menuRoot?.nativeElement;
    if (!root) {
      return;
    }

    const firstTarget = root.querySelector<HTMLElement>('a, button');
    firstTarget?.focus();
  }

}
