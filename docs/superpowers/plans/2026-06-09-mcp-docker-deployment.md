# MCP Docker Deployment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deploy the four non-WeCom MCP SSE services as private Docker Compose services on txcloud.

**Architecture:** Each MCP module remains an independent Spring Boot jar and gets its own `agent-mcp-*` image/container. Containers join the existing `agent-network` and are reached by `agent-backend` through Docker DNS names, with no public port mappings.

**Tech Stack:** Spring Boot 3.4.3, Java 17, Spring AI MCP SSE server, Docker, Docker Compose, MySQL `ai_mcp` configuration.

**User Constraints:** Work in the current workspace. Do not create a git worktree. Do not commit unless the user explicitly asks.

---

## File Structure

- Create `mcp/Dockerfile`: shared runtime Dockerfile for packaged MCP jars.
- Modify `backend/docs/docker/docker-compose.yml`: add four private MCP services.
- Modify `backend/docs/docker/.env.example`: document MCP image names only; do not add public port variables.
- Modify `backend/docs/mysql/ideal_agent_seed.sql`: change Docker seed MCP `baseUri` values from `localhost` to container DNS names, preserving redacted secrets.
- Deploy to txcloud under `/home/ubuntu/agent-deploy`: build MCP images remotely, then upload Compose file, MCP Dockerfile, and updated seed/config files.

---

### Task 1: Add Shared MCP Dockerfile

**Files:**
- Create: `mcp/Dockerfile`

- [ ] **Step 1: Create the Dockerfile**

Create `mcp/Dockerfile` with this exact content:

```dockerfile
FROM eclipse-temurin:17-jre-alpine

ARG MCP_MODULE
ARG MCP_JAR
ARG MCP_PORT

WORKDIR /app

COPY ${MCP_MODULE}/target/${MCP_JAR} /app/mcp-server.jar

EXPOSE ${MCP_PORT}

ENTRYPOINT ["java", "-jar", "/app/mcp-server.jar"]
```

- [ ] **Step 2: Verify the file exists**

Run: `Test-Path -LiteralPath "mcp\Dockerfile"`

Expected: `True`

---

### Task 2: Add MCP Services To Compose

**Files:**
- Modify: `backend/docs/docker/docker-compose.yml`

- [ ] **Step 1: Insert four MCP services**

Add these services after `agent-backend` and before `agent-frontend`:

```yaml
  agent-mcp-csdn:
    image: ${AGENT_MCP_CSDN_IMAGE:-agent-mcp-csdn:latest}
    build:
      context: ../../../mcp
      dockerfile: Dockerfile
      args:
        MCP_MODULE: mcp-server-csdn
        MCP_JAR: mcp-server-csdn.jar
        MCP_PORT: 9001
    container_name: agent-mcp-csdn
    restart: unless-stopped
    expose:
      - "9001"
    networks:
      - agent-network

  agent-mcp-amap:
    image: ${AGENT_MCP_AMAP_IMAGE:-agent-mcp-amap:latest}
    build:
      context: ../../../mcp
      dockerfile: Dockerfile
      args:
        MCP_MODULE: mcp-server-amap
        MCP_JAR: mcp-server-amap.jar
        MCP_PORT: 9003
    container_name: agent-mcp-amap
    restart: unless-stopped
    expose:
      - "9003"
    networks:
      - agent-network

  agent-mcp-email:
    image: ${AGENT_MCP_EMAIL_IMAGE:-agent-mcp-email:latest}
    build:
      context: ../../../mcp
      dockerfile: Dockerfile
      args:
        MCP_MODULE: mcp-server-email
        MCP_JAR: mcp-server-email.jar
        MCP_PORT: 9004
    container_name: agent-mcp-email
    restart: unless-stopped
    expose:
      - "9004"
    networks:
      - agent-network

  agent-mcp-bocha:
    image: ${AGENT_MCP_BOCHA_IMAGE:-agent-mcp-bocha:latest}
    build:
      context: ../../../mcp
      dockerfile: Dockerfile
      args:
        MCP_MODULE: mcp-server-bocha
        MCP_JAR: mcp-server-bocha.jar
        MCP_PORT: 9005
    container_name: agent-mcp-bocha
    restart: unless-stopped
    expose:
      - "9005"
    networks:
      - agent-network
```

