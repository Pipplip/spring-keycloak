#!/bin/sh
set -eu

escape_json() {
    printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'
}

case "$API_BASE_URL" in
    http://*|https://*) ;;
    *) echo "API_BASE_URL must use http or https." >&2; exit 1 ;;
esac
case "$KEYCLOAK_URL" in
    http://*|https://*) ;;
    *) echo "KEYCLOAK_URL must use http or https." >&2; exit 1 ;;
esac

api_base_url="$(escape_json "$API_BASE_URL")"
keycloak_url="$(escape_json "$KEYCLOAK_URL")"
printf 'window.__ABFALLCONTAINER_CONFIG__ = {"apiBaseUrl":"%s","keycloakUrl":"%s"};\n' \
    "$api_base_url" "$keycloak_url" > /usr/share/nginx/html/runtime-config.js
