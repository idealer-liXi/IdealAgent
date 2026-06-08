# MiniAgent Strategy Template Loading Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Generate Agent role Prompt and Flow prompt content from MiniAgent-compatible classpath template resources.

**Architecture:** Add a focused `StrategyPromptTemplateService` in the Agent domain service package. `AgentManagementService` delegates system/user prompt lookup to it, replaces the first user-prompt `%s` with a role boundary constraint, and keeps existing fallback strings when resources are absent. MiniAgent template files are copied into the app module resources so packaged runtime has the same template contract.

**Tech Stack:** Java 17, Spring `ResourceLoader`, Spring Boot Maven modules, JUnit 5, AssertJ, Vue frontend build for final verification.

---

## File Structure

- Create: `backend/ideal-agent-domain/src/main/java/com/idealagent/domain/ai/service/agent/StrategyPromptTemplateService.java`
- Modify: `backend/ideal-agent-domain/src/main/java/com/idealagent/domain/ai/service/agent/AgentManagementService.java`
- Modify: `backend/ideal-agent-domain/src/test/java/com/idealagent/domain/ai/service/agent/AgentManagementServiceTest.java`
- Create: `backend/ideal-agent-domain/src/test/resources/template/step/system-prompt/Inspector.md`
- Create: `backend/ideal-agent-domain/src/test/resources/template/step/user-prompt/Inspector.md`
- Create: `backend/ideal-agent-app/src/main/resources/template/**`

## Task 1: RED Test For Template-Backed Agent Creation

**Files:**
- Modify: `backend/ideal-agent-domain/src/test/java/com/idealagent/domain/ai/service/agent/AgentManagementServiceTest.java`
- Create: `backend/ideal-agent-domain/src/test/resources/template/step/system-prompt/Inspector.md`
- Create: `backend/ideal-agent-domain/src/test/resources/template/step/user-prompt/Inspector.md`

- [ ] **Step 1: Add test resources**

`backend/ideal-agent-domain/src/test/resources/template/step/system-prompt/Inspector.md`:

```markdown
Template system prompt for Inspector: %s
```

`backend/ideal-agent-domain/src/test/resources/template/step/user-prompt/Inspector.md`:

```markdown
Template user prompt for Inspector: %s
```

- [ ] **Step 2: Update test construction and assertions**

Use `new StrategyPromptTemplateService(new DefaultResourceLoader())` when constructing `AgentManagementService`. Add a test that creates a `step` Agent, then asserts `inspector_prompt` uses `Template system prompt for Inspector: %s`, the `inspector` Flow replaces the first user-prompt `%s` with a role boundary constraint while retaining the remaining `%s`, and the `planner` role still uses fallback content.

- [ ] **Step 3: Run RED test**

Run from `D:\projects\ai-agent\IdealAgent\backend`:

```powershell
mvn -q -pl ideal-agent-domain -am "-Dtest=AgentManagementServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: compilation fails because `StrategyPromptTemplateService` does not exist or constructor signature is not updated.

## Task 2: Implement Template Loader

**Files:**
- Create: `backend/ideal-agent-domain/src/main/java/com/idealagent/domain/ai/service/agent/StrategyPromptTemplateService.java`

- [ ] **Step 1: Add loader service**

Implement a Spring `@Service` with these methods:

```java
String systemPrompt(String strategy, String role)
String userPrompt(String strategy, String role)
```

The service reads resources from:

```text
classpath:template/{strategy}/system-prompt/{Role}.md
classpath:template/{strategy}/user-prompt/{Role}.md
```

It returns an empty string when the resource is missing or unreadable.

- [ ] **Step 2: Run focused tests**

Run:

```powershell
mvn -q -pl ideal-agent-domain -am "-Dtest=AgentManagementServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: tests still fail until `AgentManagementService` delegates to the loader.

## Task 3: Wire Loader Into Agent Creation