Do not add `ports` entries for these services.

- [ ] **Step 2: Validate Compose syntax locally**

Run: `docker compose -f backend/docs/docker/docker-compose.yml config --quiet`

Expected: command exits `0`.

---

### Task 3: Document MCP Image Variables

**Files:**
- Modify: `backend/docs/docker/.env.example`

- [ ] **Step 1: Add image variables**

Insert these lines after `AGENT_FRONTEND_IMAGE=agent-frontend:latest`:

```dotenv
AGENT_MCP_CSDN_IMAGE=agent-mcp-csdn:latest
AGENT_MCP_AMAP_IMAGE=agent-mcp-amap:latest
AGENT_MCP_EMAIL_IMAGE=agent-mcp-email:latest
AGENT_MCP_BOCHA_IMAGE=agent-mcp-bocha:latest
```

Do not add public MCP port variables.

- [ ] **Step 2: Confirm no MCP port variables exist**

Run: `rg "AGENT_MCP_.*PORT|9001:|9003:|9004:|9005:" backend/docs/docker/.env.example backend/docs/docker/docker-compose.yml`

Expected: no output for host port mappings or MCP port env vars.

---

### Task 4: Update Docker Seed MCP Base URIs

**Files:**
- Modify: `backend/docs/mysql/ideal_agent_seed.sql`

- [ ] **Step 1: Replace localhost base URIs with Docker DNS names**

Make these exact replacements in the `ai_mcp` seed rows:

```text
http://localhost:9001 -> http://agent-mcp-csdn:9001
http://localhost:9003 -> http://agent-mcp-amap:9003
http://localhost:9004 -> http://agent-mcp-email:9004
http://localhost:9005 -> http://agent-mcp-bocha:9005
```

Keep `mcp_secret` values as `{}`.

- [ ] **Step 2: Verify seed routing and redaction**

Run: `rg "localhost:900|agent-mcp-|mcp_secret|sk-|smtpPassword|apiKey|cookie" backend/docs/mysql/ideal_agent_seed.sql`

Expected:

```text
No localhost:900 entries.
Four agent-mcp-* baseUri entries.
No real sk-* token, SMTP password, API key, or cookie value.
```

---

### Task 5: Build And Verify MCP Jars Locally

**Files:**
- Generated: `mcp/mcp-server-csdn/target/mcp-server-csdn.jar`
- Generated: `mcp/mcp-server-amap/target/mcp-server-amap.jar`
- Generated: `mcp/mcp-server-email/target/mcp-server-email.jar`
- Generated: `mcp/mcp-server-bocha/target/mcp-server-bocha.jar`

- [ ] **Step 1: Package CSDN MCP**

Run: `mvn -f mcp/mcp-server-csdn/pom.xml clean package`

Expected: build exits `0` and creates `mcp/mcp-server-csdn/target/mcp-server-csdn.jar`.

- [ ] **Step 2: Package Amap MCP**

Run: `mvn -f mcp/mcp-server-amap/pom.xml clean package`

Expected: build exits `0` and creates `mcp/mcp-server-amap/target/mcp-server-amap.jar`.

- [ ] **Step 3: Package Email MCP**

Run: `mvn -f mcp/mcp-server-email/pom.xml clean package`

Expected: build exits `0` and creates `mcp/mcp-server-email/target/mcp-server-email.jar`.

- [ ] **Step 4: Package Bocha MCP**

Run: `mvn -f mcp/mcp-server-bocha/pom.xml clean package`

Expected: build exits `0` and creates `mcp/mcp-server-bocha/target/mcp-server-bocha.jar`.

---

### Task 6: Build MCP Docker Images Remotely On txcloud

**Files:**
- Uses: `mcp/Dockerfile`
- Uses: packaged MCP jars from Task 5

Local Docker daemon access is unavailable, so build the MCP images directly on txcloud. Upload the packaged jars and shared Dockerfile to `/home/ubuntu/agent-deploy/mcp`, then run `sudo docker build` remotely.

- [ ] **Step 1: Ensure remote MCP build directories exist**

Run:

