import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';

/**
 * RGPD self-service endpoints for the authenticated user:
 *  - Art. 15/20 export ("download my data")
 *  - Art. 17 erasure ("delete my account")
 * Auth is carried by the bearer interceptor + the refresh cookie.
 */
@Injectable({ providedIn: 'root' })
export class AccountPrivacyService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/account`;

  /** Downloads the personal-data export as a JSON file. */
  exportMyData(): Observable<void> {
    return this.http
      .get(`${this.base}/export`, { responseType: 'blob', withCredentials: true })
      .pipe(map(blob => this.triggerDownload(blob, 'cyna-my-data.json')));
  }

  /** Erases (anonymizes or deletes) the authenticated user's account. */
  deleteMyAccount(): Observable<void> {
    return this.http
      .delete<unknown>(`${this.base}`, { withCredentials: true })
      .pipe(map(() => void 0));
  }

  private triggerDownload(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  }
}
