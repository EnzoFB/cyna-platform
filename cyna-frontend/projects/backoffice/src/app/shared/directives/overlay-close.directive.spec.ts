import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { OverlayCloseDirective } from './overlay-close.directive';

@Component({
  standalone: true,
  imports: [OverlayCloseDirective],
  template: `
    <div class="overlay" appOverlayClose (overlayClose)="onClose()">
      <div class="card">content</div>
    </div>
  `,
})
class TestHostComponent {
  closed = false;
  onClose() { this.closed = true; }
}

describe('OverlayCloseDirective', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let host: TestHostComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    host = fixture.componentInstance;
    fixture.detectChanges();
  });

  function overlayEl(): HTMLElement {
    return fixture.debugElement.query(By.css('.overlay')).nativeElement;
  }

  function cardEl(): HTMLElement {
    return fixture.debugElement.query(By.css('.card')).nativeElement;
  }

  function fire(target: HTMLElement, type: string, currentTarget: HTMLElement): void {
    const event = new MouseEvent(type, { bubbles: true });
    Object.defineProperty(event, 'target', { value: target });
    Object.defineProperty(event, 'currentTarget', { value: currentTarget });
    currentTarget.dispatchEvent(event);
  }

  it('emits overlayClose when mousedown and click both land on the overlay', () => {
    const overlay = overlayEl();
    fire(overlay, 'mousedown', overlay);
    fire(overlay, 'click', overlay);
    expect(host.closed).toBeTrue();
  });

  it('does not emit when mousedown starts inside the card and mouseup lands on the overlay (drag)', () => {
    const overlay = overlayEl();
    const card = cardEl();
    // mousedown on card bubbles up — target is card, currentTarget is overlay
    fire(card, 'mousedown', overlay);
    fire(overlay, 'click', overlay);
    expect(host.closed).toBeFalse();
  });

  it('does not emit when click lands inside the card (even if mousedown was on overlay)', () => {
    const overlay = overlayEl();
    const card = cardEl();
    fire(overlay, 'mousedown', overlay);
    fire(card, 'click', overlay);
    expect(host.closed).toBeFalse();
  });

  it('resets the flag after each click so subsequent drags are still blocked', () => {
    const overlay = overlayEl();
    const card = cardEl();

    // First: legitimate click → closes
    fire(overlay, 'mousedown', overlay);
    fire(overlay, 'click', overlay);
    expect(host.closed).toBeTrue();

    host.closed = false;

    // Second: drag → should NOT close
    fire(card, 'mousedown', overlay);
    fire(overlay, 'click', overlay);
    expect(host.closed).toBeFalse();
  });
});
