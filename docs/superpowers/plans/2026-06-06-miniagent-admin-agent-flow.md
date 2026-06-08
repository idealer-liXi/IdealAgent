# MiniAgent Admin Agent/Flow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild IdealAgent management-side Agent, Flow, and CONFIG CANVAS to match MiniAgent's admin configuration model before Workspace design.

**Architecture:** Migrate Flow from `flow_id/role_type/sort_order` to MiniAgent-style `agent_id/client_id/client_role/user_prompt/flow_seq`, adapt runtime to use `user_prompt`, and expose admin APIs that drive Agent list, role-slot Flow view, and Canvas relation editing. Canvas persists no layout; it renders and mutates existing Agent, Flow, Client, Model, API, Prompt, Advisor, MCP, and Config records.

**Tech Stack:** Java 21, Spring Boot, MyBatis, JUnit 5, Mockito, AssertJ, Vue 3, Vite, Tailwind, `@vue-flow/core`, MySQL schema docs, CodeGraph.

---

## File Structure

Backend data model and persistence:

- Modify: `backend/docs/mysql/ideal_agent_schema.sql` - change `ai_flow` columns and keys.
- Modify: `backend/docs/mysql/ideal_agent_seed.sql` - seed MiniAgent-style Flow rows with `user_prompt`.
- Modify: `backend/ideal-agent-infrastructure/src/main/java/com/idealagent/infrastructure/persistent/po/AiFlow.java` - replace old fields with `clientRole`, `userPrompt`, `flowSeq`.
- Modify: `backend/ideal-agent-infrastructure/src/main/java/com/idealagent/infrastructure/persistent/dao/IAiFlowDao.java` - query/update/delete by `agentId + clientId`.
- Modify: `backend/ideal-agent-infrastructure/src/main/resources/mapper/AiFlowDao.xml` - SQL for MiniAgent-style Flow.
- Test: `backend/ideal-agent-infrastructure/src/test/java/com/idealagent/infrastructure/schema/IdealAgentSchemaTest.java` - schema assertions.

Backend admin API:

- Modify: `backend/ideal-agent-domain/src/main/java/com/idealagent/domain/ai/model/dto/FlowManageDTO.java` - MiniAgent DTO.
- Modify: `backend/ideal-agent-domain/src/main/java/com/idealagent/domain/ai/model/vo/FlowManageVO.java` - MiniAgent VO.
- Modify: `backend/ideal-agent-domain/src/main/java/com/idealagent/domain/ai/repository/IAgentManagementRepository.java` - flow methods by composite key.
- Modify: `backend/ideal-agent-domain/src/main/java/com/idealagent/domain/ai/service/agent/AgentManagementService.java` - validation and flow management.
- Modify: `backend/ideal-agent-infrastructure/src/main/java/com/idealagent/infrastructure/repository/AgentManagementRepository.java` - persistence mapping.
- Modify: `backend/ideal-agent-trigger/src/main/java/com/idealagent/trigger/controller/AiAgentController.java` - `/ai/admin` endpoints.
- Test: `backend/ideal-agent-domain/src/test/java/com/idealagent/domain/ai/service/agent/AgentManagementServiceTest.java`.
- Test: `backend/ideal-agent-infrastructure/src/test/java/com/idealagent/infrastructure/repository/AgentManagementRepositoryTest.java`.
- Test: `backend/ideal-agent-app/src/test/java/com/idealagent/AiAgentControllerTest.java`.

Backend Canvas API:

- Create: `backend/ideal-agent-domain/src/main/java/com/idealagent/domain/ai/model/dto/CanvasNodeDTO.java`.
- Create: `backend/ideal-agent-domain/src/main/java/com/idealagent/domain/ai/model/dto/CanvasRelationDTO.java`.
- Create: `backend/ideal-agent-domain/src/main/java/com/idealagent/domain/ai/model/vo/CanvasGraphVO.java`.
- Create: `backend/ideal-agent-domain/src/main/java/com/idealagent/domain/ai/service/agent/AdminCanvasService.java`.
- Extend: `backend/ideal-agent-domain/src/main/java/com/idealagent/domain/ai/repository/IAgentManagementRepository.java` - canvas detail and relation methods.
- Extend: `backend/ideal-agent-infrastructure/src/main/java/com/idealagent/infrastructure/repository/AgentManagementRepository.java` - canvas graph assembly and relation writes.
- Modify: `backend/ideal-agent-trigger/src/main/java/com/idealagent/trigger/controller/AiAgentController.java` - canvas endpoints.
- Test: `backend/ideal-agent-domain/src/test/java/com/idealagent/domain/ai/service/agent/AdminCanvasServiceTest.java`.
- Test: `backend/ideal-agent-infrastructure/src/test/java/com/idealagent/infrastructure/repository/AgentManagementRepositoryTest.java`.

Runtime:

- Modify: `backend/ideal-agent-domain/src/main/java/com/idealagent/domain/ai/model/vo/AiFlowVO.java` - ensure MiniAgent fields.
- Modify: `backend/ideal-agent-infrastructure/src/main/java/com/idealagent/infrastructure/repository/WorkAgentRepository.java` - list enabled Flow by `flow_seq` and `user_prompt`.
- Modify: `backend/ideal-agent-domain/src/main/java/com/idealagent/domain/ai/service/work/*` only if names no longer compile.
- Test: `backend/ideal-agent-infrastructure/src/test/java/com/idealagent/infrastructure/repository/WorkAgentRepositoryTest.java`.
- Test: `backend/ideal-agent-domain/src/test/java/com/idealagent/domain/ai/service/work/step/ExecuteStepStrategyTest.java`.
- Test: `backend/ideal-agent-domain/src/test/java/com/idealagent/domain/ai/service/work/MiniAgentParityStrategyTest.java`.

