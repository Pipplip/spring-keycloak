# spring-keycloak

Beispielanwendung für Spring Boot mit Keycloak (OpenID Connect) und PostgreSQL.  
Als Prompt-Übung wird AI Agents (Copilot) für die Implementierung verwendet.

## AI Agents

Struktur für AI Agent:

```
.
├─ .github/
├─── copilot-instructions.md         # Dauerhafte Regeln für Copilot
├─── prompts/
├─────── plan-feature.prompt.md      # Prompt für Copilot: Implementierungsplan erstellen
├─────── implement-feature.prompt.md # Prompt für Copilot: Implementierungsplan umsetzen
├─────── review-code.prompt.md       # Prompt für Copilot: Code review durchführen
├─ docs/                             # Projekt-Dokumentation (README, TODO, Architektur, ...)
├─── requirements.md                 # Was soll das System machen (User Stories, Use Cases, ...)
├─── architecture.md                 # Architektur-Dokumentation (Architekturdiagramme, ...)
├─── data-model.md                   # Datenmodell (ER-Diagramme, Tabellen, Entitäten, ...)
├─── business-rules.md               # Geschäftsregeln (z. B. Buchungsregeln, Preisberechnung, ...)
├─ planning/                         # Implementierungsplan (von Copilot generiert, basierend auf Anforderungen, Architektur und Projektanweisungen)
├─── roadmap.md                      # Roadmap (Meilensteine, Zeitplan, ...)
├─── backlog.md                      # Backlog (offene Aufgaben, Features, Bugs, ...)
├─────── plans/                      # Automatisch generierte Implementierungspläne (PLAN-001, PLAN-002, ...)
```

Entwicklungsprozess für Copilot:  
Erstelle einmalig die grundlegenden md Dateien (auch mithilfe von Copilot):  
- copilot-instructions.md
- requirements.md
- architecture.md 
- data-model.md 
- business-rules.md 
- roadmap.md

Führe dann diese Schritte aus:

```Prompt
Use the prompt plan-feature.prompt.md to create an implementation plan for the next milestone of the roadmap: Project Setup.
The implementation plan should consider Gradle, Kotlin, Spring Boot, PostgreSQL, Keycloak, and Docker Compose.
Do not implement any code yet.
```

Prüfe und ändere `PLAN-001-project-setup.md` und setze den Status auf `APPROVED` im Plan-Header.

Implementierung starten:
```Prompt
Use the prompt implement-feature.prompt.md to implement the approved implementation plan PLAN-001-project-setup.md.
```

Code Review durchführen:
```Prompt
Use the prompt review-code.prompt.md to perform a code review of the implementation of PLAN-001-project-setup.md.
```

Danach geht es weiter mit dem nächsten Meilenstein aus der `roadmap.md`

___

## Lokale Entwicklungsumgebung

Voraussetzungen: JDK 25, Docker mit Docker Compose v2 oder neuer und eine Shell.

1. Erstelle die lokale Konfiguration und ersetze die Platzhalterpasswörter:

   ```powershell
   Copy-Item .env.example .env
   ```

   Die Datei `.env` wird nicht versioniert. Für Bash kannst du `cp .env.example .env` verwenden.

2. Starte PostgreSQL und Keycloak aus dem Repository-Hauptverzeichnis:

   ```powershell
   docker compose up -d --wait
   docker compose ps
   ```

   PostgreSQL ist unter `localhost:5432` verfügbar; Keycloak unter
   `http://localhost:8081`. Keycloak verwendet eine eigene Datenbank im
   PostgreSQL-Service. Beim Start wird der lokale Realm
   `abfallcontainer-realm` mit den Clients und Rollen `USER` und `ADMIN`
   importiert.

3. Optional: Erstelle den lokalen Anwendungsadministrator mit dem separaten
   Passwort aus `.env`:

   ```powershell
   docker compose --profile keycloak-bootstrap run --rm keycloak-admin-init
   ```

   Das erstellt oder aktualisiert den in
   `ABFALLCONTAINER_ADMIN_USERNAME` angegebenen Benutzer im lokalen Realm und
   weist ihm die Rolle `ADMIN` zu. Die Keycloak-Master-Admin-Zugangsdaten und
   das Anwendungsadministrator-Passwort müssen lokal gesetzt sein; sie werden
   nicht versioniert.

