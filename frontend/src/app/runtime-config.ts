import { environment } from '../environments/environment';

interface PublicRuntimeConfig {
  apiBaseUrl: string;
  keycloakUrl: string;
}

declare global {
  interface Window {
    __ABFALLCONTAINER_CONFIG__?: Partial<PublicRuntimeConfig>;
  }
}

export function getRuntimeConfig(): PublicRuntimeConfig {
  const runtimeConfig = window.__ABFALLCONTAINER_CONFIG__;
  return {
    apiBaseUrl: runtimeConfig?.apiBaseUrl || environment.apiBaseUrl,
    keycloakUrl: runtimeConfig?.keycloakUrl || environment.keycloakUrl,
  };
}