Frontend:

- Modify: `frontend/src/router/router.js` - add Flow and Canvas routes.
- Modify: `frontend/src/components/Sidebar.vue` - surface Agent management routes if needed.
- Replace: `frontend/src/components/AgentAdmin.vue` - Agent CRUD table/grid.
- Create: `frontend/src/components/AgentFlow.vue` - MiniAgent-style Agent grid and role slots.
- Create: `frontend/src/components/AgentCanvas.vue` - CONFIG CANVAS using Vue Flow.
- Modify: `frontend/package.json` if `@vue-flow/core`, `@vue-flow/background`, `@vue-flow/controls`, and `@vue-flow/minimap` are missing.

Docs and tooling:

- Modify: `docs/superpowers/specs/2026-06-06-miniagent-admin-agent-flow-design.md` only if implementation reveals a spec mismatch.
- Run: `codegraph sync` after edits.
- Run: `codegraph status` before completion.

No commits are part of this plan because the user asked not to commit unless explicitly requested.

---

### Task 1: Migrate `ai_flow` Schema And Persistence

**Files:**
- Modify: `backend/docs/mysql/ideal_agent_schema.sql`
- Modify: `backend/docs/mysql/ideal_agent_seed.sql`
- Modify: `backend/ideal-agent-infrastructure/src/main/java/com/idealagent/infrastructure/persistent/po/AiFlow.java`
- Modify: `backend/ideal-agent-infrastructure/src/main/java/com/idealagent/infrastructure/persistent/dao/IAiFlowDao.java`
- Modify: `backend/ideal-agent-infrastructure/src/main/resources/mapper/AiFlowDao.xml`
- Test: `backend/ideal-agent-infrastructure/src/test/java/com/idealagent/infrastructure/schema/IdealAgentSchemaTest.java`

- [ ] **Step 1: Query current symbols with CodeGraph**

Run:

```powershell
codegraph query AiFlow
codegraph query IAiFlowDao
codegraph context "ai_flow schema migration"
```

Expected: CodeGraph lists `AiFlow`, `IAiFlowDao`, `AiFlowDao.xml`, and tests that mention Flow.

- [ ] **Step 2: Write failing schema test**

Add assertions to `IdealAgentSchemaTest` that read `backend/docs/mysql/ideal_agent_schema.sql` and require MiniAgent columns:

```java
@Test
void aiFlowUsesMiniAgentColumns() throws Exception {
    String schema = Files.readString(Path.of("..", "docs", "mysql", "ideal_agent_schema.sql"));
    assertThat(schema).contains("client_role VARCHAR(64) NOT NULL");
    assertThat(schema).contains("user_prompt TEXT NOT NULL");
    assertThat(schema).contains("flow_seq INT NOT NULL DEFAULT 1");
    assertThat(schema).contains("UNIQUE KEY uk_flow_agent_client (agent_id, client_id)");
    assertThat(schema).doesNotContain("flow_id VARCHAR(64) NOT NULL");
    assertThat(schema).doesNotContain("role_type VARCHAR(64) NOT NULL");
    assertThat(schema).doesNotContain("sort_order INT NOT NULL");
}
```

- [ ] **Step 3: Run schema test and verify failure**

Run:

```powershell
mvn -q -pl ideal-agent-infrastructure -Dtest=IdealAgentSchemaTest test
```

Expected: FAIL because `ai_flow` still contains old columns.

- [ ] **Step 4: Update schema**

Change `ai_flow` in `backend/docs/mysql/ideal_agent_schema.sql` to:

```sql
CREATE TABLE IF NOT EXISTS ai_flow (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  agent_id VARCHAR(64) NOT NULL,
  client_id VARCHAR(64) NOT NULL,
  client_role VARCHAR(64) NOT NULL,
  user_prompt TEXT NOT NULL,
  flow_seq INT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_flow_agent_client (agent_id, client_id),
  KEY idx_flow_agent (agent_id),
  KEY idx_flow_client (client_id),
  KEY idx_flow_seq (agent_id, flow_seq)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

- [ ] **Step 5: Update seed Flow rows**

Replace `INSERT INTO ai_flow` in `backend/docs/mysql/ideal_agent_seed.sql` with MiniAgent columns:

```sql
INSERT INTO ai_flow (agent_id, client_id, client_role, user_prompt, flow_seq)
VALUES
  ('agent_default_step', 'client_default_chat', 'inspector', '请分析用户任务：%s，并输出可用工具和约束 JSON 数组。', 1),
  ('agent_default_step', 'client_default_chat_planner', 'planner', '基于用户任务：%s 和审查结果：%s，输出步骤 JSON 数组。', 2),
  ('agent_default_step', 'client_default_chat_runner', 'runner', '执行步骤：%s。用户任务：%s。审查结果：%s。', 3),
  ('agent_default_step', 'client_default_chat_replier', 'replier', '汇总用户任务：%s 和执行历史：%s，输出最终 JSON 对象。', 4)
