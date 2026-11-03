#!/bin/sh
set -eu

if [ -n "${KEYCLOAK_DB_PASSWORD_FILE:-}" ]; then
  KEYCLOAK_DB_PASSWORD="$(cat "$KEYCLOAK_DB_PASSWORD_FILE")"
fi

: "${KEYCLOAK_DB_PASSWORD:?KEYCLOAK_DB_PASSWORD or KEYCLOAK_DB_PASSWORD_FILE must be set}"

psql \
  --set=ON_ERROR_STOP=1 \
  --set=keycloak_db_user="$KEYCLOAK_DB_USER" \
  --set=keycloak_db_password="$KEYCLOAK_DB_PASSWORD" \
  --set=keycloak_db="$KEYCLOAK_DB" \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" <<'SQL'
CREATE ROLE :"keycloak_db_user" WITH LOGIN PASSWORD :'keycloak_db_password';
CREATE DATABASE :"keycloak_db" WITH OWNER :"keycloak_db_user";
SQL
