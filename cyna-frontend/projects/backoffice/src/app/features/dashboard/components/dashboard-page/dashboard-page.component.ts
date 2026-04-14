import { Component } from '@angular/core';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  template: `
    <div class="dashboard-page">
      <h2>Dashboard</h2>
      <p>Bienvenue dans le back-office CYNA.</p>
    </div>
  `,
  styles: [`
    .dashboard-page { padding: 32px; }
    h2 { font-size: 22px; font-weight: 700; color: #141b2d; margin: 0 0 8px; }
    p { color: #6b7280; }
  `],
})
export class DashboardPageComponent {}
