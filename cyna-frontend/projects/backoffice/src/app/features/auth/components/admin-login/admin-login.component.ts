import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-admin-login',
  standalone: true,
  template: `
    <div class="login-container">
      <h1>Admin Login</h1>
      <p>Login page placeholder</p>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminLoginComponent {}