ON DUPLICATE KEY UPDATE client_role = VALUES(client_role), user_prompt = VALUES(user_prompt), flow_seq = VALUES(flow_seq);
```

If seed currently lacks the role-specific clients used above, use `client_default_chat` for all four rows in the first implementation pass, then let Canvas create role-specific clients later.

- [ ] **Step 6: Update `AiFlow` PO**

Replace fields in `AiFlow.java` with:

```java
private String agentId;
private String clientId;
private String clientRole;
private String userPrompt;
private Integer flowSeq;
```

Add getters and setters for those five fields. Remove `flowId`, `roleType`, `sortOrder`, `flowStatus`, `promptId`, and `promptContent` from this PO.

- [ ] **Step 7: Update `IAiFlowDao` methods**

Change `IAiFlowDao.java` to expose:

```java
List<AiFlow> listEnabledByAgentId(@Param("agentId") String agentId);

List<AiFlow> listByAgentId(@Param("agentId") String agentId);

AiFlow queryByAgentIdAndClientId(@Param("agentId") String agentId, @Param("clientId") String clientId);

int insert(AiFlow flow);

int update(@Param("originAgentId") String originAgentId, @Param("originClientId") String originClientId, @Param("flow") AiFlow flow);

int deleteByAgentIdAndClientId(@Param("agentId") String agentId, @Param("clientId") String clientId);

int deleteByAgentId(@Param("agentId") String agentId);
```

- [ ] **Step 8: Update MyBatis mapper**

Rewrite `AiFlowDao.xml` around MiniAgent columns:

```xml
<resultMap id="dataMap" type="com.idealagent.infrastructure.persistent.po.AiFlow">
    <result column="agent_id" property="agentId"/>
    <result column="client_id" property="clientId"/>
    <result column="client_role" property="clientRole"/>
    <result column="user_prompt" property="userPrompt"/>
    <result column="flow_seq" property="flowSeq"/>
</resultMap>

<select id="listEnabledByAgentId" resultMap="dataMap">
    SELECT agent_id, client_id, client_role, user_prompt, flow_seq
    FROM ai_flow
    WHERE agent_id = #{agentId}
    ORDER BY flow_seq ASC, id ASC
</select>

<select id="listByAgentId" resultMap="dataMap">
    SELECT agent_id, client_id, client_role, user_prompt, flow_seq
    FROM ai_flow
    WHERE agent_id = #{agentId}
    ORDER BY flow_seq ASC, id ASC
</select>

<select id="queryByAgentIdAndClientId" resultMap="dataMap">
    SELECT agent_id, client_id, client_role, user_prompt, flow_seq
    FROM ai_flow
    WHERE agent_id = #{agentId}
      AND client_id = #{clientId}
    LIMIT 1
</select>