4. Baue/teste das Backend oder starte den lokalen Spring-Boot-Server:

   ```powershell
   Set-Location backend
   .\gradlew.bat test
   .\gradlew.bat bootRun
   ```

   Unter Bash führe im Backend-Verzeichnis `./gradlew test` bzw.
   `./gradlew bootRun` aus. Die Spring-Konfiguration liest die Datenbankwerte
   aus der im Repository-Hauptverzeichnis liegenden `.env`. Der Resource Server
   erwartet den Issuer
   `http://localhost:8081/realms/abfallcontainer-realm` und die Audience
   `abfallcontainer-backend`. Diese Werte können mit
   `KEYCLOAK_ISSUER_URI` und `KEYCLOAK_API_AUDIENCE` überschrieben werden.
   Für einen Backend-Container bleibt der Issuer gleich; setze
   `KEYCLOAK_JWK_SET_URI` auf die interne URL
   `http://keycloak:8080/realms/abfallcontainer-realm/protocol/openid-connect/certs`,
   damit die JWKS im Compose-Netzwerk erreichbar ist.

   Für einen zusätzlichen Verbindungstest gegen die laufende Compose-Datenbank
   setze im Backend-Verzeichnis `COMPOSE_DATABASE_TEST=true` vor dem Testlauf:

   ```powershell
   $env:COMPOSE_DATABASE_TEST = "true"
   .\gradlew.bat test
   Remove-Item Env:\COMPOSE_DATABASE_TEST
   ```

   Unter Bash verwendest du `COMPOSE_DATABASE_TEST=true ./gradlew test`.

   Alternativ kannst du Backend und Angular-Entwicklungsserver zusammen mit
   PostgreSQL und Keycloak über das optionale Compose-Profil `app` starten:

   ```powershell
   docker compose --profile app up -d --wait
   ```

   Das Profil verwendet die lokalen Quellcode-Mounts und startet Gradle sowie
   `npm ci` in den Entwicklungscontainern. Der Backend-Service generiert den
   Angular-Client vor dem Start; das Frontend wartet auf diese Build-Ausgabe.
   Die Standardausführung `docker compose up` bleibt auf PostgreSQL und
   Keycloak beschränkt. Frontend und API sind dann unter `localhost:4200` und
   `localhost:8080` erreichbar.

5. Stoppe die Services, wenn du fertig bist:

   ```powershell
   docker compose down
   ```

   Die PostgreSQL-Daten bleiben im benannten Volume erhalten. Entferne das
   Volume nur, wenn du dessen lokale Daten ausdrücklich löschen möchtest.

## OpenAPI-Vertrag

`openapi/api.yaml` ist die Source of Truth fuer die API. Die Kotlin/Spring-
Schnittstellen und der Angular-TypeScript-Client werden generiert; generierte
Dateien duerfen nicht manuell bearbeitet werden.

Fuehre die folgenden Befehle im Backend-Verzeichnis aus:

```powershell
.\gradlew.bat openApiValidate
.\gradlew.bat generateKotlinOpenApi
.\gradlew.bat generateAngularOpenApi
.\gradlew.bat validateAngularOpenApiOutput
.\gradlew.bat test
.\gradlew.bat build
```

Unter Bash verwendest du `./gradlew` mit denselben Gradle-Tasks.
`validateAngularOpenApiOutput` prueft die erwarteten Angular-Services und Modelle;
`test` kompiliert auch die generierten Kotlin-Schnittstellen. Die Ausgaben liegen unter
`backend/build/generated/openapi/kotlin-spring/` und
`frontend/build/generated/openapi/typescript-angular/`. Die Angular-Anwendung
verwendet diesen Client direkt; generierte Dateien bleiben Build-Ausgabe und
werden nicht manuell bearbeitet. Die npm-Befehle `start`, `build` und `test`
führen die Clientgenerierung und -validierung automatisch über den Backend-
Gradle-Wrapper aus.

API-Fehler verwenden RFC-7807-Problem-Details mit dem Medientyp
`application/problem+json`. Unerwartete Serverfehler liefern einen generischen
500-Fehler; interne Ausnahme- und Datenbankdetails werden nicht an Clients
ausgegeben. Paginierte Endpunkte verwenden nullbasierte Seiten (`page >= 0`)
und Seitengroessen von 1 bis 100. Listen sind deterministisch sortiert, damit
benachbarte Seiten bei gleichen Zeitstempeln stabil bleiben.

## Angular-Frontend