**Files:**
- Modify: `backend/ideal-agent-domain/src/main/java/com/idealagent/domain/ai/service/agent/AgentManagementService.java`
- Modify: `backend/ideal-agent-domain/src/test/java/com/idealagent/domain/ai/service/agent/AgentManagementServiceTest.java`

- [ ] **Step 1: Inject loader**

Add `StrategyPromptTemplateService templateService` to the constructor and assign it to a final field.

- [ ] **Step 2: Replace hard-coded prompt generation**

Change `defaultSystemPrompt(agent, role)` to first read `templateService.systemPrompt(agent.agentType(), role)`. If it has text, return it. Otherwise return the existing fallback string.

Change `defaultUserPrompt(agent, role)` to first read `templateService.userPrompt(agent.agentType(), role)`. If it has text, replace the first `%s` with a role boundary constraint and return the result. Otherwise return the existing fallback string.

- [ ] **Step 3: Run focused tests**

Run:

```powershell
mvn -q -pl ideal-agent-domain -am "-Dtest=AgentManagementServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: all `AgentManagementServiceTest` tests pass.

## Task 4: Add MiniAgent Template Resources

**Files:**
- Create: `backend/ideal-agent-app/src/main/resources/template/README.md`
- Create: `backend/ideal-agent-app/src/main/resources/template/step/system-prompt/Inspector.md`
- Create: `backend/ideal-agent-app/src/main/resources/template/step/system-prompt/Planner.md`
- Create: `backend/ideal-agent-app/src/main/resources/template/step/system-prompt/Runner.md`
- Create: `backend/ideal-agent-app/src/main/resources/template/step/system-prompt/Replier.md`
- Create: `backend/ideal-agent-app/src/main/resources/template/step/user-prompt/Inspector.md`
- Create: `backend/ideal-agent-app/src/main/resources/template/step/user-prompt/Planner.md`
- Create: `backend/ideal-agent-app/src/main/resources/template/step/user-prompt/Runner.md`
- Create: `backend/ideal-agent-app/src/main/resources/template/step/user-prompt/Replier.md`
- Create all matching `loop` and `react` system/user prompt files from MiniAgent.

- [ ] **Step 1: Copy exact MiniAgent resource contents**

Use the source files under `D:\projects\ai-agent\MiniAgent\backend\ai-agent-app\src\main\resources\template` and add the same tree under IdealAgent's app resources.

- [ ] **Step 2: Run app package**

Run:

```powershell
mvn -q -pl ideal-agent-app -am -DskipTests package
```

Expected: package succeeds and includes resources on the app classpath.

## Task 5: Final Verification

**Files:**
- No code changes in this task.

- [ ] **Step 1: Run focused domain tests**

```powershell
mvn -q -pl ideal-agent-domain -am "-Dtest=AgentManagementServiceTest,WorkServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: success.

- [ ] **Step 2: Run full backend tests**

```powershell
mvn -q test
```

Expected: success.

- [ ] **Step 3: Package app**

```powershell
mvn -q -pl ideal-agent-app -am -DskipTests package
```

Expected: success.

- [ ] **Step 4: Build frontend**

Run from `D:\projects\ai-agent\IdealAgent\frontend`:

```powershell
npm run build
```

Expected: success. Vite chunk-size warnings are acceptable when exit code is 0.

- [ ] **Step 5: Sync index if available**

```powershell
codegraph sync
codegraph status
```

Expected: status reports the index is up to date. If the command is unavailable, report that verification gap.

- [ ] **Step 6: Inspect status**

```powershell
git status --short
```

Expected: only intended files are modified or added. Do not commit.

## Self-Review

- Spec coverage: tasks cover test resources, loader, service wiring, first-placeholder replacement, production resources, and verification.
- Placeholder scan: no `TBD`, unresolved placeholders, or vague implementation steps remain.
- Type consistency: service and constructor names match the code changes planned for `AgentManagementService` and its test.
- User instruction: no worktree and no commit steps are included.