<insert id="insert" parameterType="com.idealagent.infrastructure.persistent.po.AiFlow">
    INSERT INTO ai_flow (agent_id, client_id, client_role, user_prompt, flow_seq)
    VALUES (#{agentId}, #{clientId}, #{clientRole}, #{userPrompt}, #{flowSeq})
</insert>

<update id="update">
    UPDATE ai_flow
    SET agent_id = #{flow.agentId}, client_id = #{flow.clientId}, client_role = #{flow.clientRole}, user_prompt = #{flow.userPrompt}, flow_seq = #{flow.flowSeq}
    WHERE agent_id = #{originAgentId}
      AND client_id = #{originClientId}
</update>

<delete id="deleteByAgentIdAndClientId">
    DELETE FROM ai_flow
    WHERE agent_id = #{agentId}
      AND client_id = #{clientId}
</delete>

<delete id="deleteByAgentId">
    DELETE FROM ai_flow WHERE agent_id = #{agentId}
</delete>
```

- [ ] **Step 9: Run schema test and compile infrastructure**

Run:

```powershell
mvn -q -pl ideal-agent-infrastructure -Dtest=IdealAgentSchemaTest test
mvn -q -pl ideal-agent-infrastructure -DskipTests compile
```

Expected: schema test passes; compile errors are acceptable only if they point to callers that are updated in later tasks.

---

### Task 2: Convert Agent Management Service And Repository To MiniAgent Flow DTO

**Files:**
- Modify: `backend/ideal-agent-domain/src/main/java/com/idealagent/domain/ai/model/dto/FlowManageDTO.java`
- Modify: `backend/ideal-agent-domain/src/main/java/com/idealagent/domain/ai/model/vo/FlowManageVO.java`
- Modify: `backend/ideal-agent-domain/src/main/java/com/idealagent/domain/ai/repository/IAgentManagementRepository.java`
- Modify: `backend/ideal-agent-domain/src/main/java/com/idealagent/domain/ai/service/agent/AgentManagementService.java`
- Modify: `backend/ideal-agent-infrastructure/src/main/java/com/idealagent/infrastructure/repository/AgentManagementRepository.java`
- Test: `backend/ideal-agent-domain/src/test/java/com/idealagent/domain/ai/service/agent/AgentManagementServiceTest.java`
- Test: `backend/ideal-agent-infrastructure/src/test/java/com/idealagent/infrastructure/repository/AgentManagementRepositoryTest.java`

- [ ] **Step 1: Write failing service tests**

Update `AgentManagementServiceTest` to create Flow with MiniAgent DTO:

```java
@Test
void createsMiniAgentStyleFlow() {
    repository.agents.add(new AgentManageVO("agent_custom_step", "Custom Step", "step", "desc", "", "", 1));

    FlowManageVO flow = service.createFlow(new FlowManageDTO(
            null, null, "agent_custom_step", "client_planner", "planner", "Plan %s", 2));

    assertThat(flow.agentId()).isEqualTo("agent_custom_step");
    assertThat(flow.clientId()).isEqualTo("client_planner");
    assertThat(flow.clientRole()).isEqualTo("planner");
    assertThat(flow.userPrompt()).isEqualTo("Plan %s");
    assertThat(flow.flowSeq()).isEqualTo(2);
}
```

Add a role mismatch test:

```java
@Test
void rejectsMiniAgentFlowRoleThatDoesNotMatchAgentType() {
    repository.agents.add(new AgentManageVO("agent_custom_step", "Custom Step", "step", "desc", "", "", 1));

    assertThatThrownBy(() -> service.createFlow(new FlowManageDTO(
            null, null, "agent_custom_step", "client_summarizer", "summarizer", "Summarize", 4)))
            .isInstanceOf(AgentManagementException.class)
            .hasMessageContaining("Flow 角色不匹配");
}
```

- [ ] **Step 2: Run service tests and verify failure**

Run:

```powershell
mvn -q -pl ideal-agent-domain -Dtest=AgentManagementServiceTest test
```

Expected: FAIL because `FlowManageDTO` still has old fields.

- [ ] **Step 3: Replace Flow DTO and VO**

Set `FlowManageDTO.java` to:

```java
package com.idealagent.domain.ai.model.dto;

public record FlowManageDTO(
        String originAgentId,
        String originClientId,
        String agentId,
        String clientId,
        String clientRole,
        String userPrompt,
        Integer flowSeq) {
}
```

Set `FlowManageVO.java` to:

```java
package com.idealagent.domain.ai.model.vo;

public record FlowManageVO(
        String agentId,
        String clientId,
        String clientRole,
        String userPrompt,
        Integer flowSeq) {
}
```

- [ ] **Step 4: Update repository interface**

Replace Flow methods in `IAgentManagementRepository` with:

```java
List<FlowManageVO> listFlows(String agentId);

FlowManageVO findFlow(String agentId, String clientId);

FlowManageVO saveFlow(FlowManageDTO request);

FlowManageVO updateFlow(String originAgentId, String originClientId, FlowManageDTO request);

void deleteFlow(String agentId, String clientId);
```

- [ ] **Step 5: Update service validation**

In `AgentManagementService`, make `validateFlow` require `agentId`, `clientId`, `clientRole`, `userPrompt`, and `flowSeq >= 1`. Use `ROLE_MAP` exactly as existing values.

Minimal implementation shape:

```java
public FlowManageVO createFlow(FlowManageDTO request) {
    FlowManageDTO normalized = normalizeFlow(request);
    validateFlow(normalized);
    return repository.saveFlow(normalized);
}

public FlowManageVO updateFlow(FlowManageDTO request) {
    String originAgentId = StringUtils.hasText(request.originAgentId()) ? request.originAgentId() : request.agentId();
    String originClientId = StringUtils.hasText(request.originClientId()) ? request.originClientId() : request.clientId();
    validateId(originAgentId, "Origin Agent ID不能为空");
    validateId(originClientId, "Origin Client ID不能为空");
    FlowManageDTO normalized = normalizeFlow(request);
    validateFlow(normalized);
    return repository.updateFlow(originAgentId.trim(), originClientId.trim(), normalized);
}

public void deleteFlow(String agentId, String clientId) {
    validateId(agentId, "Agent ID不能为空");
    validateId(clientId, "Client ID不能为空");
    repository.deleteFlow(agentId.trim(), clientId.trim());
}
```

- [ ] **Step 6: Update fake repository in service test**

Make `FakeRepository.saveFlow` return:

```java
FlowManageVO vo = new FlowManageVO(
        request.agentId(), request.clientId(), request.clientRole(), request.userPrompt(), request.flowSeq());
savedFlows.add(vo);
return vo;
```

- [ ] **Step 7: Update infrastructure repository mapping**

In `AgentManagementRepository`, map Flow fields:

```java
private AiFlow toFlowPo(FlowManageDTO request) {
    AiFlow flow = new AiFlow();
    flow.setAgentId(request.agentId());
    flow.setClientId(request.clientId());
    flow.setClientRole(request.clientRole());
    flow.setUserPrompt(request.userPrompt());
    flow.setFlowSeq(request.flowSeq());
    return flow;
}

private FlowManageVO toFlowVo(AiFlow flow) {
    if (flow == null) {
        return null;
    }
    return new FlowManageVO(flow.getAgentId(), flow.getClientId(), flow.getClientRole(), flow.getUserPrompt(), flow.getFlowSeq());
}
```

Remove `savePromptBinding` and all `flow -> prompt` config writes from this repository.

- [ ] **Step 8: Update repository tests**

Change `AgentManagementRepositoryTest.savesFlowAndPromptBinding` into `savesMiniAgentStyleFlow`:

```java
@Test
void savesMiniAgentStyleFlow() {
    FlowManageVO flow = repository.saveFlow(new FlowManageDTO(
            null, null, "agent_custom_step", "client_planner", "planner", "Plan %s", 2));

    assertThat(flow.agentId()).isEqualTo("agent_custom_step");
    assertThat(flow.clientRole()).isEqualTo("planner");
    verify(flowDao).insert(any(AiFlow.class));
    verifyNoInteractions(configDao);
}
```

- [ ] **Step 9: Run focused tests**

Run:

```powershell
mvn -q -pl ideal-agent-domain -Dtest=AgentManagementServiceTest test
mvn -q -pl ideal-agent-infrastructure -Dtest=AgentManagementRepositoryTest test
```

Expected: PASS.

---

### Task 3: Add `/ai/admin` Agent And Flow Endpoints

**Files:**
- Modify: `backend/ideal-agent-trigger/src/main/java/com/idealagent/trigger/controller/AiAgentController.java`
- Test: `backend/ideal-agent-app/src/test/java/com/idealagent/AiAgentControllerTest.java`

- [ ] **Step 1: Write failing controller tests**

Add tests for the new Flow endpoints:

```java
@Test
void createsAdminFlow() throws Exception {
    FlowManageVO response = new FlowManageVO("agent_custom_step", "client_planner", "planner", "Plan %s", 2);
    when(agentManagementService.createFlow(any(FlowManageDTO.class))).thenReturn(response);

    mockMvc.perform(post("/ai/admin/flows")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"agentId\":\"agent_custom_step\",\"clientId\":\"client_planner\",\"clientRole\":\"planner\",\"userPrompt\":\"Plan %s\",\"flowSeq\":2}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.clientRole").value("planner"));
}

