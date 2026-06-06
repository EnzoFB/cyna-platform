import { Component } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-help-guide-page',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './help-guide-page.component.html',
  styleUrl: './help-guide-page.component.scss',
})
export class HelpGuidePageComponent {
  protected scrollToSection(sectionId: string): void {
    document.getElementById(sectionId)?.scrollIntoView({
      behavior: 'smooth',
      block: 'start',
    });
  }
}

// Compatibility export for stale lazy-route chunks that may still reference
// HelpGuidePageComponent2 in browser cache.
export { HelpGuidePageComponent as HelpGuidePageComponent2 };
