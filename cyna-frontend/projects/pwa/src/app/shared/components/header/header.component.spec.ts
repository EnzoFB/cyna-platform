import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { provideRouter } from '@angular/router';
import { provideTranslateService, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../../core/services/auth.service';
import { CartService } from '../../../core/services/cart.service';
import { ToastService } from '../../../core/services/toast.service';
import { HeaderComponent } from './header.component';

@Component({ standalone: true, template: '<p>Page</p>' })
class DummyPageComponent {}

describe('HeaderComponent', () => {
  let fixture: ComponentFixture<HeaderComponent>;
  let component: HeaderComponent;
  let router: Router;
  let translate: TranslateService;

  const cartServiceMock = {
    totalItems: signal(2),
    getOrCreateGuestToken: jasmine.createSpy('getOrCreateGuestToken')
  };
  const authServiceMock = {
    isAuthenticated: signal(false),
    logout: jasmine.createSpy('logout')
  };
  const toastServiceMock = {
    showSuccess: jasmine.createSpy('showSuccess')
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HeaderComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([
          { path: 'home', component: DummyPageComponent },
          { path: 'offers', component: DummyPageComponent },
          { path: 'catalog', component: DummyPageComponent },
          { path: 'cart', component: DummyPageComponent },
          { path: 'auth', component: DummyPageComponent },
          { path: 'account', component: DummyPageComponent },
          { path: 'cgu', component: DummyPageComponent },
          { path: 'mentions-legales', component: DummyPageComponent },
          { path: 'contact', component: DummyPageComponent }
        ]),
        provideTranslateService(),
        { provide: CartService, useValue: cartServiceMock },
        { provide: AuthService, useValue: authServiceMock },
        { provide: ToastService, useValue: toastServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(HeaderComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    translate = TestBed.inject(TranslateService);

    translate.setTranslation('fr', {
      header: {
        homepage: 'Accueil',
        offers: 'Offres',
        catalog: 'Catalogue',
        cart: 'Panier',
        searchbar: 'Rechercher',
        language: 'Changer de langue',
        menu: { toggle: 'Ouvrir le menu', login: 'Connexion', register: 'Inscription' }
      },
      global: {
        'cgu-cgv': 'CGU',
        'legal-notice': 'Mentions',
        contact: 'Contact'
      }
    }, true);
    void translate.use('fr');

    fixture.detectChanges();
  });

  it('affiche le header et les liens principaux', () => {
    const header = fixture.nativeElement.querySelector('header.app-header');
    const links = Array.from(fixture.nativeElement.querySelectorAll('.header-bottom a')) as HTMLAnchorElement[];

    expect(header).toBeTruthy();
    expect(links.map(link => link.textContent?.trim())).toEqual(['Accueil', 'Offres', 'Catalogue']);
  });

  it('navigue vers /cart au clic sur le lien panier', async () => {
    const cartLink = fixture.nativeElement.querySelector('.cart') as HTMLAnchorElement;
    cartLink.click();

    await fixture.whenStable();
    expect(router.url).toBe('/cart');
  });

  it('applique l etat actif sur le lien courant', async () => {
    await router.navigateByUrl('/catalog');
    fixture.detectChanges();

    const navLinks = Array.from(
      fixture.nativeElement.querySelectorAll('.header-bottom a')
    ) as HTMLAnchorElement[];
    const catalogLink = navLinks.find(link => link.textContent?.trim() === 'Catalogue') as HTMLAnchorElement;

    expect(catalogLink.classList.contains('active')).toBeTrue();
    expect(catalogLink.getAttribute('aria-current')).toBe('page');
  });

  it('ouvre et ferme le menu burger + met a jour aria-expanded', () => {
    const burgerButton = fixture.nativeElement.querySelector('.burger') as HTMLButtonElement;

    expect(burgerButton.getAttribute('aria-expanded')).toBe('false');

    burgerButton.click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-user-menu')).toBeTruthy();
    expect(burgerButton.getAttribute('aria-expanded')).toBe('true');

    burgerButton.click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-user-menu')).toBeFalsy();
    expect(burgerButton.getAttribute('aria-expanded')).toBe('false');
  });

  it('gere focus et fermeture clavier (Escape)', async () => {
    const burgerButton = fixture.nativeElement.querySelector('.burger') as HTMLButtonElement;

    burgerButton.click();
    fixture.detectChanges();
    await fixture.whenStable();

    const firstMenuLink = fixture.nativeElement.querySelector('app-user-menu a') as HTMLAnchorElement;
    expect(document.activeElement).toBe(firstMenuLink);

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-user-menu')).toBeFalsy();
    expect(document.activeElement).toBe(burgerButton);
  });

  it('ferme le menu sur changement de route', async () => {
    component.toggleMenu();
    fixture.detectChanges();

    await router.navigateByUrl('/catalog');
    fixture.detectChanges();

    expect(component.menuOpen).toBeFalse();
    expect(fixture.nativeElement.querySelector('app-user-menu')).toBeFalsy();
  });

  it('declare les attributs d accessibilite attendus', () => {
    const nav = fixture.nativeElement.querySelector('nav[aria-label="Navigation principale"]');
    const searchInput = fixture.nativeElement.querySelector('#header-search') as HTMLInputElement;
    const burgerButton = fixture.nativeElement.querySelector('.burger') as HTMLButtonElement;

    expect(nav).toBeTruthy();
    expect(searchInput.type).toBe('search');
    expect(searchInput.getAttribute('aria-label')).toBeTruthy();
    expect(burgerButton.getAttribute('aria-controls')).toBe('user-menu-panel');
  });
});
