import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpEvent, HttpErrorResponse, HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, throwError, catchError, switchMap, tap, finalize } from 'rxjs';

export const tokenRefreshInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn): Observable<HttpEvent<unknown>> => {
  const http = inject(HttpClient);
  let isRefreshing = false;

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !isRefreshing) {
        isRefreshing = true;

        return http.post<{ accessToken: string; refreshToken: string | null }>(
          '/auth/refresh-token',
          {},
          { withCredentials: true }
        ).pipe(
          tap(res => localStorage.setItem('accessToken', res.accessToken)),
          switchMap(() => {
            const token = localStorage.getItem('accessToken');
            const clonedReq = req.clone({
              headers: req.headers.set('Authorization', `Bearer ${token}`),
              withCredentials: true
            });
            return next(clonedReq);
          }),
          finalize(() => {
            isRefreshing = false;
          })
        );
      }
      return throwError(() => error);
    })
  );
};
