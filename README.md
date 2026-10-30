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
├─ docs/                             # Projekt-Dokumentation (README, TODO, Architektur, ...)
├─── requirements/                   # Was soll das System machen (User Stories, Use Cases, ...)
├─── architecture/                   # Architektur-Dokumentation (Architekturdiagramme, ...)
├─── data-model/                     # Datenmodell (ER-Diagramme, Tabellen, Entitäten, ...)
├─── business-rules/                 # Geschäftsregeln (z. B. Buchungsregeln, Preisberechnung, ...)
├─ planning/                         # Implementierungsplan (von Copilot generiert, basierend auf Anforderungen, Architektur und Projektanweisungen)
├─── roadmap.md                      # Roadmap (Meilensteine, Zeitplan, ...)
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
```

Unter Bash verwendest du `./gradlew` mit denselben Gradle-Tasks.
`validateAngularOpenApiOutput` prueft die erwarteten Angular-Services und Modelle;
`test` kompiliert auch die generierten Kotlin-Schnittstellen. Die Ausgaben liegen unter
`backend/build/generated/openapi/kotlin-spring/` und
`frontend/build/generated/openapi/typescript-angular/`. Es wird kein Angular-
Projekt angelegt; der Client wird bis zur Einrichtung des Frontends strukturell
validiert.

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