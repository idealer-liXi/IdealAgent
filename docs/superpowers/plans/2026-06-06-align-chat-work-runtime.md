# Chat Work Runtime Alignment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Align IdealAgent Chat and Work behavior with MiniAgent: Chat uses request-level RAG/MCP choices, while Work uses only Agent selection and Agent Client configuration.

**Architecture:** Chat keeps using a runtime Client/ChatClient as the model execution wrapper, but RAG and MCP are chosen per chat request. Work requests select an Agent only; each Agent flow Client supplies Prompt/Advisor/MCP configuration, so Work no longer accepts a user-selected ragTag from the page.

**Tech Stack:** Spring Boot, JUnit, Vue 3, Vite.

---

### Task 1: Backend Runtime Semantics

**Files:**
- Modify: `backend/ideal-agent-domain/src/test/java/com/idealagent/domain/chat/service/ChatServiceTest.java`
- Modify: `backend/ideal-agent-domain/src/test/java/com/idealagent/domain/ai/service/agent/*StrategyTest.java` if Work tests assert request ragTag behavior
- Modify: `backend/ideal-agent-domain/src/main/java/com/idealagent/domain/ai/service/agent/*Strategy.java` only if Work still forwards request ragTag

- [ ] **Step 1: Write failing test**

Assert Work runtime does not use request-level ragTag and relies on client-bound advisor settings instead.

- [ ] **Step 2: Run focused test to verify RED**

Run: `mvn -q -pl ideal-agent-domain -am "-Dtest=ChatServiceTest,MiniAgentParityStrategyTest,ExecuteStepStrategyTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

- [ ] **Step 3: Implement minimal runtime change**

Remove request ragTag forwarding from Work execution if present. Keep Chat request-level `ragTag` and `mcpIdList` unchanged.

- [ ] **Step 4: Verify GREEN**

Run the same focused Maven command.

### Task 2: Frontend UX Alignment

**Files:**
- Modify: `frontend/src/components/Chat.vue`
- Modify: `frontend/src/components/Work.vue` or the current Work component
- Modify: `frontend/src/request/api.js` if Work payload includes `ragTag`

- [ ] **Step 1: Remove Work RAG selector**

Work page should let users choose Agent and enter task content only. Do not send `ragTag` in Work payload.

- [ ] **Step 2: Keep Chat request-level RAG/MCP**

Chat page should keep selecting model/chat client, optional knowledge tag, and optional MCP IDs.

- [ ] **Step 3: Verify build**

Run: `npm run build`

### Task 3: Final Verification

- [ ] Run: `mvn -q test`
- [ ] Run: `mvn -q -pl ideal-agent-app -am -DskipTests package`
- [ ] Run: `npm run build`
- [ ] Run: `codegraph sync`
- [ ] Run: `codegraph status`
- [ ] Run: `git status --short`

No commit is created unless the user asks for one.
