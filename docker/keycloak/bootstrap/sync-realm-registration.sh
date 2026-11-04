#!/bin/bash
set -euo pipefail

realm="abfallcontainer-realm"
realm_config="/opt/keycloak/realm/abfallcontainer-realm-realm.json"
config_file="/tmp/keycloak-realm-sync.config"

if [[ -n "${KEYCLOAK_ADMIN_PASSWORD_FILE:-}" ]]; then
  if [[ ! -r "$KEYCLOAK_ADMIN_PASSWORD_FILE" ]]; then
    echo "Keycloak bootstrap administrator password file is not readable." >&2
    exit 1
  fi
  KEYCLOAK_ADMIN_PASSWORD="$(<"$KEYCLOAK_ADMIN_PASSWORD_FILE")"
fi

if [[ -z "${KEYCLOAK_ADMIN_USERNAME:-}" || -z "${KEYCLOAK_ADMIN_PASSWORD:-}" ]]; then
  echo "Keycloak bootstrap administrator credentials are required." >&2
  exit 1
fi

if [[ ! -r "$realm_config" ]]; then
  echo "Keycloak realm configuration is not readable: $realm_config" >&2
  exit 1
fi

registration_allowed="$(
  sed -nE 's/^[[:space:]]*"registrationAllowed"[[:space:]]*:[[:space:]]*(true|false),?[[:space:]]*$/\1/p' \
    "$realm_config"
)"
if [[ "$registration_allowed" != "true" && "$registration_allowed" != "false" ]]; then
  echo "The realm configuration must define exactly one boolean registrationAllowed value." >&2
  exit 1
fi

umask 077
trap 'rm -f "$config_file"' EXIT

ready=false
for attempt in $(seq 1 30); do
  if /opt/keycloak/bin/kcadm.sh config credentials \
    --server http://keycloak:8080 \
    --realm master \
    --user "$KEYCLOAK_ADMIN_USERNAME" \
    --password "$KEYCLOAK_ADMIN_PASSWORD" \
    --config "$config_file" >/dev/null 2>&1; then
    ready=true
    break
  fi
  sleep 2
done

if [[ "$ready" != true ]]; then
  echo "Keycloak did not become ready for realm registration settings synchronization." >&2
  exit 1
fi

/opt/keycloak/bin/kcadm.sh update "realms/$realm" \
  -s "registrationAllowed=$registration_allowed" \
  --config "$config_file" >/dev/null

echo "Keycloak realm $realm registrationAllowed synchronized to $registration_allowed."
