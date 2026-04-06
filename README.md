# LSW Online — Multi-Module Game System

This repository is a Maven multi-module Spring Boot project that provides services for the Legends of Sword and Wand (LSW) online system.

## Quick Overview

- Parent project: [pom.xml](pom.xml#L1)
- Modules: `lsw-core`, `lsw-user-service`, `lsw-party-service`, `lsw-campaign-service`, `lsw-battle-service`, `lsw-pvp-service`, `lsw-gateway`, `lsw-client`
- Top-level helper: `docker-compose.yml`

## Tech Stack

- Java: 21 (configured in the parent POM)
- Spring Boot: 3.2.3 (managed by the parent POM)
- Build: Maven multi-module
- Testing: JUnit / Spring Boot Test
- Containerization: Docker

## Project Structure

```
lsw-online/
├── .github/
│   └── workflows/ci-cd.yml
├── docker-compose.yml
├── pom.xml                    # Parent POM (module list + dependency management)
├── lsw-core/
│   ├── pom.xml
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/org/example/...
│   │   │   └── resources/org/...
│   │   └── test/
│   └── target/
│       ├── classes/
│       ├── generated-sources/
│       ├── maven-status/
│       └── test-classes/
├── lsw-user-service/
│   ├── Dockerfile
│   ├── pom.xml
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/org/...
│   │   │   └── resources/application.yml
│   │   └── test/
│   └── target/
├── lsw-party-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/java/org/...
│       └── main/resources/
├── lsw-campaign-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/java/...
│       └── main/resources/application.yml
├── lsw-battle-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/java/...
│       └── main/resources/application.yml
├── lsw-pvp-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/java/...
│       └── main/resources/application.yml
├── lsw-gateway/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/java/...
│   │       └── resources/application.yml
└── lsw-client/
	├── pom.xml
	└── src/
		├── main/java/...
		└── resources/
```

Notes:
- Many modules include a `Dockerfile` and an `application.yml` under `src/main/resources` for service configuration.
- Java sources live under `src/main/java` with package roots under `org` / `org/example`.
- Build outputs are placed in `target/` for each module (classes, generated-sources, test-classes, etc.).

## Building

From the repository root run:

```bash
mvn -T 1C clean install
```

This will build all modules and run tests.

## Docker

- Individual services include `Dockerfile` where applicable.
- A top-level `docker-compose.yml` is provided for local integration testing.

## CI/CD

The codebase includes a GitHub Actions workflow that builds, tests, runs integration smoke tests with Docker Compose, and publishes container images. See [`.github/workflows/ci-cd.yml`](.github/workflows/ci-cd.yml#L1) for the canonical configuration.

### What the pipeline does

- Runs Maven unit tests and uploads Surefire reports as artifacts.
- Spins up the system stack with `docker compose` and runs smoke/integration tests (`ci/smoke-test-compose.sh`).
- Builds and publishes Docker images to GHCR for each service in a matrix (only on pushes).

### Pipeline architecture

```
GitHub Push/PR/Tag
	│
	▼
┌───────────────────────────┐
│ Maven Unit Tests (job: maven-tests)
│ - Checkout
│ - Setup JDK (Temurin, Java 21)
│ - mvn test
│ - Upload test reports
└───────────────────────────┘
	│
	▼
┌───────────────────────────┐
│ Docker Compose System Tests (job: compose-system-tests)
│ - Build and start stack via `docker compose -f docker-compose.yml up --build -d`
│ - Run `./ci/smoke-test-compose.sh`
│ - Tear down stack
└───────────────────────────┘
	│
	▼
┌───────────────────────────┐
│ Publish Docker Images (job: publish-images)
│ - Runs on pushes
│ - Matrix builds for: lsw-user-service, lsw-party-service, lsw-campaign-service, lsw-battle-service, lsw-pvp-service, lsw-gateway
│ - Builds with Docker Buildx and pushes to `ghcr.io/${{ github.repository_owner }}`
└───────────────────────────┘
```

### Triggers

- `pull_request` targeting the `master` branch.
- `push` to the `master` branch.
- `push` of tags matching `v*` (semantic-release style tags trigger image tagging).
