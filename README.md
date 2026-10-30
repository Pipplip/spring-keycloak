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
Use the prompt plan-feature.prompt.md to create an implementation plan for the first milestone of the roadmap: Project Setup.
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
   PostgreSQL-Service. Es wird kein Realm oder Client vorab importiert.

3. Baue/teste das Backend oder starte den lokalen Spring-Boot-Server:

   ```powershell
   Set-Location backend
   .\gradlew.bat test
   .\gradlew.bat bootRun
   ```

   Unter Bash führe im Backend-Verzeichnis `./gradlew test` bzw.
   `./gradlew bootRun` aus. Die Spring-Konfiguration liest die Datenbankwerte
   aus der im Repository-Hauptverzeichnis liegenden `.env`.

   Für einen zusätzlichen Verbindungstest gegen die laufende Compose-Datenbank
   setze im Backend-Verzeichnis `COMPOSE_DATABASE_TEST=true` vor dem Testlauf:

   ```powershell
   $env:COMPOSE_DATABASE_TEST = "true"
   .\gradlew.bat test
   Remove-Item Env:\COMPOSE_DATABASE_TEST
   ```

   Unter Bash verwendest du `COMPOSE_DATABASE_TEST=true ./gradlew test`.

4. Stoppe die Services, wenn du fertig bist:

   ```powershell
   docker compose down
   ```

   Die PostgreSQL-Daten bleiben im benannten Volume erhalten. Entferne das
   Volume nur, wenn du dessen lokale Daten ausdrücklich löschen möchtest.