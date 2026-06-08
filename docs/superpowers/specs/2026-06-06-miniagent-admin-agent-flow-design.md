# MiniAgent Admin Agent/Flow Design

Date: 2026-06-06

## Goal

Align IdealAgent's management-side Agent and Flow configuration with MiniAgent before designing Workspace. The admin experience should expose the full configuration surface: Agent CRUD, Flow role-slot management, and a CONFIG CANVAS for visual Client/Model/API/Prompt/Advisor/MCP binding.

## Scope

In scope:

- Replace the current mixed `AgentAdmin.vue` experience with MiniAgent-style management pages.
- Migrate `ai_flow` to MiniAgent-style semantics: `agent_id + client_id + client_role + user_prompt + flow_seq`.
- Update Work runtime to read node prompt text from `ai_flow.user_prompt`.
- Implement management-side CONFIG CANVAS backed by existing database relations, not by persisted canvas layout.
- Update tests, seed data, schema docs, and CodeGraph index after changes.

Out of scope:

- Workspace / Studio simplified customer flow. That comes after admin configuration is aligned.
- Persisted free-form graph layouts. Canvas layout is derived from data.
- New permission system. Admin endpoints stay under the existing authenticated `/ai` application surface for this phase.

## Current State

IdealAgent currently has:

- `frontend/src/components/AgentAdmin.vue`: a single page combining Agent list/form and Flow table/form.
- `AiAgentController`: `/ai/agents`, `/ai/flows`, and `/ai/flow-options` CRUD endpoints.
- `ai_flow` schema with `flow_id`, `role_type`, `sort_order`, and `flow_status`.
- Flow prompt binding through `ai_config` with `owner_type = 'flow'` and `config_type = 'prompt'`.

MiniAgent's management model differs:

- Agent stores metadata only.
- Flow rows are agent-client role steps, ordered by `flow_seq`.
- Node prompt text is stored directly as `ai_flow.user_prompt`.
- Client dependencies are configured through Client, Model, API, and Config relations.
- CONFIG CANVAS is a visual editor over those relations.

## Data Model

Migrate `ai_flow` to MiniAgent-style fields:

```text
agent_id
client_id
client_role
user_prompt
flow_seq
```

The row identity for management operations becomes the pair:

```text
agent_id + client_id
```

If a technical primary key remains as `id`, it is internal only. The admin API should not require `flow_id`.

Flow prompt semantics:

- `ai_flow.user_prompt` is the role node prompt used during Work execution.
- `client -> prompt` in `ai_config` remains the ChatClient system prompt binding.
- `flow -> prompt` in `ai_config` is removed from the management/runtime path.

Existing `ai_config` remains for:

```text
client -> prompt
client -> advisor
client -> mcp
```

## Backend API

Use MiniAgent-style admin endpoints under `/ai/admin`:

```text
GET    /ai/admin/agents
POST   /ai/admin/agents
PUT    /ai/admin/agents/{agentId}
PATCH  /ai/admin/agents/{agentId}/status
DELETE /ai/admin/agents/{agentId}
```

Flow endpoints:

```text
GET    /ai/admin/flows/agents
GET    /ai/admin/flows/{agentId}
POST   /ai/admin/flows
PUT    /ai/admin/flows
DELETE /ai/admin/flows
```

Flow DTO:

```json
{
  "originAgentId": "agent_x",
  "originClientId": "client_x",
  "agentId": "agent_x",
  "clientId": "client_x",
  "clientRole": "planner",
  "userPrompt": "...",
  "flowSeq": 2
}
```

`PUT /ai/admin/flows` uses `originAgentId + originClientId` to locate the existing row. `DELETE /ai/admin/flows` accepts `agentId + clientId` query parameters or request body fields and deletes that unique Flow row.

Canvas support endpoints:

```text
GET    /ai/admin/canvas/{agentId}
POST   /ai/admin/canvas/node
PUT    /ai/admin/canvas/node
POST   /ai/admin/canvas/relation
DELETE /ai/admin/canvas/relation
```

Canvas relations map to existing persisted data:

```text
Agent -> Client       ai_flow
Client -> Client      create another ai_flow row for the same Agent, with the next flow_seq
Client -> Model       ai_client.model_id
Model -> API          ai_model.api_id
Client -> Prompt      ai_config owner=client type=prompt
Client -> Advisor     ai_config owner=client type=advisor
Client -> MCP         ai_config owner=client type=mcp
```

The existing `/ai/agents` and `/ai/flows` endpoints do not need to remain the primary API. They may be removed or left unused if removing them creates avoidable risk.

## Runtime

