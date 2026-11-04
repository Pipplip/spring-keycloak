import { inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { CanActivateFn, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthService } from './auth.service';
import { CustomerProfilesService } from '../../../build/generated/openapi/typescript-angular/api/customerProfiles.service';

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

export const customerProfileGuard: CanActivateFn = async (_route, state) => {
  const profilesApi = inject(CustomerProfilesService);
  const router = inject(Router);
  try {
    await firstValueFrom(profilesApi.getMyCustomerProfile());
    return true;
  } catch (error: unknown) {
    if (error instanceof HttpErrorResponse) {
      return router.createUrlTree(['/profile'], { queryParams: { returnUrl: state.url } });
    }
    throw error;
  }
};
