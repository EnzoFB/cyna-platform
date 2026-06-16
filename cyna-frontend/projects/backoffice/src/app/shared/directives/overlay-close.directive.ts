import { Directive, EventEmitter, HostListener, Output } from '@angular/core';

@Directive({
  selector: '[appOverlayClose]',
  standalone: true,
})
export class OverlayCloseDirective {
  @Output() readonly overlayClose = new EventEmitter<void>();

  private mousedownOnSelf = false;

  @HostListener('mousedown', ['$event'])
  onMousedown(event: MouseEvent): void {
    this.mousedownOnSelf = event.target === event.currentTarget;
  }

  @HostListener('click', ['$event'])
  onClick(event: MouseEvent): void {
    if (this.mousedownOnSelf && event.target === event.currentTarget) {
      this.overlayClose.emit();
    }
    this.mousedownOnSelf = false;
  }
}