@Test
void deletesAdminFlowByCompositeKey() throws Exception {
    mockMvc.perform(delete("/ai/admin/flows")
                    .param("agentId", "agent_custom_step")
                    .param("clientId", "client_planner"))
            .andExpect(status().isOk());

    verify(agentManagementService).deleteFlow("agent_custom_step", "client_planner");
}
```

- [ ] **Step 2: Run controller tests and verify failure**

Run:

```powershell
mvn -q -pl ideal-agent-app -Dtest=AiAgentControllerTest test
```

Expected: FAIL because `/ai/admin/flows` routes do not exist.

- [ ] **Step 3: Add admin routes**

In `AiAgentController`, keep existing routes only if needed, and add:

```java
@GetMapping("/admin/agents")
public Result<List<AgentManageVO>> adminAgents() {
    return Result.success(agentManagementService.listAgents());
}

@PostMapping("/admin/agents")
public Result<AgentManageVO> adminCreateAgent(@RequestBody AgentManageDTO request) {
    return Result.success(agentManagementService.createAgent(request));
}

@PutMapping("/admin/agents/{agentId}")
public Result<AgentManageVO> adminUpdateAgent(@PathVariable String agentId, @RequestBody AgentManageDTO request) {
    return Result.success(agentManagementService.updateAgent(agentId, request));
}

@PatchMapping("/admin/agents/{agentId}/status")
public Result<Void> adminUpdateAgentStatus(@PathVariable String agentId, @RequestBody AgentManageDTO request) {
    agentManagementService.updateAgentStatus(agentId, request.status());
    return Result.success(null);
}

@DeleteMapping("/admin/agents/{agentId}")
public Result<Void> adminDeleteAgent(@PathVariable String agentId) {
    agentManagementService.deleteAgent(agentId);
    return Result.success(null);
}

@GetMapping("/admin/flows/agents")
public Result<List<AgentManageVO>> adminFlowAgents() {
    return Result.success(agentManagementService.listAgents());
}

@GetMapping("/admin/flows/{agentId}")
public Result<List<FlowManageVO>> adminFlows(@PathVariable String agentId) {
    return Result.success(agentManagementService.listFlows(agentId));
}

@PostMapping("/admin/flows")
public Result<FlowManageVO> adminCreateFlow(@RequestBody FlowManageDTO request) {
    return Result.success(agentManagementService.createFlow(request));
}

@PutMapping("/admin/flows")
public Result<FlowManageVO> adminUpdateFlow(@RequestBody FlowManageDTO request) {
    return Result.success(agentManagementService.updateFlow(request));
}

