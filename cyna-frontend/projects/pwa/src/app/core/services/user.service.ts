import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { UserResponse } from '../models/user.model';

export interface UpdateProfilePayload {
  firstName?: string;
  lastName?: string;
  company?: string | null;
}

export interface RequestEmailChangePayload {
  newEmail: string;
  lang: string;
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);

  getProfile(): Observable<ApiResponse<UserResponse>> {
    return this.http.get<ApiResponse<UserResponse>>(`${environment.apiUrl}/account`);
  }

  updateProfile(payload: UpdateProfilePayload): Observable<ApiResponse<void>> {
    return this.http.patch<ApiResponse<void>>(`${environment.apiUrl}/account/profile`, payload);
  }

  requestEmailChange(payload: RequestEmailChangePayload): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${environment.apiUrl}/account/email/request-change`, payload);
  }

  confirmEmailChange(token: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(
      `${environment.apiUrl}/account/email/confirm`,
      null,
      { params: { token } }
    );
  }
}
