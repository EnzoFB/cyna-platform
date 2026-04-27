import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { UserFormModalComponent } from './user-form-modal.component';
import { AdminUser } from '../../../../core/services/user.service';

describe('UserFormModalComponent', () => {
  let component: UserFormModalComponent;
  let fixture: ComponentFixture<UserFormModalComponent>;

  const mockUser: AdminUser = {
    id: 'abc12345-6789',
    email: 'john@example.com',
    firstName: 'John',
    lastName: 'Doe',
    phone: '+33 6 12 34 56 78',
    role: 'CUSTOMER',
    status: 'ACTIVE',
    totalPurchases: 100,
    orderCount: 5,
    address: '123 Main St',
    createdAt: '2025-01-01T00:00:00Z',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserFormModalComponent, ReactiveFormsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(UserFormModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should not render when open is false', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.modal-overlay')).toBeNull();
  });

  it('should render modal when open is true', () => {
    component.open = true;
    component.ngOnChanges({
      open: { currentValue: true, previousValue: false, firstChange: false, isFirstChange: () => false },
    });
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.modal-overlay')).toBeTruthy();
    expect(el.querySelector('.modal-header h2')?.textContent?.trim()).toBe('Nouvel utilisateur');
  });

  it('should show create form with email and password fields in create mode', () => {
    component.open = true;
    component.user = null;
    component.ngOnChanges({
      open: { currentValue: true, previousValue: false, firstChange: false, isFirstChange: () => false },
    });
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('#email')).toBeTruthy();
    expect(el.querySelector('#password')).toBeTruthy();
    expect(el.querySelector('#status')).toBeNull();
  });

  it('should show edit form without password and with status in edit mode', () => {
    component.open = true;
    component.user = mockUser;
    component.ngOnChanges({
      open: { currentValue: true, previousValue: false, firstChange: false, isFirstChange: () => false },
    });
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('#password')).toBeNull();
    expect(el.querySelector('#status')).toBeTruthy();
    expect(el.querySelector('.modal-header h2')?.textContent?.trim()).toBe("Modifier l'utilisateur");
  });

  it('should populate form with user data in edit mode', () => {
    component.open = true;
    component.user = mockUser;
    component.ngOnChanges({
      open: { currentValue: true, previousValue: false, firstChange: false, isFirstChange: () => false },
    });
    fixture.detectChanges();

    expect(component.form.get('firstName')?.value).toBe('John');
    expect(component.form.get('lastName')?.value).toBe('Doe');
    expect(component.form.get('role')?.value).toBe('CUSTOMER');
    expect(component.form.get('status')?.value).toBe('ACTIVE');
  });

  it('should disable email field in edit mode', () => {
    component.open = true;
    component.user = mockUser;
    component.ngOnChanges({
      open: { currentValue: true, previousValue: false, firstChange: false, isFirstChange: () => false },
    });
    fixture.detectChanges();

    expect(component.form.get('email')?.disabled).toBeTrue();
  });

  it('should emit closed on close', () => {
    spyOn(component.closed, 'emit');
    component.close();
    expect(component.closed.emit).toHaveBeenCalled();
  });

  it('should emit saved with form data on submit', () => {
    component.open = true;
    component.user = null;
    component.ngOnChanges({
      open: { currentValue: true, previousValue: false, firstChange: false, isFirstChange: () => false },
    });

    component.form.patchValue({
      email: 'new@example.com',
      password: 'password123',
      firstName: 'Jane',
      lastName: 'Smith',
      role: 'ADMIN',
    });

    spyOn(component.saved, 'emit');
    component.onSubmit();

    expect(component.saved.emit).toHaveBeenCalledWith(jasmine.objectContaining({
      email: 'new@example.com',
      password: 'password123',
      firstName: 'Jane',
      lastName: 'Smith',
      role: 'ADMIN',
    }));
  });

  it('should not submit when form is invalid', () => {
    component.open = true;
    component.user = null;
    component.ngOnChanges({
      open: { currentValue: true, previousValue: false, firstChange: false, isFirstChange: () => false },
    });

    spyOn(component.saved, 'emit');
    component.onSubmit();

    expect(component.saved.emit).not.toHaveBeenCalled();
  });

  it('should close modal when clicking overlay background', () => {
    spyOn(component.closed, 'emit');

    const mockEvent = {
      target: { classList: { contains: (cls: string) => cls === 'modal-overlay' } },
    } as unknown as MouseEvent;

    component.onOverlayClick(mockEvent);
    expect(component.closed.emit).toHaveBeenCalled();
  });

  it('should not close modal when clicking inside card', () => {
    spyOn(component.closed, 'emit');

    const mockEvent = {
      target: { classList: { contains: () => false } },
    } as unknown as MouseEvent;

    component.onOverlayClick(mockEvent);
    expect(component.closed.emit).not.toHaveBeenCalled();
  });
});
