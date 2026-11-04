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

Voraussetzungen: Docker mit Docker Compose v2 oder neuer. Für das Ausführen
der Container werden JDK und Node.js auf dem Host nicht benötigt.

### Empfohlener Start: kompletter Entwicklungsstack

1. Erstelle im Projektverzeichnis `.env` und ersetze dort alle
   `replace-with-a-local-password`-Platzhalter durch lokale Passwörter:

   ```powershell
   Copy-Item .env.example .env
   ```

   Unter Bash/macOS/Linux: `cp .env.example .env`. Die Datei `.env` ist
   ignoriert und wird nicht versioniert.

2. Starte den vollständigen Entwicklungsstack mit einem Compose-Befehl:

   ```powershell
   docker compose --profile app --profile keycloak-bootstrap up -d --wait
   docker compose --profile app --profile keycloak-bootstrap ps
   ```

   Beim ersten Start werden Images geladen, der API-Client generiert und
   Frontend-Abhängigkeiten installiert. Das kann einige Minuten dauern.

3. Rufe die Anwendung auf:

   - Frontend: <http://localhost:4200>
   - API: <http://localhost:8080/api/v1>
   - Keycloak: <http://localhost:8081>

   Keycloak importiert beim Start den Realm `abfallcontainer-realm` mit den
   Clients und Rollen `USER` und `ADMIN`. Anschließend synchronisiert ein
   einmaliger Compose-Schritt die Einstellung `registrationAllowed` aus der
   Realm-Datei auch in bereits vorhandene Keycloak-Daten, ohne den Realm oder
   seine Benutzer zu überschreiben. Der lokale Anwendungsadministrator
   wird beim Compose-Start automatisch angelegt. Benutzername und Passwort
   stehen in `.env` unter `ABFALLCONTAINER_ADMIN_USERNAME` und
   `ABFALLCONTAINER_ADMIN_PASSWORD`; sie sind unabhängig von den Zugangsdaten
   für die Keycloak-Administration.

   Kunden können über **Register** die von Keycloak gehostete Registrierung
   starten. Neue Konten erhalten automatisch die Rolle `USER`; eine bestätigte
   E-Mail-Adresse ist für die Erstellung des lokalen Kundenprofils nicht
   erforderlich. Beim ersten Aufruf eines Kunden-Buchungsablaufs werden
   Kunden ohne lokales Profil zur Profilvervollständigung weitergeleitet. Name
   und E-Mail kommen aus dem Keycloak-Token; Firmenname und Rechnungsadresse
   werden im Kundenprofil verwaltet. Spätere
   Änderungen von Name oder E-Mail in Keycloak werden nicht automatisch in das
   Profil übernommen.

4. Zeige bei Bedarf die Logs an:

   ```powershell
   docker compose --profile app --profile keycloak-bootstrap logs -f backend frontend
   ```

   Beende die Loganzeige mit `Ctrl+C`. Stoppe danach die Umgebung:

   ```powershell
   docker compose down
   ```

   `docker compose down` behält die Daten im PostgreSQL-Volume. Nur wenn du
   die lokalen Daten löschen willst, führe `docker compose down -v` aus.

### Backend und Frontend lokal starten

Wenn Backend und Angular nicht in Entwicklungscontainern laufen sollen,
starte zunächst `.env` wie oben beschrieben und dann nur PostgreSQL und
Keycloak:

```powershell
docker compose --profile keycloak-bootstrap up -d --wait postgres keycloak keycloak-admin-init
```

Benötigt werden JDK 25 sowie Node.js 22.23.4 und npm 10.9.9. Starte Backend und
Frontend in separaten Terminals aus dem Projektverzeichnis:

```powershell
Set-Location backend
.\gradlew.bat bootRun
```

```powershell
Set-Location frontend
npm.cmd ci
npm.cmd start
```

Unter Bash/macOS/Linux verwende `./gradlew bootRun` im Verzeichnis `backend`
sowie `npm ci` und `npm start` in `frontend`. Die npm-Befehle generieren und
validieren den Angular-API-Client automatisch.

## Gebaute Images mit Docker Compose starten

