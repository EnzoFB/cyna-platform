import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { ContactComponent } from './contact.component';

describe('ContactComponent', () => {
  let component: ContactComponent;
  let fixture: ComponentFixture<ContactComponent>;
  let httpMock: HttpTestingController;

  const validForm = {
    name: 'Alice Dupont',
    email: 'alice@example.com',
    subject: 'Question',
    message: 'Bonjour, j\'ai une question.',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ContactComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTranslateService(),
      ],
    }).compileComponents();

    fixture   = TestBed.createComponent(ContactComponent);
    component = fixture.componentInstance;
    httpMock  = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('form validation', () => {
    it('form is invalid when empty', () => {
      expect(component.form.invalid).toBeTrue();
    });

    it('form is valid with all required fields', () => {
      component.form.setValue(validForm);
      expect(component.form.valid).toBeTrue();
    });

    it('email field rejects an invalid email', () => {
      component.form.patchValue({ email: 'not-an-email' });
      expect(component.form.get('email')?.invalid).toBeTrue();
    });
  });

  describe('submit', () => {
    it('marks all fields as touched and does not send HTTP when form is invalid', () => {
      component.submit();
      expect(httpMock.match(r => r.url.includes('/contact')).length).toBe(0);
      expect(component.form.get('name')?.touched).toBeTrue();
    });

    it('sends POST /contact and sets submittedSuccessfully=true on success', () => {
      component.form.setValue(validForm);
      component.submit();

      expect(component.isSubmitting).toBeTrue();

      const req = httpMock.expectOne(r => r.url.includes('/contact'));
      expect(req.request.method).toBe('POST');
      expect(req.request.body.name).toBe('Alice Dupont');
      expect(req.request.body.email).toBe('alice@example.com');
      req.flush({ success: true, data: null, timestamp: '' });

      expect(component.submittedSuccessfully).toBeTrue();
      expect(component.isSubmitting).toBeFalse();
      expect(component.form.get('name')?.value).toBeFalsy();
    });

    it('sets isSubmitting=false on error and does not set submittedSuccessfully', () => {
      component.form.setValue(validForm);
      component.submit();

      httpMock.expectOne(r => r.url.includes('/contact'))
        .error(new ProgressEvent('error'), { status: 500 });

      expect(component.isSubmitting).toBeFalse();
      expect(component.submittedSuccessfully).toBeFalse();
    });
  });

  describe('hasError', () => {
    it('returns false before the field is touched', () => {
      expect(component.hasError('email', 'required')).toBeFalse();
    });

    it('returns true after the field is touched with an error', () => {
      const ctrl = component.form.get('email')!;
      ctrl.markAsTouched();
      expect(component.hasError('email', 'required')).toBeTrue();
    });
  });
});
