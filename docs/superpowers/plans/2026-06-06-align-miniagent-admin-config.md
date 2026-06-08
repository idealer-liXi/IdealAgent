# Align MiniAgent Admin Config Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Align IdealAgent admin Agent, Flow, and CONFIG CANVAS behavior with MiniAgent's strategy-constrained configuration workflow.

**Architecture:** Agent creation generates IDs server-side. Flow remains a strategy slot binding (`agentId/clientId/clientRole/userPrompt/flowSeq`) and is edited from the Canvas Client modal, not as a free-form graph. Canvas edits nodes and dependency relations while enforcing `step/loop/react` role constraints.

**Tech Stack:** Java 21, Spring Boot, MyBatis, JUnit 5, Mockito, Vue 3, Vite, Vue Flow, CodeGraph.

---

### Task 1: Backend Agent ID Generation

**Files:**
- Modify: `backend/ideal-agent-domain/src/main/java/com/idealagent/domain/ai/service/agent/AgentManagementService.java`
- Test: `backend/ideal-agent-domain/src/test/java/com/idealagent/domain/ai/service/agent/AgentManagementServiceTest.java`

- [ ] Add a failing test proving `createAgent` accepts blank `agentId` and returns a generated `agent_...` ID.
- [ ] Implement `generateAgentId()` in `AgentManagementService` using `UUID`.
- [ ] Keep update behavior path-ID based and do not allow form payload to override the path ID.
- [ ] Run `mvn -q -pl ideal-agent-domain -am "-Dtest=AgentManagementServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`.

### Task 2: Backend Canvas Strategy Guards

**Files:**
- Modify: `backend/ideal-agent-domain/src/main/java/com/idealagent/domain/ai/service/agent/AdminCanvasService.java`
- Modify: `backend/ideal-agent-infrastructure/src/main/java/com/idealagent/infrastructure/repository/AgentManagementRepository.java`
- Test: `backend/ideal-agent-domain/src/test/java/com/idealagent/domain/ai/service/agent/AdminCanvasServiceTest.java`
- Test: `backend/ideal-agent-infrastructure/src/test/java/com/idealagent/infrastructure/repository/AgentManagementRepositoryTest.java`

- [ ] Add tests for rejecting `step` roles outside `inspector/planner/runner/replier`.
- [ ] Add tests for rejecting flow sequence outside the strategy role count.
- [ ] Make `CanvasRelationDTO` support `clientRole`, `userPrompt`, and `flowSeq` for `Agent/Client -> Client` writes.
- [ ] Save Flow relation with explicit role, prompt, seq instead of defaulting to `auto/nextSeq`.
- [ ] Run focused domain and infrastructure tests.

### Task 3: Frontend Agent Form

**Files:**
- Modify: `frontend/src/components/AgentAdmin.vue`

- [ ] Remove editable Agent ID from create form.
- [ ] Show generated Agent ID as read-only when editing.
- [ ] Send blank `agentId` only on create; use route/path ID for update.
- [ ] Run `npm run build`.

### Task 4: Frontend Flow Detail

**Files:**
- Modify: `frontend/src/components/AgentFlow.vue`

- [ ] Load Canvas graph/client detail in Flow page.
- [ ] Show each slot's Client, API, Model, MCP, Advisor, Prompt, and Flow Prompt like MiniAgent.
- [ ] Keep Flow as a strategy-slot overview, not the primary editor.
- [ ] Run `npm run build`.

### Task 5: Frontend CONFIG CANVAS Core Editing

**Files:**
- Modify: `frontend/src/components/AgentCanvas.vue`

- [ ] Add “新增节点” selector for Client/Model/API/Prompt/Advisor/MCP.
- [ ] Add draft nodes that must connect from an existing node before saving.
- [ ] Add double-click edit modal for existing nodes.
- [ ] For Client modal, edit Client fields plus current Agent Flow fields: Flow Seq, Client Role, User Prompt.
- [ ] For Client modal, edit Prompt/Advisor/MCP dependencies.
- [ ] Enforce allowed strategy role and slot count in UI before API calls.
- [ ] Run `npm run build`.

### Task 6: Final Verification

**Files:**
- No source edits unless verification fails.

- [ ] Run focused backend tests.
- [ ] Run `mvn -q test`.
- [ ] Run `mvn -q -pl ideal-agent-app -am -DskipTests package`.
- [ ] Run `npm run build`.
- [ ] Run `codegraph sync` and `codegraph status`.
- [ ] Inspect `git status --short` without committing.

No commit is part of this plan because the user has not requested a commit.
