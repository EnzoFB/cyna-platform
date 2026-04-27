import { ChangeDetectionStrategy, Component } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-history',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './history.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HistoryComponent {}