@DeleteMapping("/admin/flows")
public Result<Void> adminDeleteFlow(@RequestParam String agentId, @RequestParam String clientId) {
    agentManagementService.deleteFlow(agentId, clientId);
    return Result.success(null);
}
```

- [ ] **Step 4: Run controller tests**

Run:

```powershell
mvn -q -pl ideal-agent-app -Dtest=AiAgentControllerTest test
```

Expected: PASS.

---

### Task 4: Update Work Runtime To Consume MiniAgent Flow Rows

**Files:**
- Modify: `backend/ideal-agent-domain/src/main/java/com/idealagent/domain/ai/model/vo/AiFlowVO.java`
- Modify: `backend/ideal-agent-infrastructure/src/main/java/com/idealagent/infrastructure/repository/WorkAgentRepository.java`
- Test: `backend/ideal-agent-infrastructure/src/test/java/com/idealagent/infrastructure/repository/WorkAgentRepositoryTest.java`
- Test: `backend/ideal-agent-domain/src/test/java/com/idealagent/domain/ai/service/work/step/ExecuteStepStrategyTest.java`
- Test: `backend/ideal-agent-domain/src/test/java/com/idealagent/domain/ai/service/work/MiniAgentParityStrategyTest.java`

- [ ] **Step 1: Query runtime flow symbols**

Run:

```powershell
codegraph query AiFlowVO
codegraph query WorkAgentRepository
codegraph context "Work runtime flow userPrompt clientRole flowSeq"
```

Expected: CodeGraph shows `AiFlowVO`, `WorkAgentRepository`, `ExecuteStepStrategy`, and step nodes.

- [ ] **Step 2: Write failing runtime repository test**

In `WorkAgentRepositoryTest`, assert MiniAgent fields map correctly:

```java
@Test
void listsEnabledFlowsOrderedByFlowSeqWithUserPrompt() {
    AiFlow flow = new AiFlow();
    flow.setAgentId("agent_custom_step");
    flow.setClientId("client_planner");
    flow.setClientRole("planner");
    flow.setUserPrompt("Plan %s");
    flow.setFlowSeq(2);
    when(flowDao.listEnabledByAgentId("agent_custom_step")).thenReturn(List.of(flow));

    List<AiFlowVO> flows = repository.listEnabledFlows("agent_custom_step");

    assertThat(flows).hasSize(1);
    assertThat(flows.get(0).getClientId()).isEqualTo("client_planner");
    assertThat(flows.get(0).getClientRole()).isEqualTo("planner");
    assertThat(flows.get(0).getUserPrompt()).isEqualTo("Plan %s");
    assertThat(flows.get(0).getFlowSeq()).isEqualTo(2);
}
```

- [ ] **Step 3: Run runtime repository test and verify failure**

Run:

```powershell
mvn -q -pl ideal-agent-infrastructure -Dtest=WorkAgentRepositoryTest test
```

Expected: FAIL if `AiFlowVO` or repository still uses old names.

- [ ] **Step 4: Update `AiFlowVO`**

Ensure `AiFlowVO` exposes:

```java
private String agentId;
private String clientId;
private String clientRole;
private String userPrompt;
private Integer flowSeq;
```

Keep compatibility methods only if existing strategy code still expects constants. Do not reintroduce `flowId`, `promptId`, or `sortOrder`.

- [ ] **Step 5: Update `WorkAgentRepository` mapping**

Map `AiFlow` to `AiFlowVO` using MiniAgent fields:

```java
private AiFlowVO toFlow(AiFlow po) {
    AiFlowVO vo = new AiFlowVO();
    vo.setAgentId(po.getAgentId());
    vo.setClientId(po.getClientId());
    vo.setClientRole(po.getClientRole());
    vo.setUserPrompt(po.getUserPrompt());
    vo.setFlowSeq(po.getFlowSeq());
    return vo;
}
```

- [ ] **Step 6: Update flow maps in strategy setup if necessary**

Where runtime builds `Map<String, AiFlowVO>`, use:

```java
Collectors.toMap(AiFlowVO::getClientRole, Function.identity(), (left, right) -> left, LinkedHashMap::new)
```

This keeps Step/Loop/React node lookup by role unchanged.

- [ ] **Step 7: Run focused runtime tests**

Run:

```powershell
mvn -q -pl ideal-agent-infrastructure -Dtest=WorkAgentRepositoryTest test
mvn -q -pl ideal-agent-domain -am "-Dtest=ExecuteStepStrategyTest,MiniAgentParityStrategyTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: PASS.

---

### Task 5: Add Canvas Backend Service And Endpoints

**Files:**
- Create: `backend/ideal-agent-domain/src/main/java/com/idealagent/domain/ai/model/dto/CanvasNodeDTO.java`
- Create: `backend/ideal-agent-domain/src/main/java/com/idealagent/domain/ai/model/dto/CanvasRelationDTO.java`
- Create: `backend/ideal-agent-domain/src/main/java/com/idealagent/domain/ai/model/vo/CanvasGraphVO.java`
- Create: `backend/ideal-agent-domain/src/main/java/com/idealagent/domain/ai/service/agent/AdminCanvasService.java`
- Modify: `backend/ideal-agent-domain/src/main/java/com/idealagent/domain/ai/repository/IAgentManagementRepository.java`
- Modify: `backend/ideal-agent-infrastructure/src/main/java/com/idealagent/infrastructure/repository/AgentManagementRepository.java`
- Modify: `backend/ideal-agent-trigger/src/main/java/com/idealagent/trigger/controller/AiAgentController.java`
- Test: `backend/ideal-agent-domain/src/test/java/com/idealagent/domain/ai/service/agent/AdminCanvasServiceTest.java`
- Test: `backend/ideal-agent-infrastructure/src/test/java/com/idealagent/infrastructure/repository/AgentManagementRepositoryTest.java`

- [ ] **Step 1: Write service tests for relation validation**

Create `AdminCanvasServiceTest` with:

```java
@Test
void rejectsUnsupportedRelation() {
    AdminCanvasService service = new AdminCanvasService(repository);

    assertThatThrownBy(() -> service.saveRelation(new CanvasRelationDTO("api", "prompt", "api_x", "prompt_x", "agent_x", null)))
            .isInstanceOf(AgentManagementException.class)
            .hasMessageContaining("连接规则不允许");
}

@Test
void acceptsClientPromptRelation() {
    AdminCanvasService service = new AdminCanvasService(repository);

    service.saveRelation(new CanvasRelationDTO("client", "prompt", "client_x", "prompt_x", "agent_x", "prompt"));

    assertThat(repository.savedRelations).hasSize(1);
}
```

- [ ] **Step 2: Run service test and verify failure**

Run:

```powershell
mvn -q -pl ideal-agent-domain -Dtest=AdminCanvasServiceTest test
```

Expected: FAIL because DTO and service do not exist.

- [ ] **Step 3: Add Canvas DTOs**

Create `CanvasNodeDTO.java`:

```java
package com.idealagent.domain.ai.model.dto;

import java.util.Map;

public record CanvasNodeDTO(
        String nodeType,
        String id,
        String agentId,
        Map<String, Object> payload) {
}
```

Create `CanvasRelationDTO.java`:

```java
package com.idealagent.domain.ai.model.dto;

public record CanvasRelationDTO(
        String sourceType,
        String targetType,
        String sourceId,
        String targetId,
        String agentId,
        String configType) {
}
```

Create `CanvasGraphVO.java`:

```java
package com.idealagent.domain.ai.model.vo;

import java.util.List;
import java.util.Map;

public record CanvasGraphVO(
        AgentManageVO agent,
        List<FlowManageVO> flows,
        List<Map<String, Object>> clients,
        List<Map<String, Object>> models,
        List<Map<String, Object>> apis,
        List<Map<String, Object>> prompts,
        List<Map<String, Object>> advisors,
        List<Map<String, Object>> mcps,
        List<Map<String, Object>> configs) {
}
```