```powershell
ssh -o BatchMode=yes txcloud "mkdir -p /home/ubuntu/agent-deploy/mcp/mcp-server-csdn/target /home/ubuntu/agent-deploy/mcp/mcp-server-amap/target /home/ubuntu/agent-deploy/mcp/mcp-server-email/target /home/ubuntu/agent-deploy/mcp/mcp-server-bocha/target"
```

Expected: command exits `0`.

- [ ] **Step 2: Upload MCP Dockerfile and jars to txcloud**

Run each command:

```powershell
scp -o BatchMode=yes "mcp/Dockerfile" txcloud:/home/ubuntu/agent-deploy/mcp/Dockerfile
scp -o BatchMode=yes "mcp/mcp-server-csdn/target/mcp-server-csdn.jar" txcloud:/home/ubuntu/agent-deploy/mcp/mcp-server-csdn/target/mcp-server-csdn.jar
scp -o BatchMode=yes "mcp/mcp-server-amap/target/mcp-server-amap.jar" txcloud:/home/ubuntu/agent-deploy/mcp/mcp-server-amap/target/mcp-server-amap.jar
scp -o BatchMode=yes "mcp/mcp-server-email/target/mcp-server-email.jar" txcloud:/home/ubuntu/agent-deploy/mcp/mcp-server-email/target/mcp-server-email.jar
scp -o BatchMode=yes "mcp/mcp-server-bocha/target/mcp-server-bocha.jar" txcloud:/home/ubuntu/agent-deploy/mcp/mcp-server-bocha/target/mcp-server-bocha.jar
```

Expected: all commands exit `0`.

- [ ] **Step 3: Build CSDN image on txcloud**

Run:

```powershell
ssh -o BatchMode=yes txcloud "cd /home/ubuntu/agent-deploy && sudo docker build -f mcp/Dockerfile --build-arg MCP_MODULE=mcp-server-csdn --build-arg MCP_JAR=mcp-server-csdn.jar --build-arg MCP_PORT=9001 -t agent-mcp-csdn:latest mcp"
```

Expected: image `agent-mcp-csdn:latest` exists on txcloud.

- [ ] **Step 4: Build Amap image on txcloud**

Run:

```powershell
ssh -o BatchMode=yes txcloud "cd /home/ubuntu/agent-deploy && sudo docker build -f mcp/Dockerfile --build-arg MCP_MODULE=mcp-server-amap --build-arg MCP_JAR=mcp-server-amap.jar --build-arg MCP_PORT=9003 -t agent-mcp-amap:latest mcp"
```

Expected: image `agent-mcp-amap:latest` exists on txcloud.

- [ ] **Step 5: Build Email image on txcloud**

Run:

```powershell
ssh -o BatchMode=yes txcloud "cd /home/ubuntu/agent-deploy && sudo docker build -f mcp/Dockerfile --build-arg MCP_MODULE=mcp-server-email --build-arg MCP_JAR=mcp-server-email.jar --build-arg MCP_PORT=9004 -t agent-mcp-email:latest mcp"
```

Expected: image `agent-mcp-email:latest` exists on txcloud.

- [ ] **Step 6: Build Bocha image on txcloud**

Run:

```powershell
ssh -o BatchMode=yes txcloud "cd /home/ubuntu/agent-deploy && sudo docker build -f mcp/Dockerfile --build-arg MCP_MODULE=mcp-server-bocha --build-arg MCP_JAR=mcp-server-bocha.jar --build-arg MCP_PORT=9005 -t agent-mcp-bocha:latest mcp"
```

Expected: image `agent-mcp-bocha:latest` exists on txcloud.

- [ ] **Step 7: Inspect remote images**

Run: `ssh -o BatchMode=yes txcloud "sudo docker image ls 'agent-mcp-*'"`

Expected: four images are listed.

---

### Task 7: Upload Compose Assets To txcloud

**Files:**
- Upload: `backend/docs/docker/docker-compose.yml`
- Upload: `backend/docs/docker/.env.example`
- Upload: `backend/docs/mysql/ideal_agent_seed.sql`
- Upload: `mcp/Dockerfile`

Images are already built on txcloud by Task 6. Do not create, upload, or load an image archive in this task.

