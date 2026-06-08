# MiniAgent Strategy Template Loading Design

Date: 2026-06-07

## Goal

Align IdealAgent Agent creation with MiniAgent Workspace Agent creation for strategy prompt templates. Creating a `step`, `loop`, or `react` Agent should generate role Clients, role system Prompts, Client bindings, and Flow user prompts from MiniAgent-style resource files instead of hard-coded fallback strings.

## Scope

In scope:

- Copy MiniAgent strategy prompt resources into IdealAgent under `backend/ideal-agent-app/src/main/resources/template`.
- Load templates by strategy, prompt kind, and role during `AgentManagementService.createAgent(...)`.
- Use `system-prompt/*.md` content for generated Client Prompt records.
- Use `user-prompt/*.md` content for generated Flow `userPrompt` values.
- Replace the first `%s` in each generated Flow `userPrompt` with a role boundary constraint, matching MiniAgent's creation-time constraint injection and leaving runtime placeholders intact.
- Keep the existing generated runtime skeleton: Work Client, Prompt, Client-Prompt binding, default Memory Advisor binding, and Agent Flow.
- Preserve fallback prompt generation if a resource is missing so Agent creation still fails only for validation or persistence errors.
- Cover behavior with focused domain tests.

Out of scope:

- Full `ai_template` CRUD.
- Plaza publish, fork, favorite, comment, or Template marketplace behavior.
- Workspace/Studio page implementation.
- Database schema changes for template snapshots.

## Design

Add a small template-loading component in the domain Agent package. It will read classpath resources using Spring's `ResourceLoader` so resources packaged in the app module are available at runtime while the domain module keeps a simple dependency boundary.

Template paths follow MiniAgent:

```text
classpath:template/{strategy}/system-prompt/{Role}.md
classpath:template/{strategy}/user-prompt/{Role}.md
```

Role file names use MiniAgent's title-case names, for example `Inspector.md`, `Planner.md`, `Analyzer.md`, and `Observer.md`. Agent runtime roles remain normalized lower-case values such as `inspector` and `planner`.

`AgentManagementService` will delegate prompt text lookup to the loader. If the loader returns no content, the existing minimal fallback strings remain in place. This keeps the change isolated and avoids widening Agent creation failure cases.

## Data Flow

When an Agent is created:

1. Validate Agent metadata and required `modelId`.
2. Save the Agent.
3. Resolve ordered strategy roles from existing `ROLE_ORDER`.
4. For each role, create a Work Client bound to the selected Model.
5. Load `system-prompt` template for the role and create a Prompt record with that content.
6. Bind Client to Prompt and default Memory Advisor.
7. Load `user-prompt` template for the role.
8. Replace its first `%s` with a deterministic role boundary constraint derived from Agent description and role.
9. Create the Flow row with the resulting prompt content.

## Testing

Focused tests should prove:

- Agent creation uses MiniAgent template content for generated Prompt records.
- Agent creation uses MiniAgent template content for generated Flow user prompts.
- Generated Flow user prompts have the first `%s` replaced and retain the remaining runtime placeholders.
- Missing template resources fall back to the existing minimal prompt strings.

Full verification remains the existing sequence: focused Maven tests, full Maven tests, app package, frontend build, and CodeGraph sync/status when available.

## Self-Review

- No placeholders remain.
- Scope excludes Template marketplace features explicitly.
- Resource paths match MiniAgent's directory contract.
- User prompt placeholder semantics avoid runtime `formatted(...)` argument-count failures.
- Runtime behavior remains compatible with the existing generated Client/Prompt/Flow skeleton.
