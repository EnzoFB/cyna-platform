import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';

type ConfirmState = 'verifying' | 'success' | 'error';

/**
 * Landing page for the confirmation link e-mailed at registration
 * ({@code <APP_PUBLIC_URL>/confirm-email?token=...}). Reads the one-shot token,
 * confirms the account (which logs the user in automatically), then redirects
 * home. On an invalid/expired token it points the user back to login, where a
 * blocked sign-in offers to re-send the email.
 */
@Component({
  selector: 'app-confirm-email',
  standalone: true,
  imports: [TranslatePipe, RouterLink],
  templateUrl: './confirm-email.component.html',
  styleUrl: './confirm-email.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ConfirmEmailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly translate = inject(TranslateService);

  readonly state = signal<ConfirmState>('verifying');

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token || token.trim().length === 0) {
      this.state.set('error');
      return;
    }

    this.authService.confirmEmail(token).subscribe({
      next: () => {
        // confirmEmail already wired the tokens into the session (auto-login).
        this.state.set('success');
        this.toast.showSuccess(this.translate.instant('auth.confirm.success'));
        void this.router.navigateByUrl('/');
      },
      error: () => {
        this.state.set('error');
      }
    });
  }
}