- [ ] **Step 4: Add `AdminCanvasService`**

Implement relation validation:

```java
private boolean allowed(String sourceType, String targetType) {
    String edge = normalize(sourceType) + "->" + normalize(targetType);
    return Set.of(
            "agent->client",
            "client->client",
            "client->model",
            "model->api",
            "client->prompt",
            "client->advisor",
            "client->mcp").contains(edge);
}
```

Expose:

```java
public CanvasGraphVO graph(String agentId) {
    validateId(agentId, "Agent ID不能为空");
    return repository.canvasGraph(agentId.trim());
}

public void saveNode(CanvasNodeDTO request) {
    validateId(request.nodeType(), "Node Type不能为空");
    repository.saveCanvasNode(request);
}

public void saveRelation(CanvasRelationDTO request) {
    if (!allowed(request.sourceType(), request.targetType())) {
        throw new AgentManagementException("连接规则不允许");
    }
    repository.saveCanvasRelation(request);
}
```

- [ ] **Step 5: Extend repository interface**

Add:

```java
CanvasGraphVO canvasGraph(String agentId);

void saveCanvasNode(CanvasNodeDTO request);

void saveCanvasRelation(CanvasRelationDTO request);

void deleteCanvasRelation(CanvasRelationDTO request);
```

- [ ] **Step 6: Implement minimal repository relation writes**

In `AgentManagementRepository.saveCanvasRelation`, implement these cases first:

```java
if ("agent".equals(sourceType) && "client".equals(targetType)) {
    FlowManageDTO flow = new FlowManageDTO(null, null, request.agentId(), request.targetId(), "inspector", "auto", nextFlowSeq(request.agentId()));
    saveFlow(flow);
    return;
}
if ("client".equals(sourceType) && Set.of("prompt", "advisor", "mcp").contains(targetType)) {
    AiConfigData config = new AiConfigData();
    config.setConfigId("config_" + request.sourceId() + "_" + targetType + "_" + request.targetId());
    config.setOwnerId(request.sourceId());
    config.setContent(request.sourceId());
    config.setOwnerType("client");
    config.setConfigType(targetType);
    config.setRefId(request.targetId());
    config.setStatus(1);
    configDao.insertConfig(config);
    return;
}
```

Then add `client->model` and `model->api` with existing DAO update methods. If no update method exists yet, add the DAO method and test it in the same task.

- [ ] **Step 7: Add controller endpoints**

Inject `AdminCanvasService` and add:

```java
@GetMapping("/admin/canvas/{agentId}")
public Result<CanvasGraphVO> adminCanvas(@PathVariable String agentId) {
    return Result.success(adminCanvasService.graph(agentId));
}

@PostMapping("/admin/canvas/node")
public Result<Void> adminCreateCanvasNode(@RequestBody CanvasNodeDTO request) {
    adminCanvasService.saveNode(request);
    return Result.success(null);
}

@PutMapping("/admin/canvas/node")
public Result<Void> adminUpdateCanvasNode(@RequestBody CanvasNodeDTO request) {
    adminCanvasService.saveNode(request);
    return Result.success(null);
}

@PostMapping("/admin/canvas/relation")
public Result<Void> adminCreateCanvasRelation(@RequestBody CanvasRelationDTO request) {
    adminCanvasService.saveRelation(request);
    return Result.success(null);
}

@DeleteMapping("/admin/canvas/relation")
public Result<Void> adminDeleteCanvasRelation(@RequestBody CanvasRelationDTO request) {
    adminCanvasService.deleteRelation(request);
    return Result.success(null);
}
```

- [ ] **Step 8: Run Canvas focused tests**

Run:

```powershell
mvn -q -pl ideal-agent-domain -Dtest=AdminCanvasServiceTest test
mvn -q -pl ideal-agent-infrastructure -Dtest=AgentManagementRepositoryTest test
```

Expected: PASS.

---

### Task 6: Split Frontend Agent Management, Flow Management, And Canvas

**Files:**
- Modify: `frontend/src/router/router.js`
- Modify: `frontend/src/components/Sidebar.vue`
- Replace: `frontend/src/components/AgentAdmin.vue`
- Create: `frontend/src/components/AgentFlow.vue`
- Create: `frontend/src/components/AgentCanvas.vue`
- Modify: `frontend/package.json` if Vue Flow packages are missing

- [ ] **Step 1: Check Vue Flow dependency**

Run:

```powershell
npm ls @vue-flow/core
```

Expected: If missing, install with `npm install @vue-flow/core @vue-flow/background @vue-flow/controls @vue-flow/minimap` after confirming `frontend/package.json` parent exists.

- [ ] **Step 2: Update routes**

In `router.js`, add imports:

```javascript
import AgentFlow from '../components/AgentFlow.vue'
import AgentCanvas from '../components/AgentCanvas.vue'
```

Add routes:

```javascript
{ path: '/agents', name: 'agents', component: AgentAdmin, meta: { requiresAuth: true } },
{ path: '/agents/flow', name: 'agent-flow', component: AgentFlow, meta: { requiresAuth: true } },
{ path: '/agents/canvas', name: 'agent-canvas', component: AgentCanvas, meta: { requiresAuth: true } }
```

- [ ] **Step 3: Replace `AgentAdmin.vue` with Agent table CRUD**

The component should call:

