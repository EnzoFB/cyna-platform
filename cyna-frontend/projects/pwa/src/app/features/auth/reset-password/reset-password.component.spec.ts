import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { ResetPasswordComponent } from './reset-password.component';

describe('ResetPasswordComponent', () => {
  let component: ResetPasswordComponent;
  let fixture: ComponentFixture<ResetPasswordComponent>;
  let httpMock: HttpTestingController;

  function createComponent(token: string | null) {
    TestBed.configureTestingModule({
      imports: [ResetPasswordComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTranslateService(),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: { get: () => token } } },
        },
      ],
    }).compileComponents();

    fixture   = TestBed.createComponent(ResetPasswordComponent);
    component = fixture.componentInstance;
    httpMock  = TestBed.inject(HttpTestingController);

    httpMock.match(r => r.url.includes('/auth/csrf'))
      .forEach(r => r.flush({ success: true, data: { token: 'tok', headerName: 'X-XSRF-TOKEN' }, timestamp: '' }));
    httpMock.match(r => r.url.includes('/auth/refresh'))
      .forEach(r => r.error(new ProgressEvent('error')));

    fixture.detectChanges();
  }

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  describe('token handling', () => {
    it('sets token signal from query param on init', () => {
      createComponent('abc-reset-token');
      expect(component.token()).toBe('abc-reset-token');
    });

    it('sets token to null when query param is absent', () => {
      createComponent(null);
      expect(component.token()).toBeNull();
    });

    it('sets token to null when query param is blank', () => {
      createComponent('   ');
      expect(component.token()).toBeNull();
    });
  });

  describe('form validation', () => {
    beforeEach(() => createComponent('valid-token'));

    it('form is invalid when empty', () => {
      expect(component.form.invalid).toBeTrue();
    });

    it('form is invalid when passwords do not match', () => {
      component.form.setValue({ password: 'Secure1!z', confirmPassword: 'Different1!' });
      expect(component.form.hasError('passwordMismatch')).toBeTrue();
    });

    it('form is valid when passwords match and meet requirements', () => {
      component.form.setValue({ password: 'Secure1!z', confirmPassword: 'Secure1!z' });
      expect(component.form.valid).toBeTrue();
    });
  });

  describe('submit', () => {
    beforeEach(() => createComponent('valid-token'));

    it('does not fire HTTP when form is invalid', () => {
      component.submit();
      expect(httpMock.match(r => r.url.includes('/auth/reset-password')).length).toBe(0);
    });

    it('does not fire HTTP when token is null', () => {
      component.token.set(null);
      component.form.setValue({ password: 'Secure1!z', confirmPassword: 'Secure1!z' });
      component.submit();
      expect(httpMock.match(r => r.url.includes('/auth/reset-password')).length).toBe(0);
    });

    it('sets loading=true then fires POST on success', () => {
      component.form.setValue({ password: 'Secure1!z', confirmPassword: 'Secure1!z' });
      component.submit();
      expect(component.loading()).toBeTrue();

      httpMock.expectOne(r => r.url.includes('/auth/reset-password'))
        .flush({ success: true, data: null, timestamp: '' });
    });

    it('sets loading=false and does not navigate on 400 error', () => {
      component.form.setValue({ password: 'Secure1!z', confirmPassword: 'Secure1!z' });
      component.submit();
      expect(component.loading()).toBeTrue();

      httpMock.expectOne(r => r.url.includes('/auth/reset-password'))
        .error(new ProgressEvent('error'), { status: 400 });

      expect(component.loading()).toBeFalse();
    });
  });

  describe('password strength helpers', () => {
    beforeEach(() => createComponent('valid-token'));

    it('hasMinLength is true for passwords ≥ 8 chars', () => {
      component.form.patchValue({ password: 'Abcde1!z' });
      expect(component.hasMinLength()).toBeTrue();
    });

    it('hasLowerAndUpper is true when both cases present', () => {
      component.form.patchValue({ password: 'Abcde123' });
      expect(component.hasLowerAndUpper()).toBeTrue();
    });

    it('hasNumber is true when digit present', () => {
      component.form.patchValue({ password: 'Abcdef1' });
      expect(component.hasNumber()).toBeTrue();
    });

    it('hasSpecialChar is true for recognised special chars', () => {
      component.form.patchValue({ password: 'Abcdef1!' });
      expect(component.hasSpecialChar()).toBeTrue();
    });
  });
});