- [ ] **Step 1: Ensure remote deployment asset directories exist**

Run:

```powershell
ssh -o BatchMode=yes txcloud "mkdir -p /home/ubuntu/agent-deploy/backend/docs/docker /home/ubuntu/agent-deploy/backend/docs/mysql /home/ubuntu/agent-deploy/mcp"
```

Expected: command exits `0`.

- [ ] **Step 2: Check remote name conflicts**

Run:

```powershell
ssh -o BatchMode=yes txcloud "sudo docker ps -a --format '{{.Names}}' | grep -E '^agent-mcp-(csdn|amap|email|bocha)$' || true"
```

Expected: no output, or only stopped containers that belong to the current deployment.

- [ ] **Step 3: Upload Compose, env, and seed files**

Run each command:

```powershell
scp -o BatchMode=yes "backend/docs/docker/docker-compose.yml" txcloud:/home/ubuntu/agent-deploy/backend/docs/docker/docker-compose.yml
scp -o BatchMode=yes "backend/docs/docker/.env.example" txcloud:/home/ubuntu/agent-deploy/backend/docs/docker/.env.example
scp -o BatchMode=yes "backend/docs/mysql/ideal_agent_seed.sql" txcloud:/home/ubuntu/agent-deploy/backend/docs/mysql/ideal_agent_seed.sql
```

Expected: all commands exit `0`.

- [ ] **Step 4: Upload MCP Dockerfile for deploy-source parity**

Run:

```powershell
scp -o BatchMode=yes "mcp/Dockerfile" txcloud:/home/ubuntu/agent-deploy/mcp/Dockerfile
```

Expected: command exits `0`.

---

### Task 8: Start MCP Services On txcloud

**Files:**
- Uses remote: `/home/ubuntu/agent-deploy/backend/docs/docker/docker-compose.yml`

- [ ] **Step 1: Validate remote Compose config**

Run:

```powershell
ssh -o BatchMode=yes txcloud "cd /home/ubuntu/agent-deploy && sudo docker compose -f backend/docs/docker/docker-compose.yml config --quiet"
```

Expected: command exits `0`. If the remote MCP build context is missing, create `/home/ubuntu/agent-deploy/mcp` and rerun; do not expose MCP ports.

- [ ] **Step 2: Start four MCP services**

Run:

```powershell
ssh -o BatchMode=yes txcloud "cd /home/ubuntu/agent-deploy && sudo docker compose -f backend/docs/docker/docker-compose.yml up -d agent-mcp-csdn agent-mcp-amap agent-mcp-email agent-mcp-bocha"
```

Expected: four MCP containers start.

- [ ] **Step 3: Verify no public MCP ports are mapped**

Run:

```powershell
ssh -o BatchMode=yes txcloud "sudo docker ps --filter name=agent-mcp --format '{{.Names}} {{.Ports}}'"
```

Expected: output lists four containers with container ports only, and no `0.0.0.0:9001`, `0.0.0.0:9003`, `0.0.0.0:9004`, or `0.0.0.0:9005` mapping.

---

### Task 9: Update txcloud MCP Routing Config

**Files:**
- No tracked file changes. Remote DB update only.

- [ ] **Step 1: Apply config-only SQL update**

Run:

```powershell
ssh -o BatchMode=yes txcloud "sudo docker exec agent-mysql mysql -uidealagent -p123456 --default-character-set=utf8mb4 ideal_agent -e \"UPDATE ai_mcp SET mcp_config='{\\\"baseUri\\\":\\\"http://agent-mcp-csdn:9001\\\",\\\"sseEndpoint\\\":\\\"/sse\\\",\\\"timeoutMinutes\\\":3}' WHERE mcp_id='mcp_csdn'; UPDATE ai_mcp SET mcp_config='{\\\"baseUri\\\":\\\"http://agent-mcp-amap:9003\\\",\\\"sseEndpoint\\\":\\\"/sse\\\",\\\"timeoutMinutes\\\":3}' WHERE mcp_id='mcp_amap'; UPDATE ai_mcp SET mcp_config='{\\\"baseUri\\\":\\\"http://agent-mcp-email:9004\\\",\\\"sseEndpoint\\\":\\\"/sse\\\",\\\"timeoutMinutes\\\":5}' WHERE mcp_id='mcp_qq_smtp'; UPDATE ai_mcp SET mcp_config='{\\\"baseUri\\\":\\\"http://agent-mcp-bocha:9005\\\",\\\"sseEndpoint\\\":\\\"/sse\\\",\\\"timeoutMinutes\\\":3}' WHERE mcp_id='mcp_bocha';\""
```

