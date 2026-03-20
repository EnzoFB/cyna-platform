# cyna-platform

Plateforme Cyna: Angular (PWA + Backoffice) + backend Spring Boot.

## Prerequis

- Docker Desktop
- Node.js (version LTS recommandee)
- Java 21

## Demarrage rapide

Depuis la racine du projet:

1. Demarrer PostgreSQL (Docker)

```powershell
docker compose up -d
```

2. Lancer le backend

```powershell
Set-Location cyna-backend
.\gradlew.bat bootRun
```

3. Lancer le frontend (dans un 2e terminal)

```powershell
Set-Location cyna-frontend
npm install
ng serve pwa
```

Option backoffice:

```powershell
ng serve backoffice
```

## URLs utiles

- PWA: http://localhost:4200
- Backoffice: http://localhost:4200 (ou le port propose par Angular, souvent 4201 si 4200 est deja occupe)
- API backend: http://localhost:8080/api/v1
- Swagger UI: http://localhost:8080/swagger-ui.html

## Arret des services

Depuis la racine du projet:

```powershell
docker compose down
```