Die SPA benötigt Node.js 22.23.4 und npm 10.9.9 (Versionen stehen in
`frontend/.nvmrc`, `frontend/.node-version` und `frontend/package.json`).
Angular-Abhängigkeiten sind exakt versioniert und `package-lock.json` ist
eingecheckt. Installiere und starte die Anwendung aus `frontend/`:

```powershell
Set-Location frontend
npm.cmd ci
npm.cmd start
```

Unter Bash verwende `cd frontend`, `npm ci` und `npm start`. Angular läuft
standardmäßig unter `http://localhost:4200`. Die Anwendung enthält Kunden-
und Admin-Oberflächen mit Standalone-Komponenten, lazy-loaded Routen, typed
reactive forms und Angular Material. Keycloak verwendet den öffentlichen
Client `abfallcontainer-frontend` mit Authorization Code Flow + PKCE; das
Frontend enthält kein Client-Geheimnis und legt Tokens nicht in Browser-
Webspeichern ab.

Lokale Konfiguration:

- API: `http://localhost:8080` (Spring Boot)
- Keycloak: `http://localhost:8081`, Realm `abfallcontainer-realm`
- erlaubter Browser-Origin für die API: `http://localhost:4200`

Die öffentlichen Frontend-Werte liegen in
`frontend/src/environments/environment.ts`; die Produktionsdatei enthält nur
secret-freie Build-Fallbacks. Das Frontend-Containerimage liest API- und
Keycloak-URLs zur Laufzeit aus `API_BASE_URL` und `KEYCLOAK_PUBLIC_URL`. Der
Backend-CORS-Origin kann mit `FRONTEND_ALLOWED_ORIGIN` gesetzt werden.
Standardmäßig sind nur
`http://localhost:4200` und die API-Pfade unter `/api/v1/**` erlaubt. Die API
bleibt die Autorisierungs- und Ownership-Grenze.

Führe die Frontend-Prüfungen aus:

```powershell
npm.cmd run lint
npm.cmd test
npm.cmd run build
```

`npm.cmd run test:e2e` startet Playwright und erwartet eine laufende lokale
PostgreSQL-/Keycloak-/Backend-Umgebung sowie einen installierten Chromium-
Browser (`npx.cmd playwright install chromium`). Der öffentliche Katalogtest
verifiziert die API-Verbindung. Für die authentifizierten Browser-Workflows
müssen die lokal vorbereiteten Keycloak-Testkonten und vorhandene Inventardaten
über Umgebungsvariablen bereitgestellt werden; sie werden nicht versioniert:

```powershell
$env:E2E_USER_USERNAME = "<test-user>"
$env:E2E_USER_PASSWORD = "<test-user-password>"
$env:E2E_ADMIN_USERNAME = "<test-admin>"
$env:E2E_ADMIN_PASSWORD = "<test-admin-password>"
npm.cmd run test:e2e
```

Ohne die jeweiligen Kontodaten weist Playwright diese Tests mit einem
konkreten Hinweis als übersprungen aus. Der USER benötigt ein lokal angelegtes
Kundenprofil; für die Buchungsreise muss außerdem für den Testzeitraum
Inventar verfügbar sein.

Compose startet standardmäßig nur PostgreSQL und Keycloak. Für den lokalen
Entwicklungsbetrieb sind Backend und Angular optional über
`docker compose --profile app up -d --wait` verfügbar. Produktionscontainer
und die imagebasierte Compose-Variante werden im folgenden Abschnitt
beschrieben.

## Imagebasierter Compose-Betrieb

Die Deployment-Compose-Datei baut und startet Backend und Frontend als
Produktionsimages. Sie überschreibt den Keycloak-Service für den
nicht-Development-Modus; ohne die zusätzliche Datei bleibt der bestehende
lokale Entwicklungsbetrieb unverändert.

1. Erstelle `.env` und die drei lokalen Secret-Dateien. Die Werte werden nicht
   eingecheckt:

   ```powershell
   Copy-Item .env.example .env
   New-Item -ItemType Directory -Force secrets
   Set-Content secrets/postgres_password.txt "<lokales-datenbankpasswort>"
   Set-Content secrets/keycloak_db_password.txt "<keycloak-datenbankpasswort>"
   Set-Content secrets/keycloak_admin_password.txt "<keycloak-adminpasswort>"
   ```

   `secrets/postgres_password.txt` muss zum Passwort der Anwendungsdatenbank
   passen; Compose reicht dieselbe Datei sicher an PostgreSQL und das Backend
   weiter. PostgreSQL und Keycloak sind in dieser lokalen Topologie nur an
   Loopback-Adressen gebunden. Verwende keine produktiven Zugangsdaten in
   lokalen Entwicklungsumgebungen.

