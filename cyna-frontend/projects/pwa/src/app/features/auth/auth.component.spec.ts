import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';

import { AuthComponent } from './auth.component';

describe('AuthComponent', () => {
  let component: AuthComponent;
  let fixture: ComponentFixture<AuthComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    localStorage.clear();

    await TestBed.configureTestingModule({
      imports: [AuthComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTranslateService(),
      ],
    }).compileComponents();

    fixture   = TestBed.createComponent(AuthComponent);
    component = fixture.componentInstance;
    httpMock  = TestBed.inject(HttpTestingController);

    // Drain AuthService bootstrap HTTP calls.
    httpMock.match(r => r.url.includes('/auth/csrf'))
      .forEach(r => r.flush({ success: true, data: { token: 'tok', headerName: 'X-XSRF-TOKEN' }, timestamp: '' }));
    httpMock.match(r => r.url.includes('/auth/refresh'))
      .forEach(r => r.error(new ProgressEvent('error')));

    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('defaults to login mode and credentials step', () => {
    expect(component.mode).toBe('login');
    expect(component.loginStep).toBe('credentials');
  });

  describe('credential form validation', () => {
    it('loginForm is invalid with empty fields', () => {
      expect(component.loginForm.invalid).toBeTrue();
    });

    it('loginForm is valid with proper email and password', () => {
      component.loginForm.setValue({ email: 'user@example.com', password: 'Secure123!' });
      expect(component.loginForm.valid).toBeTrue();
    });

    it('loginForm rejects an invalid email', () => {
      component.loginForm.setValue({ email: 'not-an-email', password: 'Secure123!' });
      expect(component.loginForm.get('email')?.invalid).toBeTrue();
    });
  });

  describe('submitLogin', () => {
    it('does not fire HTTP when form is invalid', () => {
      component.submitLogin();
      expect(httpMock.match(r => r.url.includes('/auth/login')).length).toBe(0);
    });

    it('advances to OTP step after challenge response', () => {
      component.loginForm.setValue({ email: 'user@example.com', password: 'Secure123!' });
      component.submitLogin();

      httpMock.expectOne(r => r.url.includes('/auth/login'))
        .flush({ success: true, data: { challengeId: 'chal-1', expiresInSeconds: 300 }, timestamp: '' });

      expect(component.loginStep).toBe('otp');
    });

    it('stays on credentials step when login request fails', () => {
      component.loginForm.setValue({ email: 'user@example.com', password: 'WrongPass123!' });
      component.submitLogin();

      httpMock.expectOne(r => r.url.includes('/auth/login'))
        .error(new ProgressEvent('error'), { status: 401 });

      expect(component.loginStep).toBe('credentials');
    });
  });

  describe('backToLogin', () => {
    it('resets loginStep to credentials and clears the OTP form', () => {
      component.loginForm.setValue({ email: 'user@example.com', password: 'Pass123!' });
      component.submitLogin();
      httpMock.expectOne(r => r.url.includes('/auth/login'))
        .flush({ success: true, data: { challengeId: 'c1', expiresInSeconds: 300 }, timestamp: '' });
      expect(component.loginStep).toBe('otp');

      component.backToLogin();

      expect(component.loginStep).toBe('credentials');
      expect(component.otpForm.get('otpCode')?.value).toBeFalsy();
    });
  });

  describe('showForgotPassword', () => {
    it('moves loginStep to forgot and pre-fills email from the login form', () => {
      component.loginForm.patchValue({ email: 'prefill@example.com' });
      component.showForgotPassword();

      expect(component.loginStep).toBe('forgot');
      expect(component.forgotForm.get('email')?.value).toBe('prefill@example.com');
    });
  });

  describe('submitForgot', () => {
    it('does not fire HTTP when forgot form is invalid', () => {
      component.loginStep = 'forgot';
      component.submitForgot();
      expect(httpMock.match(r => r.url.includes('/auth/forgot-password')).length).toBe(0);
    });

    it('sets forgotSent=true on success', () => {
      component.showForgotPassword();
      component.forgotForm.patchValue({ email: 'user@example.com' });
      component.submitForgot();

      httpMock.expectOne(r => r.url.includes('/auth/forgot-password'))
        .flush({ success: true, data: null, timestamp: '' });

      expect(component.forgotSent).toBeTrue();
    });

    it('sets forgotSent=true even on error (anti-enumeration)', () => {
      component.showForgotPassword();
      component.forgotForm.patchValue({ email: 'unknown@example.com' });
      component.submitForgot();

      httpMock.expectOne(r => r.url.includes('/auth/forgot-password'))
        .error(new ProgressEvent('error'), { status: 500 });

      expect(component.forgotSent).toBeTrue();
    });
  });

  describe('password strength helpers', () => {
    beforeEach(() => {
      component.mode = 'register';
      fixture.detectChanges();
    });

    it('hasMinLength is true for passwords ≥ 8 chars', () => {
      component.registerForm.patchValue({ password: 'Abcde1!z' });
      expect(component.hasMinLength()).toBeTrue();
    });

    it('hasMinLength is false for short passwords', () => {
      component.registerForm.patchValue({ password: 'Ab1!' });
      expect(component.hasMinLength()).toBeFalse();
    });

    it('hasLowerAndUpper is true when both cases are present', () => {
      component.registerForm.patchValue({ password: 'Abcde123' });
      expect(component.hasLowerAndUpper()).toBeTrue();
    });

    it('hasNumber is true when a digit is present', () => {
      component.registerForm.patchValue({ password: 'Abcdef1' });
      expect(component.hasNumber()).toBeTrue();
    });

    it('hasSpecialChar is true for recognised special chars', () => {
      component.registerForm.patchValue({ password: 'Abcdef1!' });
      expect(component.hasSpecialChar()).toBeTrue();
    });
  });
});
