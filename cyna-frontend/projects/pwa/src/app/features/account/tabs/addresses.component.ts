import { ChangeDetectionStrategy, Component } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-addresses',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './addresses.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AddressesComponent {}
