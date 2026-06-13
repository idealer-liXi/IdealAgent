# Agent Docker Packaging Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Package the IdealAgent frontend and backend for Docker Compose deployment with all image and container names using the `agent-*` prefix.

**Architecture:** Build backend and frontend artifacts on the host, then build small runtime images from those artifacts. Docker Compose runs MySQL, Redis, pgvector, backend, and frontend on one bridge network, with frontend proxying `/api/v1` to the backend context path. RabbitMQ is removed because the codebase has no RabbitMQ usage.

**Tech Stack:** Spring Boot 3.4, Maven, Java 17, Vue/Vite, npm, Docker Compose, MySQL 8, Redis 8, pgvector/PostgreSQL 16, nginx runtime for frontend.

---

### File Structure

- Create: `backend/Dockerfile` - runtime image for the Spring Boot jar.
- Create: `frontend/Dockerfile` - runtime image for built Vite static files.
- Create: `frontend/nginx.conf` - static hosting and `/api/v1` reverse proxy.
- Create: `backend/ideal-agent-app/src/main/resources/application-docker.yml` - Docker profile using Compose service names.
- Modify: `backend/docs/docker/docker-compose.yml` - add app services, remove RabbitMQ, rename containers, images, volumes, and network to `agent-*`.
- Modify: `backend/docs/docker/.env.example` - add backend/frontend image tags and optional runtime env vars without secrets.

### Task 1: Backend Docker Profile

- [ ] Create `backend/ideal-agent-app/src/main/resources/application-docker.yml` with server port `8066`, context path `/idealagent/api/v1`, MySQL URL `mysql:3306`, PostgreSQL URL `pgvector:5432`, and environment-backed credentials.
- [ ] Run backend config-related tests or Maven package to verify the profile does not break compilation.

### Task 2: Runtime Dockerfiles

- [ ] Create `backend/Dockerfile` that copies `ideal-agent-app/target/ideal-agent-app-1.0.0.jar` and runs it with `SPRING_PROFILES_ACTIVE=docker`.
- [ ] Create `frontend/Dockerfile` that copies `dist` into nginx html root.
- [ ] Create `frontend/nginx.conf` that serves SPA routes and proxies `/api/v1` to `http://agent-backend:8066/idealagent/api/v1`.

### Task 3: Compose Cleanup And App Services

- [ ] Modify `backend/docs/docker/docker-compose.yml` so all image names and container names use `agent-*`.
- [ ] Remove the `rabbitmq` service.
- [ ] Add `agent-backend` build from `../../backend` is not correct from compose location; use `context: ../..` and `dockerfile: backend/Dockerfile`.
- [ ] Add `agent-frontend` build with `context: ../../frontend` and expose port `80:80`.
- [ ] Add `depends_on` from backend to mysql, redis, pgvector; from frontend to backend.

### Task 4: Packaging Verification

- [ ] Run `npm run build` in `frontend`.
- [ ] Run Maven package for backend from `backend`.
- [ ] Run `docker compose -f backend/docs/docker/docker-compose.yml config` to validate Compose syntax.
- [ ] Build images with `docker compose -f backend/docs/docker/docker-compose.yml build backend frontend`.
- [ ] Inspect images with `docker images` and confirm `agent-backend` and `agent-frontend` exist.

### Self-Review

- The plan covers frontend package, backend package, Compose update, RabbitMQ removal, and `agent-*` naming.
- No real API keys or MCP secrets are introduced.
- No commits are included because the user has not explicitly requested a commit.