Expected: command exits `0`. This updates only `mcp_config`; it does not modify `mcp_secret`.

- [ ] **Step 2: Verify DB routing**

Run:

```powershell
ssh -o BatchMode=yes txcloud "sudo docker exec agent-mysql mysql -uidealagent -p123456 --default-character-set=utf8mb4 -N ideal_agent -e 'SELECT mcp_id, mcp_config, mcp_secret FROM ai_mcp WHERE mcp_id IN (\"mcp_csdn\",\"mcp_amap\",\"mcp_qq_smtp\",\"mcp_bocha\") ORDER BY mcp_id;'"
```

Expected: four rows have `agent-mcp-*` base URIs. `mcp_secret` remains whatever was already configured on txcloud.

---

### Task 10: Verify MCP Runtime Connectivity

**Files:**
- No tracked file changes.

- [ ] **Step 1: Inspect MCP container logs**

Run:

```powershell
ssh -o BatchMode=yes txcloud "sudo docker logs --tail=40 agent-mcp-csdn; sudo docker logs --tail=40 agent-mcp-amap; sudo docker logs --tail=40 agent-mcp-email; sudo docker logs --tail=40 agent-mcp-bocha"
```

Expected: each service shows Spring Boot startup complete and no fatal startup exception.

- [ ] **Step 2: Test internal network ports from backend network namespace**

Run:

```powershell
ssh -o BatchMode=yes txcloud "sudo docker run --rm --network agent-network curlimages/curl:8.8.0 -sS -o /dev/null -w 'csdn=%{http_code}\n' http://agent-mcp-csdn:9001/sse; sudo docker run --rm --network agent-network curlimages/curl:8.8.0 -sS -o /dev/null -w 'amap=%{http_code}\n' http://agent-mcp-amap:9003/sse; sudo docker run --rm --network agent-network curlimages/curl:8.8.0 -sS -o /dev/null -w 'email=%{http_code}\n' http://agent-mcp-email:9004/sse; sudo docker run --rm --network agent-network curlimages/curl:8.8.0 -sS -o /dev/null -w 'bocha=%{http_code}\n' http://agent-mcp-bocha:9005/sse"
```

Expected: each command reaches the service. HTTP code may be `200`, `405`, or another MCP SSE-specific response, but must not be DNS failure or connection refused.

- [ ] **Step 3: Recheck full agent container status**

Run:

```powershell
ssh -o BatchMode=yes txcloud "sudo docker ps -a --filter name=agent --format '{{.Names}} {{.Status}} {{.Ports}}'"
```

Expected: four MCP containers are running. Existing `agent-backend` may still be affected by the separate pgvector issue if `agent-pgvector` is stopped.

---

### Task 11: Final Local Verification And Status

**Files:**
- Uses all modified files.

- [ ] **Step 1: Sync CodeGraph**

Run: `codegraph sync`

Expected: command exits `0`.

- [ ] **Step 2: Verify Compose syntax one final time**

Run: `docker compose -f backend/docs/docker/docker-compose.yml config --quiet`

Expected: command exits `0`.

- [ ] **Step 3: Capture working tree status**

Run: `git status --short`

Expected: output includes the intended MCP deployment files and any pre-existing unrelated changes. Do not commit.

---

## Self-Review

- Spec coverage: Docker images, Compose private networking, txcloud upload/load, DB routing updates, no public MCP ports, and secret handling are covered.
- Placeholder scan: no unresolved implementation placeholders. Secret examples are intentionally not real credentials and are not applied by this plan.
- Type consistency: service names, image names, ports, and `mcp_id` values match the existing MCP modules and `ai_mcp` records.
- User constraints: no worktree and no commit steps are included because the user required current-workspace changes and no commits unless explicitly requested.
