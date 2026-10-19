# SupportDesk — Gestion des tickets de support

Application full-stack de gestion des tickets de support réalisée dans le cadre de
l'épreuve **KFOKAM48 — Batch 2 — Phase 3 (module Ticket)**.

- **Auteur :** Ulrich Siko
- **Dépôt :** https://github.com/Ulrichsk/git-github
- **Branche de travail :** `feature/ticket-module`

## Sommaire

1. [Fonctionnalités](#fonctionnalités)
2. [Stack technique](#stack-technique)
3. [Démarrage rapide avec Docker](#démarrage-rapide-avec-docker)
4. [Démarrage local sans Docker](#démarrage-local-sans-docker)
5. [API REST](#api-rest)
6. [Règles métier](#règles-métier)
7. [Tests](#tests)
8. [Structure du projet](#structure-du-projet)
9. [Configuration](#configuration)
10. [Git et identité des commits](#git-et-identité-des-commits)

## Fonctionnalités

- Création, consultation, modification et suppression de tickets.
- Liste paginée avec recherche plein texte (titre, description, référence, demandeur)
  et filtres par statut et priorité.
- Cycle de vie contrôlé : `OPEN → IN_PROGRESS → RESOLVED → CLOSED`.
- Assignation d'un ticket à un technicien.
- Fil d'activité : commentaires horodatés sur chaque ticket.
- Tableau de bord : total, répartition par statut et priorité, tickets non assignés.
- Validation des données côté API (Bean Validation) et côté interface.
- Erreurs API normalisées au format RFC 7807 (`ProblemDetail`).
- Documentation interactive Swagger UI / OpenAPI 3.
- Interface React responsive en français (liste, filtres, formulaire, panneau de détail).
- PostgreSQL en conteneur Docker avec volume persistant.

## Stack technique

| Couche | Technologies |
|---|---|
| Backend | Java 21, Spring Boot 3.3.4, Spring Web, Spring Data JPA, Bean Validation |
| Base de données | PostgreSQL 16 (Docker), H2 en mode PostgreSQL pour les tests |
| Documentation | Springdoc OpenAPI 2.6.0, Swagger UI |
| Frontend | React 18, TypeScript 5.6, Vite 5, CSS natif |
| Build | Maven 3.9+, npm 10+ |
| Conteneurs | Docker, Docker Compose (PostgreSQL + backend + frontend nginx) |

## Démarrage rapide avec Docker

Prérequis : Docker Engine et Docker Compose v2.

```bash
cp .env.example .env      # optionnel : valeurs par défaut identiques
docker compose up --build
```

| Service | URL |
|---|---|
| Interface web | http://localhost:8081 |
| API REST | http://localhost:8080/api/tickets |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| PostgreSQL | `localhost:5432` (base `tickets`, utilisateur `tickets`) |

Les données PostgreSQL sont conservées dans le volume nommé `ticket_pgdata` :
`docker compose down` puis `docker compose up` ne perd pas les tickets.
Pour repartir de zéro : `docker compose down -v`.

## Démarrage local sans Docker

Prérequis : Java 21, Maven 3.8+, Node.js 20+, PostgreSQL 16.

1. **Base de données** — le plus simple est de démarrer uniquement le conteneur PostgreSQL :

   ```bash
   docker compose up -d db
   ```

   Ou avec une installation PostgreSQL locale :

   ```sql
   CREATE USER tickets WITH PASSWORD 'tickets';
   CREATE DATABASE tickets OWNER tickets;
   ```

2. **Backend** (les valeurs par défaut ciblent `localhost:5432`) :

   ```bash
   cd backend
   mvn spring-boot:run
   ```

   Variables disponibles : `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SERVER_PORT`,
   `CORS_ALLOWED_ORIGINS`.

3. **Frontend** :

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

   L'interface est disponible sur http://localhost:5173. Vite redirige `/api`
   vers `http://localhost:8080` (voir `frontend/vite.config.ts`).

## API REST

Base : `/api/tickets`

| Méthode | Chemin | Description | Codes |
|---|---|---|---|
| `GET` | `/api/tickets` | Liste paginée et filtrée | 200, 400 |
| `GET` | `/api/tickets/stats` | Statistiques globales | 200 |
| `GET` | `/api/tickets/{id}` | Détail d'un ticket avec ses commentaires | 200, 404 |
| `POST` | `/api/tickets` | Création d'un ticket | 201, 400 |
| `PUT` | `/api/tickets/{id}` | Modification d'un ticket non clôturé | 200, 400, 404, 409 |
| `PATCH` | `/api/tickets/{id}/status` | Changement de statut | 200, 400, 404, 409 |
| `PATCH` | `/api/tickets/{id}/assignee` | Assignation | 200, 400, 404, 409 |
| `POST` | `/api/tickets/{id}/comments` | Ajout d'un commentaire | 201, 400, 404, 409 |
| `DELETE` | `/api/tickets/{id}` | Suppression d'un ticket clôturé | 204, 404, 409 |

### Paramètres de liste

| Paramètre | Exemple | Description |
|---|---|---|
| `status` | `OPEN` | `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED` |
| `priority` | `HIGH` | `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` |
| `q` | `vpn` | Recherche insensible à la casse |
| `page` | `0` | Index de page (défaut 0) |
| `size` | `10` | Taille de page (défaut 10, max 100) |
| `sort` | `createdAt,desc` | `createdAt`, `updatedAt`, `priority`, `status`, `title`… |

### Exemple de création

```bash
curl -X POST http://localhost:8080/api/tickets \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Connexion VPN impossible",
    "description": "Le client VPN renvoie une erreur d authentification depuis ce matin.",
    "priority": "HIGH",
    "category": "Réseau",
    "requesterName": "Ulrich Siko",
    "requesterEmail": "sikoulrich204@gmail.com",
    "assignee": null
  }'
```

Réponse `201 Created` :

```json
{
  "id": 1,
  "reference": "TKT-0001",
  "title": "Connexion VPN impossible",
  "status": "OPEN",
  "priority": "HIGH",
  "requesterName": "Ulrich Siko",
  "requesterEmail": "sikoulrich204@gmail.com",
  "assignee": null,
  "commentCount": 0,
  "comments": []
}
```

### Format des erreurs

```json
{
  "type": "https://kfokam48.local/errors/400",
  "title": "Donnees invalides",
  "status": 400,
  "detail": "Certains champs de la requete sont invalides.",
  "errors": {
    "title": "Le titre doit contenir entre 5 et 120 caracteres"
  }
}
```

## Règles métier

```
OPEN ──► IN_PROGRESS ──► RESOLVED ──► CLOSED
           ▲    │           │
           └────┘           └──► IN_PROGRESS (reprise)
```

- Tout ticket est créé au statut `OPEN`.
- Transitions autorisées :
  - `OPEN → IN_PROGRESS`
  - `IN_PROGRESS → OPEN` (remise en attente) ou `IN_PROGRESS → RESOLVED`
  - `RESOLVED → IN_PROGRESS` (reprise) ou `RESOLVED → CLOSED`
  - `CLOSED` est un état terminal.
- Un ticket ne peut être résolu que s'il est assigné à un technicien.
- Un ticket clôturé ne peut plus être modifié, assigné ou commenté.
- Seul un ticket clôturé peut être supprimé.
- Les titres, descriptions et noms sont nettoyés des espaces superflus ; l'email est
  normalisé en minuscules.

## Tests

```bash
cd backend
mvn test
```

La suite couvre les règles de cycle de vie, la validation, les filtres, les erreurs
et les principaux endpoints. Les tests utilisent H2 en mode PostgreSQL ; l'application
exécutée utilise PostgreSQL.

```bash
cd frontend
npm run typecheck
npm run build
```

## Structure du projet

```
.
├── backend/
│   ├── src/main/java/com/kfokam48/tickets/
│   │   ├── config/        # CORS, OpenAPI
│   │   ├── domain/        # Ticket, TicketComment, enums
│   │   ├── dto/           # Requêtes/réponses validées
│   │   ├── exception/     # Exceptions métier et 404
│   │   ├── repository/    # Spring Data JPA
│   │   ├── service/       # Règles métier
│   │   └── web/           # Contrôleur REST, gestion globale des erreurs
│   ├── src/test/          # Tests service + API
│   ├── Dockerfile
│   └── pom.xml
├── frontend/
│   ├── src/components/    # Badges, formulaire, détail, statistiques
│   ├── src/api.ts         # Client HTTP typé
│   ├── src/App.tsx        # Liste, filtres, actions
│   ├── nginx.conf
│   └── Dockerfile
├── docker-compose.yml
├── .env.example
└── README.md
```

## Configuration

| Variable | Défaut | Usage |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/tickets` | Connexion JDBC |
| `DB_USERNAME` | `tickets` | Utilisateur PostgreSQL |
| `DB_PASSWORD` | `tickets` | Mot de passe PostgreSQL |
| `SERVER_PORT` | `8080` | Port du backend |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,...` | Origines autorisées |
| `POSTGRES_DB` | `tickets` | Base créée par Docker Compose |
| `POSTGRES_USER` | `tickets` | Utilisateur créé par Docker Compose |
| `POSTGRES_PASSWORD` | `tickets` | Mot de passe créé par Docker Compose |

## Git et identité des commits

Le dépôt distant est `https://github.com/Ulrichsk/git-github`. Le travail est
développé sur la branche `feature/ticket-module`.

L'identité locale du dépôt est configurée pour attribuer tous les commits à
l'étudiant :

```bash
git config user.name "Ulrich Siko"
git config user.email "sikoulrich204@gmail.com"
```

Vérification :

```bash
git log -1 --format='%an <%ae> | %cn <%ce>'
# Ulrich Siko <sikoulrich204@gmail.com> | Ulrich Siko <sikoulrich204@gmail.com>
```