Work execution reads enabled Flow rows by `agent_id`, ordered by `flow_seq ASC`.

Each strategy node uses:

```text
flow.clientRole
flow.userPrompt
flow.clientId
flow.flowSeq
```

Role mappings:

```text
step:  inspector -> planner -> runner -> replier
loop:  analyzer -> performer -> supervisor -> summarizer
react: observer -> reasoner -> actor -> evaluator
```

Execution behavior:

- The node prompt comes from `ai_flow.user_prompt`.
- The ChatClient comes from `flow.client_id`.
- Client Prompt, Memory Advisor, RAG Advisor, and MCP still resolve through Client-level `ai_config` bindings.
- The current `RuntimeMessageBuilder` remains the shared Chat/Agent augmentation path.

## Frontend

Replace the current one-page Agent admin with three MiniAgent-style views:

```text
/agents
/agents/flow
/agents/canvas?agentId=xxx
```

### Agent Management

Fields:

```text
Agent ID
Name
Strategy Type: step / loop / react
Description
Model ID
Template ID
Status
Actions
```

Actions:

```text
create
edit
delete
toggle status
open Flow management for selected Agent
```

### Flow Management

The default view is an Agent card grid. Selecting an Agent opens a role-slot view.

Role slots:

```text
step: INSPECTOR -> PLANNER -> RUNNER -> REPLIER
loop: ANALYZER -> PERFORMER -> SUPERVISOR -> SUMMARIZER
react: OBSERVER -> REASONER -> ACTOR -> EVALUATOR
```

Each slot displays:

```text
Client ID
Model
API
MCP list
Advisor list
Prompt list
Flow Prompt summary
```

Actions:

```text
view prompt setting
open CONFIG CANVAS
return to Agent grid
refresh
```

### CONFIG CANVAS

Use `@vue-flow/core` to mirror MiniAgent's visual editor.

Node types:

```text
Agent
Client
Model
API
Prompt
Advisor
MCP
```

Allowed connections:

```text
Agent -> Client
Client -> Client
Client -> Model
Model -> API
Client -> Prompt
Client -> Advisor
Client -> MCP
```

Supported operations:

```text
render current configuration graph
double-click node to edit
add draft node
drag from existing node to draft node
save node
save Flow fields for Client nodes
add/update/delete/toggle Client dependency config
```

The canvas does not persist positions. It rebuilds layout from Agent, Flow, Client, Model, API, Prompt, Advisor, and MCP data.

## Validation Rules

- `agentType` must be `step`, `loop`, or `react`.
- `clientRole` must match the Agent type's allowed role list.
- `flowSeq` must be at least 1.
- `agentId` and `clientId` are required for Flow.
- A Flow row is unique by `agentId + clientId`.
- A Client can bind one Model.
- A Model can bind one API.
- Client Prompt/Advisor/MCP config values must reference existing records when feasible.
- Deleting an Agent should delete or reject dependent Flow rows according to the existing repository safety pattern. Prefer explicit rejection unless current code already performs cascading cleanup for that entity.

## Testing

Backend focused tests:

```text
AgentManagementServiceTest
AgentManagementRepositoryTest
WorkAgentRepositoryTest
ExecuteStepStrategyTest
MiniAgentParityStrategyTest
```

Expected coverage:

- Agent CRUD validation.
- MiniAgent-style Flow DTO validation.
- `clientRole` compatibility with `agentType`.
- `flowSeq` ordering.
- Repository writes `agentId/clientId/clientRole/userPrompt/flowSeq`.
- Runtime uses `userPrompt`, not flow prompt config.
- Client-level Prompt/Advisor/MCP still apply to Work execution.
- Canvas relation operations write the expected underlying tables.

Verification commands:

```powershell
mvn -q -pl ideal-agent-domain -am "-Dtest=AgentManagementServiceTest,AgentManagementRepositoryTest,WorkAgentRepositoryTest,ExecuteStepStrategyTest,MiniAgentParityStrategyTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -q test
mvn -q -pl ideal-agent-app -am -DskipTests package
npm run build
```

## CodeGraph

CodeGraph has been re-indexed for IdealAgent and should be used for structural lookups during implementation.

Before editing:

```powershell
codegraph query <symbol>
codegraph context "<task>"
```

After editing:

```powershell
codegraph sync
codegraph status
```

## Implementation Notes

- Keep changes incremental and test-first.
- Prefer adapting existing IdealAgent repository/service boundaries instead of creating parallel admin stacks unless necessary.
- Preserve the existing design language where practical, but make the management flow match MiniAgent's structure.
- Do not implement Workspace in this phase.
- Do not commit changes unless explicitly requested by the user.
