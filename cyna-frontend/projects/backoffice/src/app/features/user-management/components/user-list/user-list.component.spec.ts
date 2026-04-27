import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { UserListComponent } from './user-list.component';
import { UserService } from '../../../../core/services/user.service';

describe('UserListComponent', () => {
  let component: UserListComponent;
  let fixture: ComponentFixture<UserListComponent>;
  let userServiceSpy: jasmine.SpyObj<UserService>;

  const mockUsersResponse = {
    success: true,
    data: {
      items: [
        {
          id: 'abc12345-6789',
          email: 'georgia@example.com',
          firstName: 'Leslie',
          lastName: 'Alexander',
          phone: '+62 819 1314 1435',
          role: 'CUSTOMER',
          status: 'ACTIVE',
          totalPurchases: 21.78,
          orderCount: 30,
          address: '2972 Westheimer Rd. Santa Ana, Illinois 85486',
          createdAt: '2025-01-15T10:30:00Z',
        },
        {
          id: 'def12345-6789',
          email: 'guys@example.com',
          firstName: 'Guy',
          lastName: 'Hawkins',
          phone: '+62 819 1314 1435',
          role: 'CUSTOMER',
          status: 'ACTIVE',
          totalPurchases: 21.78,
          orderCount: 30,
          address: '4517 Washington Ave. Manchester, Kentucky 39495',
          createdAt: '2025-02-20T14:00:00Z',
        },
      ],
      page: 0,
      size: 10,
      totalElements: 13,
      totalPages: 2,
      first: true,
      last: false,
    },
    timestamp: '2025-03-20T08:00:00Z',
  };

  beforeEach(async () => {
    userServiceSpy = jasmine.createSpyObj('UserService', ['getUsers', 'createUser', 'updateUser', 'deleteUser']);
    userServiceSpy.getUsers.and.returnValue(of(mockUsersResponse));

    await TestBed.configureTestingModule({
      imports: [UserListComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: UserService, useValue: userServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UserListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should call userService.getUsers on init', () => {
    expect(userServiceSpy.getUsers).toHaveBeenCalledWith(0, 10);
  });

  it('should populate rowData after loading', () => {
    expect(component['rowData']()).toEqual(mockUsersResponse.data.items);
  });

  it('should set totalElements and totalPages', () => {
    expect(component['totalElements']()).toBe(13);
    expect(component['totalPages']()).toBe(2);
  });

  it('should render toolbar with search input', () => {
    const el: HTMLElement = fixture.nativeElement;
    const searchInput = el.querySelector('.toolbar__search input') as HTMLInputElement;
    expect(searchInput).toBeTruthy();
    expect(searchInput.placeholder).toContain('Search for id');
  });

  it('should render Filter and Export buttons', () => {
    const el: HTMLElement = fixture.nativeElement;
    const buttons = el.querySelectorAll('.btn-outline');
    expect(buttons.length).toBe(2);
    expect(buttons[0].textContent?.trim()).toContain('Filter');
    expect(buttons[1].textContent?.trim()).toContain('Export');
  });

  it('should render pagination controls', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.pagination')).toBeTruthy();
    expect(el.querySelector('.pagination__info')).toBeTruthy();
    expect(el.querySelectorAll('.pagination__btn').length).toBe(2);
  });

  it('should render the AG Grid element', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('ag-grid-angular')).toBeTruthy();
  });

  it('should compute correct range label', () => {
    expect(component['rangeLabel']()).toBe('1 - 10 of 2 Pages');
  });

  it('should navigate to next page', fakeAsync(() => {
    userServiceSpy.getUsers.calls.reset();
    component['nextPage']();
    tick();

    expect(component['currentPage']()).toBe(1);
    expect(userServiceSpy.getUsers).toHaveBeenCalledWith(1, 10);
  }));

  it('should not navigate before page 0', () => {
    userServiceSpy.getUsers.calls.reset();
    component['previousPage']();

    expect(component['currentPage']()).toBe(0);
    expect(userServiceSpy.getUsers).not.toHaveBeenCalled();
  });

  it('should handle API error gracefully', fakeAsync(() => {
    userServiceSpy.getUsers.and.returnValue(throwError(() => new Error('Network error')));
    component['loadUsers']();
    tick();

    expect(component['loading']()).toBeFalse();
  }));

  it('should update search query on input', () => {
    component['onSearchChange']('Leslie');
    expect(component['searchQuery']()).toBe('Leslie');
  });

  it('should copy user ID to clipboard on ID click', fakeAsync(() => {
    spyOn(navigator.clipboard, 'writeText').and.returnValue(Promise.resolve());

    const fakeEl = document.createElement('span');
    fakeEl.dataset['copyId'] = 'abc12345-6789';
    const event = new MouseEvent('click', { bubbles: true });
    Object.defineProperty(event, 'target', { value: fakeEl });

    component['onGridClick'](event);
    tick();

    expect(navigator.clipboard.writeText).toHaveBeenCalledWith('abc12345-6789');
    expect(component['copyToast']()).toContain('ID copié');
  }));

  it('should hide toast after timeout', fakeAsync(() => {
    spyOn(navigator.clipboard, 'writeText').and.returnValue(Promise.resolve());

    const fakeEl = document.createElement('span');
    fakeEl.dataset['copyId'] = 'abc12345-6789';
    const event = new MouseEvent('click', { bubbles: true });
    Object.defineProperty(event, 'target', { value: fakeEl });

    component['onGridClick'](event);
    tick();

    expect(component['copyToast']()).toBeTruthy();
    tick(2500);
    expect(component['copyToast']()).toBeNull();
  }));

  it('should not copy when clicking a non-ID element', () => {
    spyOn(navigator.clipboard, 'writeText');

    const fakeEl = document.createElement('span');
    const event = new MouseEvent('click', { bubbles: true });
    Object.defineProperty(event, 'target', { value: fakeEl });

    component['onGridClick'](event);

    expect(navigator.clipboard.writeText).not.toHaveBeenCalled();
    expect(component['copyToast']()).toBeNull();
  });

  // ── Modal tests ─────────────────────────────────────────────────────────────

  it('should render the Nouveau button', () => {
    const el: HTMLElement = fixture.nativeElement;
    const btn = el.querySelector('.btn-primary');
    expect(btn).toBeTruthy();
    expect(btn?.textContent?.trim()).toContain('Nouveau');
  });

  it('should open create modal on Nouveau click', () => {
    component['openCreateModal']();
    expect(component['modalOpen']()).toBeTrue();
    expect(component['editingUser']()).toBeNull();
  });

  it('should open edit modal with user data', () => {
    const user = mockUsersResponse.data.items[0];
    component['openEditModal'](user as any);
    expect(component['modalOpen']()).toBeTrue();
    expect(component['editingUser']()).toEqual(user as any);
  });

  it('should close modal and reset editing user', () => {
    component['modalOpen'].set(true);
    component['editingUser'].set(mockUsersResponse.data.items[0] as any);
    component['closeModal']();
    expect(component['modalOpen']()).toBeFalse();
    expect(component['editingUser']()).toBeNull();
  });

  it('should call createUser on modal save in create mode', fakeAsync(() => {
    userServiceSpy.createUser.and.returnValue(of({ success: true, data: { id: 'new-id' }, timestamp: '' }));
    component['editingUser'].set(null);

    component['onModalSave']({
      email: 'new@example.com',
      password: 'password123',
      firstName: 'New',
      lastName: 'User',
      role: 'CUSTOMER',
    });
    tick();

    expect(userServiceSpy.createUser).toHaveBeenCalledWith({
      email: 'new@example.com',
      password: 'password123',
      firstName: 'New',
      lastName: 'User',
      role: 'CUSTOMER',
    });
  }));

  it('should call updateUser on modal save in edit mode', fakeAsync(() => {
    userServiceSpy.updateUser.and.returnValue(of({ success: true, data: undefined as any, timestamp: '' }));
    const user = mockUsersResponse.data.items[0] as any;
    component['editingUser'].set(user);

    component['onModalSave']({
      email: user.email,
      firstName: 'Updated',
      lastName: 'Name',
      role: 'ADMIN',
      status: 'ACTIVE',
    });
    tick();

    expect(userServiceSpy.updateUser).toHaveBeenCalledWith(user.id, {
      firstName: 'Updated',
      lastName: 'Name',
      role: 'ADMIN',
      status: 'ACTIVE',
    });
  }));

  it('should call deleteUser on delete action with confirm', fakeAsync(() => {
    spyOn(window, 'confirm').and.returnValue(true);
    userServiceSpy.deleteUser.and.returnValue(of({ success: true, data: undefined as any, timestamp: '' }));

    const user = mockUsersResponse.data.items[0] as any;
    component['deleteUser'](user);
    tick();

    expect(userServiceSpy.deleteUser).toHaveBeenCalledWith(user.id);
  }));

  it('should not delete when confirm is cancelled', () => {
    spyOn(window, 'confirm').and.returnValue(false);

    const user = mockUsersResponse.data.items[0] as any;
    component['deleteUser'](user);

    expect(userServiceSpy.deleteUser).not.toHaveBeenCalled();
  });

  it('should show success toast after create', fakeAsync(() => {
    userServiceSpy.createUser.and.returnValue(of({ success: true, data: { id: 'new-id' }, timestamp: '' }));
    component['editingUser'].set(null);

    component['onModalSave']({
      email: 'new@example.com',
      password: 'password123',
      firstName: 'New',
      lastName: 'User',
      role: 'CUSTOMER',
    });
    tick();

    expect(component['toast']()).toEqual({ message: 'Utilisateur créé avec succès', type: 'success' });
    tick(3000);
    expect(component['toast']()).toBeNull();
  }));

  it('should show error toast on create failure', fakeAsync(() => {
    userServiceSpy.createUser.and.returnValue(throwError(() => new Error('fail')));
    component['editingUser'].set(null);

    component['onModalSave']({
      email: 'new@example.com',
      password: 'password123',
      firstName: 'New',
      lastName: 'User',
      role: 'CUSTOMER',
    });
    tick();

    expect(component['toast']()).toEqual({ message: 'Erreur lors de la création', type: 'error' });
    tick(3000);
  }));

  it('should handle edit action from grid click', () => {
    const actionBtn = document.createElement('button');
    actionBtn.dataset['action'] = 'edit';
    actionBtn.dataset['userId'] = mockUsersResponse.data.items[0].id;

    const event = new MouseEvent('click', { bubbles: true });
    Object.defineProperty(event, 'target', { value: actionBtn });

    spyOn(component as any, 'openEditModal');
    component['onGridClick'](event);

    expect((component as any).openEditModal).toHaveBeenCalledWith(mockUsersResponse.data.items[0] as any);
  });

  it('should handle delete action from grid click', () => {
    const actionBtn = document.createElement('button');
    actionBtn.dataset['action'] = 'delete';
    actionBtn.dataset['userId'] = mockUsersResponse.data.items[0].id;

    const event = new MouseEvent('click', { bubbles: true });
    Object.defineProperty(event, 'target', { value: actionBtn });

    spyOn(component as any, 'deleteUser');
    component['onGridClick'](event);

    expect((component as any).deleteUser).toHaveBeenCalledWith(mockUsersResponse.data.items[0] as any);
  });

  it('should render the user-form-modal component', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('app-user-form-modal')).toBeTruthy();
  });
});
