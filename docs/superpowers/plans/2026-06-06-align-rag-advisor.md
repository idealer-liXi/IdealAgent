# Align RAG Advisor With MiniAgent Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make IdealAgent RAG Advisor behave like MiniAgent's Client-bound RAG advisor and make the UI help configure `ragTag` as a `knowledge` filter.

**Architecture:** Keep IdealAgent's existing `RuntimeMessageBuilder` manual augmentation path, but treat a bound Rag Advisor with `filterExpression` as sufficient to enable RAG even when the request does not provide `ragTag`. When a request `ragTag` is present, continue applying it as the base knowledge filter. Add frontend Advisor helper fields to generate `{"topK":4,"filterExpression":"knowledge == '<ragTag>'"}`.

**Tech Stack:** Java 21, Spring Boot, JUnit 5, AssertJ, Vue 3, Vite.

---

### Task 1: Runtime RAG Advisor Default Filter

**Files:**
- Modify: `backend/ideal-agent-domain/src/main/java/com/idealagent/domain/ai/service/chat/RuntimeMessageBuilder.java`
- Test: `backend/ideal-agent-domain/src/test/java/com/idealagent/domain/chat/service/ChatServiceTest.java`

- [ ] Add a failing test showing a Client-bound Rag Advisor with `filterExpression = "knowledge == 'project-docs'"` calls `augmentRagMessage` even when request `ragTag` is absent.
- [ ] Update `RuntimeMessageBuilder` to derive effective `ragTag`, `topK`, and `filterExpression` from the bound Rag Advisor.
- [ ] Preserve request `ragTag` priority when present.
- [ ] Run `mvn -q -pl ideal-agent-domain -am "-Dtest=ChatServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`.

### Task 2: Advisor UI ragTag Helper

**Files:**
- Modify: `frontend/src/components/AiConfig.vue`
- Modify: `frontend/src/components/AgentCanvas.vue`

- [ ] Load `/ai/rag/tags` in Advisor config UI.
- [ ] When Advisor type is `Rag`, expose `ragTag` and `topK` helper controls.
- [ ] Generate Advisor content as `{"topK":4,"filterExpression":"knowledge == '<ragTag>'"}`.
- [ ] Update Canvas Advisor node form to use the same helper behavior.
- [ ] Run `npm run build`.

### Task 3: Final Verification

**Files:**
- No source edits unless verification fails.

- [ ] Run focused backend tests.
- [ ] Run `mvn -q test`.
- [ ] Run `mvn -q -pl ideal-agent-app -am -DskipTests package`.
- [ ] Run `npm run build`.
- [ ] Run `codegraph sync` and `codegraph status`.

No commit is part of this plan because the user has not requested a commit.
