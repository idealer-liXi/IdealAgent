# Model Client Type Boundary Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let Config/Model define `chat` or `work` usage and enforce that Chat uses only chat clients while Agent uses only work clients.

**Architecture:** `ai_model.model_type` is the source of intent. `ai_client.client_type` follows the selected model type when saving client config. Chat lists and runs only `chat` clients; Agent Flow/Canvas options and saves only accept `work` clients.

**Tech Stack:** Spring Boot, MyBatis, JUnit, Vue 3, Vite.

---

### Task 1: Backend Type Enforcement

**Files:**
- Modify: `backend/ideal-agent-domain/src/test/java/com/idealagent/domain/chat/service/ChatServiceTest.java`
- Modify: `backend/ideal-agent-domain/src/test/java/com/idealagent/domain/ai/service/agent/AgentManagementServiceTest.java`
- Modify: `backend/ideal-agent-domain/src/main/java/com/idealagent/domain/ai/service/chat/ChatService.java`
- Modify: `backend/ideal-agent-domain/src/main/java/com/idealagent/domain/ai/service/agent/AgentManagementService.java`
- Modify: `backend/ideal-agent-domain/src/main/java/com/idealagent/domain/ai/repository/IAgentManagementRepository.java`
- Modify: `backend/ideal-agent-infrastructure/src/main/java/com/idealagent/infrastructure/repository/AgentManagementRepository.java`

- [ ] Add failing tests: Chat list excludes work clients; Chat send rejects work client; Agent flow rejects non-work client.
- [ ] Run focused tests and confirm failures.
- [ ] Add repository lookup for client config and enforce type checks.
- [ ] Run focused tests and confirm pass.

### Task 2: Config Save And UI Filtering

**Files:**
- Modify: `frontend/src/components/AiConfig.vue`
- Modify: `frontend/src/components/AgentCanvas.vue`

- [ ] Expose Model type selector with `chat` and `work`.
- [ ] When Client selects a Model, copy model type into Client type and disable manual type drift.
- [ ] Filter Client model dropdown by selected client type.
- [ ] Filter Agent Canvas client choices to `work` clients.

### Task 3: Verification

- [ ] Run focused backend tests.
- [ ] Run `mvn -q test`.
- [ ] Run `mvn -q -pl ideal-agent-app -am -DskipTests package`.
- [ ] Run `npm run build`.
- [ ] Run `codegraph sync` and `codegraph status`.
- [ ] Run `git status --short`.

No commit is created unless the user asks for one.
