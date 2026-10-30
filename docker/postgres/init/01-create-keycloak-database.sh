#!/bin/sh
set -eu

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
