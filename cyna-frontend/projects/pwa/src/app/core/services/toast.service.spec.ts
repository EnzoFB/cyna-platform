import { TestBed } from '@angular/core/testing';
import { ToastService, ToastMessage } from './toast.service';

describe('ToastService', () => {
  let service: ToastService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ToastService);
  });

  it('showSuccess emits a success toast', (done) => {
    service.toast$.subscribe((msg: ToastMessage) => {
      expect(msg).toEqual({ type: 'success', message: 'Opération réussie' });
      done();
    });
    service.showSuccess('Opération réussie');
  });

  it('showError emits an error toast', (done) => {
    service.toast$.subscribe((msg: ToastMessage) => {
      expect(msg).toEqual({ type: 'error', message: 'Une erreur est survenue' });
      done();
    });
    service.showError('Une erreur est survenue');
  });

  it('showWarning emits a warning toast', (done) => {
    service.toast$.subscribe((msg: ToastMessage) => {
      expect(msg).toEqual({ type: 'warning', message: 'Attention' });
      done();
    });
    service.showWarning('Attention');
  });

  it('emits toasts in order when called sequentially', () => {
    const received: ToastMessage[] = [];
    service.toast$.subscribe(msg => received.push(msg));

    service.showSuccess('first');
    service.showError('second');
    service.showWarning('third');

    expect(received).toEqual([
      { type: 'success', message: 'first' },
      { type: 'error',   message: 'second' },
      { type: 'warning', message: 'third' },
    ]);
  });
});
