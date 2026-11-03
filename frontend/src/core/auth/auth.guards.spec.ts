import {
  ActivatedRouteSnapshot,
  Router,
  RouterStateSnapshot,
  provideRouter,
} from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';
import { authenticatedGuard, roleGuard } from './auth.guards';

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
