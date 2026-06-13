import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type IconName =
  | 'user'
  | 'envelope'
  | 'lock'
  | 'building'
  | 'shield'
  | 'check';

@Component({
  selector: 'app-icon',
  standalone: true,
  template: `
    @switch (name()) {
      @case ('user') {
        <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
          <circle cx="12" cy="8" r="4"/>
          <path d="M4 20c0-3.87 3.582-7 8-7s8 3.13 8 7"/>
        </svg>
      }
      @case ('envelope') {
        <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
          <rect x="3" y="5" width="18" height="14" rx="2"/>
          <path d="M3 7l9 6 9-6"/>
        </svg>
      }
      @case ('lock') {
        <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
          <rect x="5" y="11" width="14" height="10" rx="2"/>
          <path d="M8 11V7a4 4 0 0 1 8 0v4"/>
        </svg>
      }
      @case ('building') {
        <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
          <rect x="4" y="3" width="16" height="18"/>
          <path d="M4 9h16"/>
          <path d="M9 3v6M15 3v6"/>
          <path d="M9 14h1M14 14h1M9 18h1M14 18h1"/>
        </svg>
      }
      @case ('shield') {
        <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
          <path d="M12 3l7 3v5c0 5-3.5 8.5-7 9.5C8.5 19.5 5 16 5 11V6l7-3z"/>
          <path d="M9 12l2 2 4-4"/>
        </svg>
      }
      @case ('check') {
        <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
          <path d="M5 13l4 4L19 7"/>
        </svg>
      }
    }
  `,
  styles: [`
    :host {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    svg {
      display: block;
      width: 16px;
      height: 16px;
      fill: none;
      stroke: currentColor;
      stroke-width: 1.5;
      stroke-linecap: round;
      stroke-linejoin: round;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class IconComponent {
  readonly name = input.required<IconName>();
}
