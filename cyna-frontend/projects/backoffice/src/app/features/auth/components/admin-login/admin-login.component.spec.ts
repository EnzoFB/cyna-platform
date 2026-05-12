import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AdminLoginComponent } from './admin-login.component';
import { AuthService } from '../../../../core/services/auth.service';

describe('AdminLoginComponent', () => {
  let component: AdminLoginComponent;
  let fixture: ComponentFixture<AdminLoginComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['login', 'verifyOtp', 'setTokens']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [AdminLoginComponent, ReactiveFormsModule],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminLoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should render the login form with expected fields', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('input[formControlName="email"]')).toBeTruthy();
    expect(el.querySelector('input[formControlName="password"]')).toBeTruthy();
    expect(el.querySelector('button[type="submit"]')).toBeTruthy();
  });

  it('should display "Connexion Administrateur" title', () => {
    const el: HTMLElement = fixture.nativeElement;
    const title = el.querySelector('.login-card__title');
    expect(title?.textContent?.trim()).toBe('Connexion Administrateur');
  });

  it('should display branding with LOGO and CYNA', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.login-branding__logo')?.textContent?.trim()).toBe('LOGO');
    expect(el.querySelector('.login-branding__name')?.textContent?.trim()).toBe('CYNA');
  });

  it('should have the submit button disabled when form is empty', () => {
    const btn = fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement;
    expect(btn.disabled).toBeTrue();
  });

  it('should have the submit button enabled when form is valid', () => {
    component['form'].setValue({ email: 'admin@test.com', password: 'secret' });
    fixture.detectChanges();
    const btn = fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement;
    expect(btn.disabled).toBeFalse();
  });

  it('should mark email as invalid for non-email values', () => {
    component['form'].controls.email.setValue('not-an-email');
    expect(component['form'].controls.email.valid).toBeFalse();
  });

  it('should switch to the OTP step after a successful credentials submit', fakeAsync(() => {
    // The login endpoint now returns a challenge; tokens are issued only after
    // the OTP is verified in the next step.
    authServiceSpy.login.and.returnValue(of({
      success: true,
      data: { challengeId: 'chal_123', expiresInSeconds: 300 },
      timestamp: '2026-05-11T00:00:00Z',
    }));

    component['form'].setValue({ email: 'admin@test.com', password: 'secret' });
    component['onSubmit']();
    tick();
    fixture.detectChanges();

    expect(authServiceSpy.login).toHaveBeenCalledWith('admin@test.com', 'secret');
    expect(component['step']()).toBe('otp');
    // No token issuance and no navigation at this step — both happen on verifyOtp.
    expect(authServiceSpy.setTokens).not.toHaveBeenCalled();
    expect(routerSpy.navigate).not.toHaveBeenCalled();
    // OTP input is now rendered.
    expect(fixture.nativeElement.querySelector('input[formControlName="otpCode"]')).toBeTruthy();
  }));

  it('should navigate to / after a successful OTP verification', fakeAsync(() => {
    // First reach the OTP step.
    authServiceSpy.login.and.returnValue(of({
      success: true,
      data: { challengeId: 'chal_456', expiresInSeconds: 300 },
      timestamp: '2026-05-11T00:00:00Z',
    }));
    component['form'].setValue({ email: 'admin@test.com', password: 'secret' });
    component['onSubmit']();
    tick();
    fixture.detectChanges();

    // Then verify the OTP — verifyOtp() returns the AuthTokens response and
    // sets tokens internally; the spec only checks the navigation effect.
    authServiceSpy.verifyOtp.and.returnValue(of({
      success: true,
      data: { accessToken: 'access', refreshToken: 'refresh' },
      timestamp: '2026-05-11T00:00:00Z',
    } as any));
    component['otpForm'].setValue({ otpCode: '123456' });
    component['onVerifyOtp']();
    tick();

    expect(authServiceSpy.verifyOtp).toHaveBeenCalledWith('chal_456', '123456');
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/']);
  }));

  it('should display error message on login failure', fakeAsync(() => {
    authServiceSpy.login.and.returnValue(throwError(() => new Error('Unauthorized')));

    component['form'].setValue({ email: 'admin@test.com', password: 'wrong' });
    component['onSubmit']();
    tick();
    fixture.detectChanges();

    const errorEl = fixture.nativeElement.querySelector('.login-card__error');
    expect(errorEl).toBeTruthy();
    expect(errorEl.textContent).toContain('Email ou mot de passe invalide');
  }));

  it('should not call authService.login when form is invalid', () => {
    component['onSubmit']();
    expect(authServiceSpy.login).not.toHaveBeenCalled();
  });
});