2. Baue und starte den Stack:

   ```powershell
   docker compose -f compose.yaml -f compose.deployment.yaml --profile deployment up --build -d --wait
   docker compose -f compose.yaml -f compose.deployment.yaml --profile deployment ps
   ```

   Die Images können mit `docker compose ... build` lokal gebaut werden; CI
   baut sie ebenfalls, veröffentlicht sie aber nicht. Die Frontend-URLs für
   API und Keycloak werden beim Containerstart aus `API_BASE_URL` und
   `KEYCLOAK_PUBLIC_URL` in die öffentliche Runtime-Konfiguration geschrieben.
   Dadurch kann dasselbe Frontend-Image in unterschiedlichen Umgebungen
   verwendet werden, ohne URLs einzubauen oder Geheimnisse an den Browser
   auszuliefern.

3. Öffne `http://localhost:4200`. Die API ist unter
   `http://localhost:8080/api/v1` erreichbar; Swagger UI unter
   `http://localhost:8080/swagger-ui/index.html`. Swagger UI und die
   OpenAPI-Datei sind standardmäßig deaktiviert und werden nur durch das lokale
   Deployment-Profil freigeschaltet. Keycloak ist für den Browser unter
   `http://localhost:8081` erreichbar; das Backend verwendet für JWKS die
   interne Compose-Adresse und validiert weiterhin den öffentlichen Issuer
   `http://localhost:8081/realms/abfallcontainer-realm`.

   Prüfe Backend-Liveness und -Readiness mit
   `http://localhost:8080/actuator/health/liveness` und
   `http://localhost:8080/actuator/health/readiness`. Die Readiness bezieht den
   Zustand der Anwendungsdatenbank ein; Liveness hängt nicht von PostgreSQL
   ab. Die Antworten enthalten nur den Status. Die API-Authentifizierung und
   Rollen-/Ownership-Prüfungen bleiben unverändert.

   Diese Compose-Konfiguration stellt weder TLS noch einen öffentlichen
   Ingress oder eine Secret-Management-Lösung bereit. Sie bindet HTTP-Ports an
   `127.0.0.1` und ist für lokale Entwicklungs- und Testumgebungen vorgesehen.
   Für öffentliche Bereitstellung sind eine abgesicherte TLS-Terminierung,
   externe Zugriffskontrollen, geeignete Secret-Verwaltung und weitere
   Betriebsmaßnahmen erforderlich. Der importierte Keycloak-Client ist auf die
   lokale Frontend-URL konfiguriert.

4. Stoppe den Stack, ohne die PostgreSQL-Daten zu löschen:

   ```powershell
   docker compose -f compose.yaml -f compose.deployment.yaml --profile deployment down
   ```

   Verwende `down -v` nur, wenn du das lokale Datenbank-Volume ausdrücklich
   löschen möchtest.

## Datenbankmigrationen und Persistenztests

Flyway migriert ausschließlich die über `SPRING_DATASOURCE_URL` konfigurierte
Anwendungsdatenbank. Die Migrationen liegen unter
`backend/src/main/resources/db/migration/`; Hibernate prüft das Schema mit
`ddl-auto: validate` und erzeugt oder ändert keine Tabellen. Schemaänderungen
werden ausschließlich als neue, versionierte Migrationen ergänzt; angewendete
Migrationen bleiben unverändert.

Starte für lokale Entwicklung zunächst den PostgreSQL-Service wie oben
beschrieben. Flyway wendet die Migration beim Backend-Start automatisch an.
Die PostgreSQL-Integrationstests starten eine leere PostgreSQL-17-Testcontainer-
Datenbank und führen Migrationen und Persistenztests aus:

```powershell
Set-Location backend
.\gradlew.bat test
```

Unter Bash verwende `./gradlew test`. Für einen zusätzlichen Test gegen die
konfigurierte Compose-Datenbank gilt weiterhin der opt-in Schalter
`COMPOSE_DATABASE_TEST=true`; diese Tests verwenden die Anwendungsdatenbank,
nicht die separate Keycloak-Datenbank.