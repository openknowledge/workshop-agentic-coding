# Workshop Agentic Coding

Herzlich Willkommen im Workshop Agentic Coding.
Im Workshop werden wir eine Kundenverwaltung als Beispielanwendung nutzen.

## Customer Management Application

Customer Management ist ein Beispielprojekt zur Verwaltung von Kunden. Es besteht aus zwei Maven-Modulen:

- **customer-management-server** – Backend auf Basis von Spring Boot (Java 21), das die REST-API sowie die Datenhaltung (PostgreSQL, Flyway-Migrationen) bereitstellt.
- **customer-management-client** – Frontend auf Basis von React/TypeScript/Vite, das über den Frontend-Maven-Plugin in den Maven-Build eingebunden ist.

Der Server bindet das gebaute Frontend ein und liefert die gesamte Anwendung als ein einziges, ausführbares Artefakt aus.

### Voraussetzungen

- Java 21
- Docker und Docker Compose (für den Container-Build/-Start)

### Build mit Maven

Im Projekt-Root befindet sich ein Maven-Wrapper, sodass keine lokale Maven-Installation notwendig ist.

Gesamtes Projekt bauen (Client und Server):

```bash
./mvnw clean install
```

### Gesamter Stack mit Docker Compose

Da der Server zur Laufzeit eine PostgreSQL-Datenbank sowie einen WireMock-Server als Abhängigkeit benötigt, empfiehlt sich der Start über Docker Compose. Dieser startet Datenbank, Flyway-Migrationen, WireMock und den Server gemeinsam:

```bash
docker compose up --build
```

Die Anwendung ist danach unter `http://localhost:8080` erreichbar.

### Anwendung lokal starten

Zunächst Datenbank und Wiremock-Server via Docker Compose starten:

```bash
docker compose up --build customer-management-database flyway wiremock
```

Den Server anschließend lokal starten:

```bash
./mvnw -pl customer-management-server spring-boot:run
```

Das Frontend kann für die lokale Entwicklung mit Hot-Reload separat gestartet werden:

```bash
cd customer-management-client
npm install
npm run dev
```


