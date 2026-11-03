import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const authenticatedGuard: CanActivateFn = async (_route, state) => {
  const auth = inject(AuthService);
  if (auth.isAuthenticated()) {
    return true;
  }
  await auth.login(`${window.location.origin}${state.url}`);
  return false;
};

export const roleGuard =
  (role: 'USER' | 'ADMIN'): CanActivateFn =>
  () => {
    const auth = inject(AuthService);
    return auth.hasRole(role) || inject(Router).parseUrl('/forbidden');
  };
