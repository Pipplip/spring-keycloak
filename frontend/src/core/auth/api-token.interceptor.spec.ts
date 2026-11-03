import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { apiTokenInterceptor } from './api-token.interceptor';
import { AuthService } from './auth.service';

describe('apiTokenInterceptor', () => {
  let http: HttpTestingController;
  let client: HttpClient;
  const auth = {
    isAuthenticated: signal(true),
    token: () => 'test-access-token',
    refreshToken: vi.fn().mockResolvedValue(undefined),
  };

  beforeEach(() => {
    auth.refreshToken.mockClear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiTokenInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: auth },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    client = TestBed.inject(HttpClient);
  });

  afterEach(() => http.verify());

  it('attaches a refreshed token only to requests for the configured backend origin', async () => {
    const apiResponse = client.get('http://localhost:8080/api/v1/bookings').subscribe();
    await new Promise((resolve) => setTimeout(resolve, 0));
    const apiRequest = http.expectOne('http://localhost:8080/api/v1/bookings');
    expect(apiRequest.request.headers.get('Authorization')).toBe('Bearer test-access-token');
    apiRequest.flush({});
    apiResponse.unsubscribe();

    const thirdPartyResponse = client.get('https://unrelated.example.test/resource').subscribe();
    const thirdPartyRequest = http.expectOne('https://unrelated.example.test/resource');
    expect(thirdPartyRequest.request.headers.has('Authorization')).toBe(false);
    thirdPartyRequest.flush({});
    thirdPartyResponse.unsubscribe();
    expect(auth.refreshToken).toHaveBeenCalledTimes(1);
  });
});
