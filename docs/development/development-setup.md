# Development Setup

## Purpose

This document provides step-by-step instructions to set up the CYNA Platform development environment on a local workstation.

---

## Prerequisites

### Required Software

| Software | Version | Purpose |
|----------|---------|---------|
| **Java JDK** | 21 (LTS) | Backend runtime |
| **Gradle** | 8.x (or use Gradle Wrapper) | Build tool |
| **Node.js** | 22.x (LTS) | Frontend runtime |
| **npm** | 10.x+ | Frontend package manager |
| **Angular CLI** | 20.x | Frontend build/serve tool |
| **Docker** | 24.x+ | Local infrastructure (PostgreSQL, etc.) |
| **Docker Compose** | 2.x+ | Container orchestration |
| **Git** | 2.40+ | Version control |
| **IDE** | IntelliJ IDEA Ultimate (recommended) | Development environment |

### Optional Software

| Software | Purpose |
|----------|---------|
| **pgAdmin** or **DBeaver** | Database GUI |
| **Postman** or **Bruno** | API testing |
| **VS Code** | Alternative IDE / frontend development |

---

## Repository Setup

### 1. Clone the Repository

```bash
git clone https://github.com/cyna/cyna-platform.git
cd cyna-platform
```

### 2. Configure Git Hooks

```bash
# Install pre-commit hooks (if using Husky or similar)
./gradlew installGitHooks
```

---

## Backend Setup

### 1. Start Infrastructure Services

```bash
# Start PostgreSQL and other services
docker compose up -d
```

The `docker-compose.yml` provides:

| Service | Port | Credentials |
|---------|------|-------------|
| PostgreSQL | 5432 | `cyna` / `cyna_dev_password` |
| pgAdmin (optional) | 5050 | `admin@cyna.com` / `admin` |

### 2. Configure Environment

Copy the example environment configuration:

```bash
cp .env.example .env
```

Edit `.env` with your local settings:

```properties
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=cyna
DB_USERNAME=cyna
DB_PASSWORD=cyna_dev_password

# Optional explicit datasource override
# SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/cyna
# SPRING_DATASOURCE_USERNAME=cyna
# SPRING_DATASOURCE_PASSWORD=cyna_dev_password

# JWT
JWT_SECRET=your-local-dev-secret-key-min-256-bits
JWT_ACCESS_EXPIRATION_HOURS=1
JWT_REFRESH_EXPIRATION_HOURS=24

# Application
SPRING_PROFILES_ACTIVE=local
```

### 3. Run Database Migrations

```bash
./gradlew flywayMigrate
```

### 4. Build the Backend

```bash
# Full build with tests
./gradlew build

# Build without tests (faster)
./gradlew build -x test
```

### 5. Run the Backend

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

The API will be available at `http://localhost:8080`.

### 6. Verify

```bash
# Health check
curl http://localhost:8080/actuator/health

# Expected response
{"status": "UP"}
```

---

## Frontend Setup

### 1. Install Dependencies

```bash
cd cyna-frontend
npm install
```

### 2. Configure Environment

```bash
# The environment files are in:
# projects/pwa/src/environments/environment.ts
# projects/backoffice/src/environments/environment.ts
```

Default local configuration points to `http://localhost:8080/api`.

### 3. Run the Customer PWA

```bash
ng serve pwa
```

Available at `http://localhost:4200`.

### 4. Run the Admin Backoffice

```bash
ng serve backoffice --port 4201
```

Available at `http://localhost:4201`.

### 5. Build the UI Kit

```bash
ng build ui-kit --watch
```

---

## IDE Configuration

### IntelliJ IDEA

1. **Import project** → Select `build.gradle.kts` → Import as Gradle project.
2. **Set JDK** → File → Project Structure → SDK → Java 21.
3. **Enable annotation processing** → Settings → Build → Compiler → Annotation Processors → Enable.
4. **Code style** → Import the team's `.editorconfig` and code style XML.
5. **Plugins** (recommended):
   - Lombok
   - Spring Boot
   - Angular
   - SonarLint

### VS Code (Frontend)

1. **Extensions** (recommended):
   - Angular Language Service
   - ESLint
   - Prettier
   - TypeScript Hero
   - GitLens

---

## Common Commands

### Backend

| Command | Purpose |
|---------|---------|
| `./gradlew build` | Full build with tests |
| `./gradlew build -x test` | Build without tests |
| `./gradlew bootRun` | Run application |
| `./gradlew test` | Run unit tests |
| `./gradlew integrationTest` | Run integration tests |
| `./gradlew flywayMigrate` | Apply database migrations |
| `./gradlew flywayClean` | Reset database (destructive) |
| `./gradlew flywayInfo` | Show migration status |
| `./gradlew dependencies` | Show dependency tree |

### Frontend

| Command | Purpose |
|---------|---------|
| `ng serve pwa` | Run customer PWA |
| `ng serve backoffice` | Run admin backoffice |
| `ng build ui-kit` | Build UI Kit library |
| `ng test pwa` | Run PWA unit tests |
| `ng test backoffice` | Run backoffice unit tests |
| `ng lint` | Run ESLint |
| `npm run format` | Run Prettier |

### Docker

| Command | Purpose |
|---------|---------|
| `docker compose up -d` | Start infrastructure |
| `docker compose down` | Stop infrastructure |
| `docker compose down -v` | Stop and remove volumes (reset data) |
| `docker compose logs -f postgres` | Follow PostgreSQL logs |

---

## Troubleshooting

### Port Already in Use

```bash
# Find process on port 8080
# Windows
netstat -ano | findstr :8080
# Linux/Mac
lsof -i :8080
```

### Database Connection Failed

1. Verify Docker is running: `docker ps`
2. Check PostgreSQL logs: `docker compose logs postgres`
3. Verify connection string in `.env`

### Flyway Migration Failure

1. Check migration status: `./gradlew flywayInfo`
2. If corrupted, reset (dev only): `./gradlew flywayClean flywayMigrate`
3. Never use `flywayClean` in staging or production.

### Node Module Issues

```bash
# Clean and reinstall
rm -rf node_modules
npm cache clean --force
npm install
```

---

## Related Documents

- [Coding Standards](coding-standards.md)
- [Database Guidelines](../database/database-guidelines.md)
- [Git Workflow](../workflow/git-workflow.md)
