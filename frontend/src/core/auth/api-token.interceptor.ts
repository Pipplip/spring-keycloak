import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { from, switchMap } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';

const apiOrigin = new URL(environment.apiBaseUrl).origin;

export const apiTokenInterceptor: HttpInterceptorFn = (request, next) => {
  let requestOrigin: string;
  try {
    requestOrigin = new URL(request.url, window.location.origin).origin;
  } catch {
    return next(request);
  }

  if (requestOrigin !== apiOrigin) {
    return next(request);
  }

  const auth = inject(AuthService);
  return from(auth.refreshToken()).pipe(
    switchMap(() => {
      const token = auth.isAuthenticated() ? auth.token() : undefined;
      return next(
        token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request,
      );
    }),
  );
};
