#!/bin/bash
set -euo pipefail

realm="abfallcontainer-realm"
config_file="/tmp/keycloak-admin-init.config"

if [[ -z "${KEYCLOAK_ADMIN_USERNAME:-}" || -z "${KEYCLOAK_ADMIN_PASSWORD:-}" ]]; then
  echo "Keycloak bootstrap administrator credentials are required." >&2
  exit 1
fi

if [[ -z "${ABFALLCONTAINER_ADMIN_USERNAME:-}" || -z "${ABFALLCONTAINER_ADMIN_PASSWORD:-}" ]]; then
  echo "Application administrator credentials are required." >&2
  exit 1
fi

ready=false
for attempt in $(seq 1 60); do
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
  echo "Keycloak did not become ready for local administrator setup." >&2
  exit 1
fi

users=$(/opt/keycloak/bin/kcadm.sh get users \
  -r "$realm" \
  -q "username=$ABFALLCONTAINER_ADMIN_USERNAME" \
  --config "$config_file")

if ! grep -Fq "\"username\" : \"$ABFALLCONTAINER_ADMIN_USERNAME\"" <<<"$users"; then
  /opt/keycloak/bin/kcadm.sh create users \
    -r "$realm" \
    -s "username=$ABFALLCONTAINER_ADMIN_USERNAME" \
    -s enabled=true \
    --config "$config_file" >/dev/null
fi

/opt/keycloak/bin/kcadm.sh set-password \
  -r "$realm" \
  --username "$ABFALLCONTAINER_ADMIN_USERNAME" \
  --new-password "$ABFALLCONTAINER_ADMIN_PASSWORD" \
  --config "$config_file" >/dev/null

/opt/keycloak/bin/kcadm.sh add-roles \
  -r "$realm" \
  --uusername "$ABFALLCONTAINER_ADMIN_USERNAME" \
  --rolename ADMIN \
  --config "$config_file"

echo "Local application administrator configured in realm $realm."
