import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  Router,
  RouterStateSnapshot,
  provideRouter,
} from '@angular/router';
import { throwError } from 'rxjs';
import { CustomerProfilesService } from '../../../build/generated/openapi/typescript-angular/api/customerProfiles.service';
import { AuthService } from './auth.service';
import { authenticatedGuard, customerProfileGuard, roleGuard } from './auth.guards';

describe('roleGuard', () => {
  let allowed: boolean;
  const login = vi.fn().mockResolvedValue(undefined);

  beforeEach(() => {
    allowed = false;
    login.mockClear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            hasRole: () => allowed,
            isAuthenticated: () => false,
            login,
          },
        },
      ],
    });
  });

  it('starts login and denies an unauthenticated navigation until the redirect returns', async () => {
    const result = await TestBed.runInInjectionContext(() =>
      authenticatedGuard({} as ActivatedRouteSnapshot, { url: '/bookings' } as RouterStateSnapshot),
    );
    expect(result).toBe(false);
    expect(login).toHaveBeenCalledWith(expect.stringMatching(/\/bookings$/));
  });

  it('allows the required role', () => {
    allowed = true;
    const result = TestBed.runInInjectionContext(() =>
      roleGuard('ADMIN')({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );
    expect(result).toBe(true);
  });

  it('redirects accounts without the required role', () => {
    const result = TestBed.runInInjectionContext(() =>
      roleGuard('ADMIN')({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );
    expect(result).toEqual(TestBed.inject(Router).parseUrl('/forbidden'));
  });
});

describe('customerProfileGuard', () => {
  const getMyCustomerProfile = vi.fn();

  beforeEach(() => {
    getMyCustomerProfile.mockReset();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: CustomerProfilesService,
          useValue: { getMyCustomerProfile },
        },
      ],
    });
  });

  it('redirects users without a profile to profile completion and preserves their destination', async () => {
    getMyCustomerProfile.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
    const result = await TestBed.runInInjectionContext(() =>
      customerProfileGuard(
        {} as ActivatedRouteSnapshot,
        { url: '/bookings' } as RouterStateSnapshot,
      ),
    );

    expect(result).toEqual(
      TestBed.inject(Router).createUrlTree(['/profile'], {
        queryParams: { returnUrl: '/bookings' },
      }),
    );
  });
});