Das Profil `deployment` baut die Backend- und Frontend-Images und startet den
vollständigen Stack. Vor dem Start müssen `.env` sowie die drei Secret-Dateien
existieren. Erstelle sie im Projektverzeichnis und ersetze die Beispielwerte
durch lokale Passwörter:

```powershell
Copy-Item .env.example .env
New-Item -ItemType Directory -Force secrets | Out-Null
Set-Content -NoNewline -Encoding ascii secrets/postgres_password.txt "<lokales-passwort>"
Set-Content -NoNewline -Encoding ascii secrets/keycloak_db_password.txt "<keycloak-db-passwort>"
Set-Content -NoNewline -Encoding ascii secrets/keycloak_admin_password.txt "<keycloak-adminpasswort>"
```

Die Secret-Dateien werden nicht eingecheckt. Sie liefern die Laufzeitpasswörter
für PostgreSQL, das Backend und Keycloak. Die passwortbezogenen Variablen in
`.env` müssen belegt sein, weil die Basis-Compose-Datei sie verlangt; bei
diesem Profil kommen die Laufzeitpasswörter jedoch aus den Secret-Dateien.

Baue und starte die Container:

```powershell
docker compose --env-file .env -f compose.yaml -f compose.deployment.yaml --profile deployment up --build -d --wait
docker compose --env-file .env -f compose.yaml -f compose.deployment.yaml --profile deployment ps
```

Frontend, API und Keycloak sind unter den oben genannten Adressen erreichbar.
Zusätzliche Endpunkte:

- Swagger UI: <http://localhost:8080/swagger-ui/index.html>
- Liveness: <http://localhost:8080/actuator/health/liveness>
- Readiness: <http://localhost:8080/actuator/health/readiness>

Swagger UI und OpenAPI-Datei sind nur in diesem lokalen Deployment-Profil
aktiviert. Die Readiness berücksichtigt PostgreSQL; die Liveness nicht. Die
Compose-Datei bindet HTTP-Ports an `127.0.0.1` und konfiguriert weder TLS noch
öffentlichen Ingress oder externe Secret-Verwaltung. Sie ist kein
Produktions-Deployment für das öffentliche Internet.

Stoppe den Stack, ohne die Datenbank zu löschen:

```powershell
docker compose --env-file .env -f compose.yaml -f compose.deployment.yaml --profile deployment down
```

Mit `down -v` werden auch die lokalen Compose-Volumes gelöscht. Beende einen
laufenden Stack, bevor du zwischen `app`- und `deployment`-Profil wechselst.

## Tests

Backend-Tests und Build aus dem Verzeichnis `backend`:

```powershell
.\gradlew.bat openApiValidate test build
```

Frontend-Prüfungen aus dem Verzeichnis `frontend`:

```powershell
npm.cmd ci
npm.cmd run lint
npm.cmd test
npm.cmd run build
```

Unter Bash/macOS/Linux verwende `./gradlew` beziehungsweise `npm`. Die
Backend-Integrationstests benötigen Docker für Testcontainers.

Für Playwright-End-to-End-Tests installiere Chromium mit
`npx.cmd playwright install chromium` und starte zuvor die lokale Umgebung.
Authentifizierte Tests benötigen lokale Keycloak-Testkonten sowie passende
Inventardaten. Übergib die Zugangsdaten über `E2E_USER_USERNAME`,
`E2E_USER_PASSWORD`, `E2E_ADMIN_USERNAME` und `E2E_ADMIN_PASSWORD`.
Der optionale Keycloak-Registrierungsablauf benötigt außerdem
`E2E_REGISTRATION_PASSWORD`. Lege Zugangsdaten nicht in versionierten Dateien
ab.

## API und Datenbank

`openapi/api.yaml` ist die einzige Quelle des API-Vertrags. Kotlin- und
Angular-Clients werden generiert; generierte Dateien sind Build-Ausgaben und
dürfen nicht manuell bearbeitet werden. API-Fehler verwenden RFC-7807-
Problem-Details. Datenbankschemaänderungen erfolgen über neue Flyway-
Migrationen; angewendete Migrationen werden nicht verändert. Flyway migriert
ausschließlich die Anwendungsdatenbank. Keycloak verwendet eine separate
PostgreSQL-Datenbank.