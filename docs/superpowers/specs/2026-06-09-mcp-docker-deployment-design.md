# MCP Docker Deployment Design

## Goal

Deploy the four non-WeCom MCP SSE services to txcloud as Docker containers managed by the existing IdealAgent Compose stack.

Services in scope:

- `mcp-server-csdn` on port `9001`
- `mcp-server-amap` on port `9003`
- `mcp-server-email` on port `9004`
- `mcp-server-bocha` on port `9005`

`mcp-server-wecom` is explicitly out of scope.

## Recommended Approach

Use Docker images and `docker compose`, not manually uploaded jars.

Each MCP service remains an independent Spring Boot application and gets its own image/container:

- `agent-mcp-csdn:latest` / `agent-mcp-csdn`
- `agent-mcp-amap:latest` / `agent-mcp-amap`
- `agent-mcp-email:latest` / `agent-mcp-email`
- `agent-mcp-bocha:latest` / `agent-mcp-bocha`

The containers join the existing `agent-network`. They are not exposed to the public internet. `agent-backend` reaches them by Docker DNS name.

## Network And Routing

Current DB seed uses localhost URLs such as `http://localhost:9003`. That is correct for local manual runs, but wrong inside Docker because `localhost` from `agent-backend` points to the backend container itself.

For Docker deployment, update txcloud `ai_mcp.mcp_config` to:

- `mcp_csdn`: `{"baseUri":"http://agent-mcp-csdn:9001","sseEndpoint":"/sse","timeoutMinutes":3}`
- `mcp_amap`: `{"baseUri":"http://agent-mcp-amap:9003","sseEndpoint":"/sse","timeoutMinutes":3}`
- `mcp_qq_smtp`: `{"baseUri":"http://agent-mcp-email:9004","sseEndpoint":"/sse","timeoutMinutes":5}`
- `mcp_bocha`: `{"baseUri":"http://agent-mcp-bocha:9005","sseEndpoint":"/sse","timeoutMinutes":3}`

The Compose services should use `expose` or no `ports` mapping for MCP services. No public security-group changes are required for ports `9001`, `9003`, `9004`, or `9005`.

## Secrets

MCP credentials stay in `ai_mcp.mcp_secret`. They are sent by `agent-backend` as Base64 JSON in the configured `X-IdealAgent-Mcp-Secret` header.

Required private secret formats:

- Amap: `{"key":"<amap-api-key>"}`
- Bocha: `{"apiKey":"<bocha-api-key>"}`
- CSDN: `{"cookie":"<csdn-cookie>","categories":"<category>","tags":"<tag1,tag2>","coverUrl":"<cover-url>"}`
- Email: `{"smtpHost":"smtp.qq.com","smtpPort":"465","smtpUsername":"<email>","smtpPassword":"<smtp-auth-code>","fromAddress":"<email>","fromName":"IdealAgent"}`

These secrets must not be committed to Git or written into seed SQL. They should be applied only on txcloud through a private SQL update or admin UI.

## Components

Implementation should add a reusable MCP Dockerfile under `mcp/` unless per-module Dockerfiles are required by build constraints. The image should run a packaged Spring Boot jar with Java 17.

Compose should add four services that:

- use `agent-*` image and container names
- run on the existing `agent-network`
- use `restart: unless-stopped`
- do not publish host ports
- are optionally included in `agent-backend.depends_on` with `service_started`, not a hard health requirement unless a reliable MCP health endpoint exists

## Deployment Flow

1. Build/package the four MCP jars locally.
2. Build four Docker images locally.
3. Save images to tar archives and upload to txcloud.
4. Load images on txcloud with `sudo docker load`.
5. Upload updated Compose files.
6. Run `sudo docker compose up -d` for the four MCP services and backend if needed.
7. Update txcloud `ai_mcp.mcp_config` to Docker DNS names.
8. Apply private `mcp_secret` values on txcloud only.
9. Verify containers are running and backend can initialize MCP clients.

## Error Handling

If an MCP container is down, the backend should fail only when a flow tries to initialize that specific MCP client. It should not require MCP services to be public or rely on host port mappings.

If secrets are missing, the MCP tool should return its existing credential-missing response. This is acceptable during deployment verification before private secrets are applied.

## Testing And Verification

Local verification:

- build all four MCP modules with Maven
- build the four Docker images
- run `docker compose config --quiet`

txcloud verification:

- confirm no `agent-mcp-*` container name conflicts before starting
- confirm no public port mapping exists for MCP services
- confirm each `agent-mcp-*` container is running
- query `ai_mcp.mcp_config` and verify Docker DNS base URIs
- test backend-to-MCP connectivity from inside `agent-backend` or through a backend workflow

## Scope Boundaries

This design does not deploy WeCom.

This design does not place real API keys, cookies, or SMTP passwords in tracked files.

This design does not expose MCP ports to the public internet.

This design does not solve the existing pgvector startup dependency; that remains a separate backend availability issue.

## Self-Review

- Placeholder scan: no unresolved placeholders except intentional secret examples.
- Internal consistency: Docker DNS names, container names, and ports match the existing MCP module configuration.
- Scope check: focused on four MCP services only; WeCom and pgvector optionality are excluded.
- Ambiguity check: public exposure, secret storage, and DB baseUri updates are explicit.
