import { getRuntimeConfig } from './runtime-config';

describe('getRuntimeConfig', () => {
  afterEach(() => {
    delete window.__ABFALLCONTAINER_CONFIG__;
  });

  it('uses the public runtime values when provided', () => {
    window.__ABFALLCONTAINER_CONFIG__ = {
      apiBaseUrl: 'https://api.example.test',
      keycloakUrl: 'https://identity.example.test',
    };

    expect(getRuntimeConfig()).toEqual({
      apiBaseUrl: 'https://api.example.test',
      keycloakUrl: 'https://identity.example.test',
    });
  });
});
