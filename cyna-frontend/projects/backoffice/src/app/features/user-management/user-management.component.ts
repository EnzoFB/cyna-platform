import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-user-management',
  standalone: true,
  template: `<h1>User Management</h1>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserManagementComponent {}
