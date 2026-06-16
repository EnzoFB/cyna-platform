import { HttpInterceptorFn } from '@angular/common/http';

const CORRELATION_ID_KEY = 'x-correlation-id';
const CORRELATION_ID_HEADER = 'X-Correlation-Id';

export const correlationIdInterceptor: HttpInterceptorFn = (req, next) => {
  let id = sessionStorage.getItem(CORRELATION_ID_KEY);
  if (!id) {
    id = crypto.randomUUID();
    sessionStorage.setItem(CORRELATION_ID_KEY, id);
  }
  const cloned = req.clone({
    setHeaders: { [CORRELATION_ID_HEADER]: id },
  });
  return next(cloned);
};
