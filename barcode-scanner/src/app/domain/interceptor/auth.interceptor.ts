import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn): Observable<HttpEvent<unknown>> => {
  const token = localStorage.getItem('accessToken');
  if (!token) {
    return next(req);
  }
  const clonedReq = req.clone({
    headers: req.headers.set('Authorization', `Bearer ${token}`),
    withCredentials: true
  });
  return next(clonedReq);
};
