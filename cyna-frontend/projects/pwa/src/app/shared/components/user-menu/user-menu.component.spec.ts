import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { UserMenuComponent } from './user-menu.component';

@Component({ standalone: true, template: '<p>Page</p>' })
class DummyPageComponent {}

describe('UserMenuComponent', () => {
  let fixture: ComponentFixture<UserMenuComponent>;
  let component: UserMenuComponent;
  const isAuthenticated = signal(false);

  const authServiceMock = {
    isAuthenticated,
    logout: jasmine.createSpy('logout')
  };

  const toastServiceMock = {
    showSuccess: jasmine.createSpy('showSuccess')
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserMenuComponent],
      providers: [
        provideRouter([
          { path: 'auth', component: DummyPageComponent },
          { path: 'account', component: DummyPageComponent },
          { path: 'cgu', component: DummyPageComponent },
          { path: 'mentions-legales', component: DummyPageComponent },
          { path: 'contact', component: DummyPageComponent }
        ]),
        provideTranslateService(),
        { provide: AuthService, useValue: authServiceMock },
        { provide: ToastService, useValue: toastServiceMock }
      ]
    }).compileComponents();

    const translate = TestBed.inject(TranslateService);
    translate.setTranslation('fr', {
      header: {
        menu: {
          login: 'Se connecter',
          register: "S'inscrire",
          account: 'Espace client',
          'log-out': 'Se deconnecter'
        }
      },
      global: {
        'cgu-cgv': 'CGU / CGV',
        'legal-notice': 'Mentions legales',
        contact: 'Contact'
      },
      auth: {
        'logout-success': 'Deconnexion reussie'
      }
    }, true);
    void translate.use('fr');
  });

  beforeEach(() => {
    isAuthenticated.set(false);
    authServiceMock.logout.calls.reset();
    toastServiceMock.showSuccess.calls.reset();

    fixture = TestBed.createComponent(UserMenuComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('affiche les liens invite quand utilisateur non connecte', () => {
    const links = Array.from(fixture.nativeElement.querySelectorAll('a')) as HTMLAnchorElement[];
    const linkTexts = links.map(link => link.textContent?.trim());

    expect(linkTexts).toContain('Se connecter');
    expect(linkTexts).toContain("S'inscrire");
    expect(linkTexts).not.toContain('Espace client');
  });

  it('affiche les liens compte et logout quand utilisateur connecte', () => {
    isAuthenticated.set(true);
    fixture.detectChanges();

    const account = fixture.nativeElement.querySelector('a[routerlink="/account"]');
    const logout = fixture.nativeElement.querySelector('button.logout');

    expect(account).toBeTruthy();
    expect(logout).toBeTruthy();
  });

  it('emet close sur click exterieur', () => {
    spyOn(component.close, 'emit');

    const outside = document.createElement('div');
    document.body.appendChild(outside);
    outside.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    document.body.removeChild(outside);

    expect(component.close.emit).toHaveBeenCalled();
  });

  it('n emet pas close sur click burger', () => {
    spyOn(component.close, 'emit');

    const burger = document.createElement('button');
    burger.className = 'burger';
    document.body.appendChild(burger);
    burger.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    document.body.removeChild(burger);

    expect(component.close.emit).not.toHaveBeenCalled();
  });

  it('emet close sur Escape', () => {
    spyOn(component.close, 'emit');

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    expect(component.close.emit).toHaveBeenCalled();
  });

  it('focus le premier element interactif', () => {
    component.focusFirstInteractiveElement();
    fixture.detectChanges();

    const firstLink = fixture.nativeElement.querySelector('a') as HTMLAnchorElement;
    expect(document.activeElement).toBe(firstLink);
  });

  it('logout affiche un toast puis appelle auth.logout', () => {
    component.logout();

    expect(toastServiceMock.showSuccess).toHaveBeenCalledWith('Deconnexion reussie');
    expect(authServiceMock.logout).toHaveBeenCalled();
  });
});
