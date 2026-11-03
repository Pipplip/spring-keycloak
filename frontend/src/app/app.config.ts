import { ApplicationConfig, inject, provideAppInitializer } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { Configuration } from '../../build/generated/openapi/typescript-angular/configuration';
import { BASE_PATH } from '../../build/generated/openapi/typescript-angular/variables';
import { environment } from '../environments/environment';
import { apiTokenInterceptor } from '../core/auth/api-token.interceptor';
import { AuthService } from '../core/auth/auth.service';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([apiTokenInterceptor])),
    { provide: BASE_PATH, useValue: environment.apiBaseUrl },
    {
      provide: Configuration,
      useFactory: () => new Configuration({ basePath: environment.apiBaseUrl }),
    },
    provideAppInitializer(() => inject(AuthService).initialize()),
  ],
};