```javascript
request.get('/ai/admin/agents')
request.post('/ai/admin/agents', body)
request.put(`/ai/admin/agents/${editingAgentId.value}`, body)
request.patch(`/ai/admin/agents/${agent.agentId}/status`, { status: nextStatus })
request.delete(`/ai/admin/agents/${agent.agentId}`)
```

Use fields:

```javascript
function emptyAgent() {
  return { agentId: '', agentName: '', agentType: 'step', agentDesc: '', modelId: '', templateId: '', status: 1 }
}
```

Add a Flow button:

```vue
<button class="text-accent" type="button" @click="router.push({ path: '/agents/flow', query: { agentId: agent.agentId } })">FLOW</button>
```

- [ ] **Step 4: Create `AgentFlow.vue` role-slot view**

Use role map:

```javascript
const roleMap = {
  step: ['INSPECTOR', 'PLANNER', 'RUNNER', 'REPLIER'],
  loop: ['ANALYZER', 'PERFORMER', 'SUPERVISOR', 'SUMMARIZER'],
  react: ['OBSERVER', 'REASONER', 'ACTOR', 'EVALUATOR']
}
```

Load data:

```javascript
const agents = ref([])
const selectedAgent = ref(null)
const flows = ref([])

async function loadAgents() {
  const response = await request.get('/ai/admin/flows/agents')
  agents.value = response.data.data || []
}

async function loadFlows(agentId) {
  const response = await request.get(`/ai/admin/flows/${agentId}`)
  flows.value = response.data.data || []
}
```

Slot matching:

```javascript
const slotList = computed(() => roleOrder.value.map((role, index) => {
  const lower = role.toLowerCase()
  const flow = flows.value.find(item => item.clientRole === lower) || flows.value.find(item => item.flowSeq === index + 1)
  return { role, seq: index + 1, flow }
}))
```

Canvas button:

```vue
<button type="button" @click="router.push({ path: '/agents/canvas', query: { agentId: selectedAgent.agentId } })">查看配置图</button>
```

- [ ] **Step 5: Create `AgentCanvas.vue` using Vue Flow**

Import Vue Flow:

```javascript
import { VueFlow, Handle, Position, MarkerType } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import { MiniMap } from '@vue-flow/minimap'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/controls/dist/style.css'
import '@vue-flow/minimap/dist/style.css'
```

Load graph:

```javascript
async function fetchData() {
  const response = await request.get(`/ai/admin/canvas/${agentId.value}`)
  graph.value = response.data.data
  buildGraph()
}
```

Connection rules:

```javascript
function isAllowed(sourceType, targetType) {
  return new Set(['agent->client', 'client->client', 'client->model', 'model->api', 'client->prompt', 'client->advisor', 'client->mcp'])
    .has(`${sourceType}->${targetType}`)
}
```

Save relation:

```javascript
await request.post('/ai/admin/canvas/relation', {
  sourceType: existingNode.type,
  targetType: draftNode.type,
  sourceId: existingNode.data.id,
  targetId: currentForm.id,
  agentId: agentId.value,
  configType: draftNode.type
})
```

- [ ] **Step 6: Run frontend build**

Run:

```powershell
npm run build
```

Expected: PASS. Vite chunk-size warning is acceptable if exit code is 0.

---

### Task 7: Final Verification And CodeGraph Sync

**Files:**
- No source edits unless verification reveals failures.

- [ ] **Step 1: Run focused backend tests**

Run:

```powershell
mvn -q -pl ideal-agent-domain -am "-Dtest=AgentManagementServiceTest,AdminCanvasServiceTest,AgentManagementRepositoryTest,WorkAgentRepositoryTest,ExecuteStepStrategyTest,MiniAgentParityStrategyTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: PASS.

- [ ] **Step 2: Run full backend tests**

Run:

```powershell
mvn -q test
```

Expected: PASS.

- [ ] **Step 3: Package app**

Run:

```powershell
mvn -q -pl ideal-agent-app -am -DskipTests package
```

Expected: PASS.

- [ ] **Step 4: Build frontend**

Run:

```powershell
npm run build
```

Expected: PASS with exit code 0.

- [ ] **Step 5: Sync CodeGraph**

Run:

```powershell
codegraph sync
codegraph status
```

Expected: CodeGraph reports index is up to date.

- [ ] **Step 6: Inspect diff without committing**

Run:

```powershell
git status --short
git diff -- backend/docs/mysql/ideal_agent_schema.sql backend/docs/mysql/ideal_agent_seed.sql
```

Expected: Only intended files are modified. Do not commit unless the user explicitly requests it.

---

## Self-Review

Spec coverage:

- Data model migration is covered by Task 1.
- Admin Agent and Flow endpoints are covered by Tasks 2 and 3.
- Runtime adaptation is covered by Task 4.
- CONFIG CANVAS backend is covered by Task 5.
- Frontend Agent, Flow, and Canvas pages are covered by Task 6.
- Tests and CodeGraph sync are covered by Task 7.

Type consistency:

- Flow management uses `originAgentId`, `originClientId`, `agentId`, `clientId`, `clientRole`, `userPrompt`, and `flowSeq` across DTO, VO, service, repository, API, and frontend.
- Runtime uses `AiFlowVO.getClientRole()`, `getUserPrompt()`, `getClientId()`, and `getFlowSeq()`.
- Canvas relation types are restricted to `agent`, `client`, `model`, `api`, `prompt`, `advisor`, and `mcp`.

Execution note:

- This plan intentionally omits commit steps because the user instructed not to commit unless explicitly requested.
