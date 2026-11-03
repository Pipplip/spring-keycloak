#!/bin/sh
set -eu

if [ -r /run/secrets/app_db_password ]; then
    SPRING_DATASOURCE_PASSWORD="$(cat /run/secrets/app_db_password)"
    export SPRING_DATASOURCE_PASSWORD
fi

exec java -XX:MaxRAMPercentage=75.0 -jar /app/app.jar
