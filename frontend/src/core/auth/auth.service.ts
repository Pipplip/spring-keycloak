import { Injectable, signal } from '@angular/core';
import Keycloak, { KeycloakInstance } from 'keycloak-js';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly keycloak: KeycloakInstance = new Keycloak({
    url: environment.keycloakUrl,
    realm: environment.keycloakRealm,
    clientId: environment.keycloakClientId,
  });

  private readonly initializedState = signal(false);
  private readonly authenticatedState = signal(false);
  private readonly rolesState = signal<string[]>([]);
  private readonly initializationErrorState = signal('');

  readonly initialized = this.initializedState.asReadonly();
  readonly isAuthenticated = this.authenticatedState.asReadonly();
  readonly roles = this.rolesState.asReadonly();
  readonly initializationError = this.initializationErrorState.asReadonly();

  async initialize(): Promise<void> {
    try {
      const authenticated = await this.keycloak.init({
        onLoad: 'check-sso',
        pkceMethod: 'S256',
        checkLoginIframe: false,
      });
      this.authenticatedState.set(authenticated);
      this.rolesState.set(this.keycloak.tokenParsed?.realm_access?.roles ?? []);
    } catch {
      this.initializationErrorState.set(
        'The identity service could not be reached. Sign-in may be unavailable until it is running.',
      );
    } finally {
      this.initializedState.set(true);
    }
  }

  hasRole(role: 'USER' | 'ADMIN'): boolean {
    return this.rolesState().includes(role);
  }

  token(): string | undefined {
    return this.keycloak.token;
  }

  async login(redirectUri = window.location.href): Promise<void> {
    try {
      await this.keycloak.login({ redirectUri });
      this.initializationErrorState.set('');
    } catch {
      this.initializationErrorState.set(
        'Sign-in could not be started. Confirm the identity service is available and try again.',
      );
    }
  }

  async logout(): Promise<void> {
    this.authenticatedState.set(false);
    this.rolesState.set([]);
    try {
      await this.keycloak.logout({ redirectUri: window.location.origin });
    } catch {
      this.initializationErrorState.set(
        'Sign-out could not be completed. Close this browser session and try again.',
      );
    }
  }

  async refreshToken(): Promise<void> {
    if (this.authenticatedState()) {
      await this.keycloak.updateToken(30);
      this.rolesState.set(this.keycloak.tokenParsed?.realm_access?.roles ?? []);
    }
  }
}
