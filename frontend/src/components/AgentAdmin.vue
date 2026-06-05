<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import request from '../request/request'
import Sidebar from './Sidebar.vue'
import Footer from './Footer.vue'
import UiButton from './ui/UiButton.vue'

const roleMap = {
  step: ['inspector', 'planner', 'runner', 'replier'],
  loop: ['analyzer', 'performer', 'supervisor', 'summarizer'],
  react: ['observer', 'reasoner', 'actor', 'evaluator']
}

const agents = ref([])
const flows = ref([])
const options = ref({ clients: [], prompts: [], mcps: [] })
const selectedAgentId = ref('')
const editingAgentId = ref('')
const editingFlowId = ref('')
const agentMessage = ref('')
const flowMessage = ref('')
const error = ref('')
const loading = ref(false)

const agentForm = ref(emptyAgent())
const flowForm = ref(emptyFlow())

const selectedAgent = computed(() => agents.value.find(agent => agent.agentId === selectedAgentId.value) || null)
const roleOptions = computed(() => roleMap[selectedAgent.value?.agentType || agentForm.value.agentType] || roleMap.step)

onMounted(refreshAll)

watch(selectedAgentId, async agentId => {
  resetFlowForm()
  if (agentId) {
    await loadFlows(agentId)
  } else {
    flows.value = []
  }
})

function emptyAgent() {
  return { agentId: '', agentName: '', agentType: 'step', agentDesc: '', modelId: '', templateId: '', status: 1 }
}

function emptyFlow() {
  return { flowId: '', agentId: selectedAgentId.value || '', clientId: '', roleType: 'inspector', sortOrder: 1, promptId: '', status: 1 }
}

async function refreshAll() {
  await Promise.all([loadAgents(), loadOptions()])
  if (selectedAgentId.value) {
    await loadFlows(selectedAgentId.value)
  }
}

async function loadAgents() {
  error.value = ''
  try {
    const response = await request.get('/ai/agents')
    agents.value = response.data.data || []
    if (!selectedAgentId.value && agents.value.length > 0) {
      selectedAgentId.value = agents.value[0].agentId
    }
  } catch (e) {
    error.value = e.response?.data?.message || 'Agent 列表加载失败'
  }
}

async function loadOptions() {
  try {
    const response = await request.get('/ai/flow-options')
    options.value = response.data.data || { clients: [], prompts: [], mcps: [] }
  } catch (e) {
    error.value = e.response?.data?.message || 'Flow 选项加载失败'
  }
}

async function loadFlows(agentId) {
  try {
    const response = await request.get(`/ai/agents/${agentId}/flows`)
    flows.value = response.data.data || []
  } catch (e) {
    error.value = e.response?.data?.message || 'Flow 列表加载失败'
  }
}

function selectAgent(agent) {
  selectedAgentId.value = agent.agentId
  editAgent(agent)
}

function editAgent(agent) {
  editingAgentId.value = agent.agentId
  agentForm.value = {
    agentId: agent.agentId || '',
    agentName: agent.agentName || '',
    agentType: agent.agentType || 'step',
    agentDesc: agent.agentDesc || '',
    modelId: agent.modelId || '',
    templateId: agent.templateId || '',
    status: agent.status ?? 1
  }
}

function resetAgentForm() {
  editingAgentId.value = ''
  agentForm.value = emptyAgent()
}

async function saveAgent() {
  error.value = ''
  agentMessage.value = ''
  loading.value = true
  try {
    const body = normalizeAgentPayload(agentForm.value)
    const response = editingAgentId.value
      ? await request.put(`/ai/agents/${editingAgentId.value}`, body)
      : await request.post('/ai/agents', body)
    agentMessage.value = `${editingAgentId.value ? '已更新' : '已创建'} ${response.data.data.agentId}`
    selectedAgentId.value = response.data.data.agentId
    await loadAgents()
  } catch (e) {
    error.value = e.response?.data?.message || 'Agent 保存失败'
  } finally {
    loading.value = false
  }
}

async function toggleAgent(agent) {
  const nextStatus = agent.status === 1 ? 0 : 1
  try {
    await request.patch(`/ai/agents/${agent.agentId}/status`, { status: nextStatus })
    await loadAgents()
  } catch (e) {
    error.value = e.response?.data?.message || 'Agent 状态更新失败'
  }
}

async function deleteAgent(agent) {
  if (!window.confirm(`确认删除 Agent ${agent.agentId}？`)) {
    return
  }
  try {
    await request.delete(`/ai/agents/${agent.agentId}`)
    if (selectedAgentId.value === agent.agentId) {
      selectedAgentId.value = ''
      flows.value = []
      resetAgentForm()
    }
    await loadAgents()
  } catch (e) {
    error.value = e.response?.data?.message || 'Agent 删除失败'
  }
}

function editFlow(flow) {
  editingFlowId.value = flow.flowId
  flowForm.value = {
    flowId: flow.flowId || '',
    agentId: flow.agentId || selectedAgentId.value,
    clientId: flow.clientId || '',
    roleType: flow.roleType || roleOptions.value[0],
    sortOrder: flow.sortOrder ?? 1,
    promptId: flow.promptId || '',
    status: flow.status ?? 1
  }
}

function resetFlowForm() {
  editingFlowId.value = ''
  flowForm.value = emptyFlow()
  flowForm.value.agentId = selectedAgentId.value || ''
  flowForm.value.roleType = roleOptions.value[0] || 'inspector'
}

async function saveFlow() {
  if (!selectedAgentId.value) {
    error.value = '请先选择 Agent'
    return
  }
  error.value = ''
  flowMessage.value = ''
  loading.value = true
  try {
    const body = normalizeFlowPayload({ ...flowForm.value, agentId: selectedAgentId.value })
    const response = editingFlowId.value
      ? await request.put(`/ai/flows/${editingFlowId.value}`, body)
      : await request.post('/ai/flows', body)
    flowMessage.value = `${editingFlowId.value ? '已更新' : '已创建'} ${response.data.data.flowId}`
    await loadFlows(selectedAgentId.value)
    resetFlowForm()
  } catch (e) {
    error.value = e.response?.data?.message || 'Flow 保存失败'
  } finally {
    loading.value = false
  }
}

async function toggleFlow(flow) {
  const nextStatus = flow.status === 1 ? 0 : 1
  try {
    await request.patch(`/ai/flows/${flow.flowId}/status`, { status: nextStatus })
    await loadFlows(selectedAgentId.value)
  } catch (e) {
    error.value = e.response?.data?.message || 'Flow 状态更新失败'
  }
}

async function deleteFlow(flow) {
  if (!window.confirm(`确认删除 Flow ${flow.flowId}？`)) {
    return
  }
  try {
    await request.delete(`/ai/flows/${flow.flowId}`)
    await loadFlows(selectedAgentId.value)
  } catch (e) {
    error.value = e.response?.data?.message || 'Flow 删除失败'
  }
}

function normalizeAgentPayload(data) {
  return {
    agentId: trim(data.agentId),
    agentName: trim(data.agentName),
    agentType: trim(data.agentType),
    agentDesc: trim(data.agentDesc),
    modelId: trim(data.modelId),
    templateId: trim(data.templateId),
    status: Number(data.status ?? 1)
  }
}

function normalizeFlowPayload(data) {
  return {
    flowId: trim(data.flowId),
    agentId: trim(data.agentId),
    clientId: trim(data.clientId),
    roleType: trim(data.roleType),
    sortOrder: Number(data.sortOrder || 1),
    promptId: trim(data.promptId),
    status: Number(data.status ?? 1)
  }
}

function trim(value) {
  return typeof value === 'string' ? value.trim() : value
}

function optionLabel(record) {
  return `${record.name || record.content || record.configId} (${record.configId})`
}
</script>

<template>
  <div class="flex h-screen bg-surface text-text-primary">
    <Sidebar />
    <div class="flex min-w-0 flex-1 flex-col">
      <main class="flex-1 overflow-auto p-5">
        <section class="mx-auto max-w-7xl page-enter">
          <div class="mb-6 flex items-start justify-between gap-4">
            <div>
              <p class="text-xs font-semibold uppercase tracking-widest text-accent">Agent</p>
              <h1 class="mt-2 text-2xl font-bold">Agent / Flow 管理</h1>
              <p class="mt-2 max-w-3xl text-sm text-text-secondary">
                管理 Work Agent、角色 Flow 节点，以及 Flow 到 Prompt、Client 到 MCP 的配置关系。
              </p>
            </div>
            <UiButton variant="secondary" :loading="loading" @click="refreshAll">刷新</UiButton>
          </div>

          <p v-if="error" class="mb-4 rounded-card border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{{ error }}</p>

          <div class="grid gap-5 lg:grid-cols-[360px_minmax(0,1fr)]">
            <aside class="space-y-5">
              <div class="rounded-card-lg border border-border-default bg-elevated p-5 shadow-card">
                <div class="mb-4 flex items-center justify-between">
                  <h2 class="font-bold">Agent 列表</h2>
                  <button class="text-sm font-medium text-accent" type="button" @click="resetAgentForm">新增</button>
                </div>
                <div class="space-y-3">
                  <button
                    v-for="agent in agents"
                    :key="agent.agentId"
                    class="w-full rounded-card border px-4 py-3 text-left transition-all"
                    :class="selectedAgentId === agent.agentId ? 'border-accent bg-accent/5' : 'border-border-subtle bg-surface hover:border-accent'"
                    type="button"
                    @click="selectAgent(agent)"
                  >
                    <div class="flex items-center justify-between gap-3">
                      <div class="font-semibold">{{ agent.agentName || agent.agentId }}</div>
                      <span class="rounded-full px-2 py-0.5 text-xs font-bold" :class="agent.status === 1 ? 'bg-emerald-100 text-emerald-700' : 'bg-red-100 text-red-700'">
                        {{ agent.status === 1 ? '启用' : '禁用' }}
                      </span>
                    </div>
                    <div class="mt-2 text-xs uppercase tracking-wide text-accent">{{ agent.agentType }}</div>
                    <p class="mt-2 line-clamp-2 text-sm text-text-secondary">{{ agent.agentDesc || '暂无描述' }}</p>
                  </button>
                  <div v-if="agents.length === 0" class="rounded-card border border-dashed border-border-default px-4 py-8 text-center text-sm text-text-tertiary">
                    暂无 Agent
                  </div>
                </div>
              </div>

              <div class="rounded-card-lg border border-border-default bg-elevated p-5 shadow-card">
                <h2 class="mb-4 font-bold">{{ editingAgentId ? '编辑 Agent' : '新增 Agent' }}</h2>
                <div class="space-y-3">
                  <input v-model="agentForm.agentId" :disabled="Boolean(editingAgentId)" class="w-full rounded-card border border-border-default bg-surface px-3 py-2 text-sm outline-none focus:border-accent" placeholder="agentId" />
                  <input v-model="agentForm.agentName" class="w-full rounded-card border border-border-default bg-surface px-3 py-2 text-sm outline-none focus:border-accent" placeholder="Agent 名称" />
                  <select v-model="agentForm.agentType" class="w-full rounded-card border border-border-default bg-surface px-3 py-2 text-sm outline-none focus:border-accent">
                    <option value="step">step</option>
                    <option value="loop">loop</option>
                    <option value="react">react</option>
                  </select>
                  <textarea v-model="agentForm.agentDesc" class="min-h-[86px] w-full rounded-card border border-border-default bg-surface px-3 py-2 text-sm outline-none focus:border-accent" placeholder="Agent 描述" />
                  <input v-model="agentForm.modelId" class="w-full rounded-card border border-border-default bg-surface px-3 py-2 text-sm outline-none focus:border-accent" placeholder="modelId，可选" />
                  <input v-model="agentForm.templateId" class="w-full rounded-card border border-border-default bg-surface px-3 py-2 text-sm outline-none focus:border-accent" placeholder="templateId，可选" />
                  <select v-model.number="agentForm.status" class="w-full rounded-card border border-border-default bg-surface px-3 py-2 text-sm outline-none focus:border-accent">
                    <option :value="1">启用</option>
                    <option :value="0">禁用</option>
                  </select>
                  <UiButton full-width :loading="loading" @click="saveAgent">保存 Agent</UiButton>
                  <p v-if="agentMessage" class="rounded-card bg-emerald-50 px-3 py-2 text-xs text-emerald-700">{{ agentMessage }}</p>
                </div>
              </div>
            </aside>

            <section class="space-y-5">
              <div class="rounded-card-lg border border-border-default bg-elevated p-5 shadow-card">
                <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <h2 class="font-bold">Flow 节点</h2>
                    <p class="mt-1 text-sm text-text-secondary">{{ selectedAgent ? selectedAgent.agentId : '请选择 Agent' }}</p>
                  </div>
                  <UiButton variant="secondary" :disabled="!selectedAgentId" @click="resetFlowForm">新增 Flow</UiButton>
                </div>
                <div class="overflow-x-auto">
                  <table class="w-full min-w-[760px] text-left text-sm">
                    <thead class="text-xs uppercase tracking-wide text-text-tertiary">
                      <tr class="border-b border-border-subtle">
                        <th class="py-2">Flow</th>
                        <th class="py-2">Role</th>
                        <th class="py-2">Client</th>
                        <th class="py-2">Prompt</th>
                        <th class="py-2">Seq</th>
                        <th class="py-2">Status</th>
                        <th class="py-2 text-right">Action</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="flow in flows" :key="flow.flowId" class="border-b border-border-subtle">
                        <td class="py-3 font-mono text-xs">{{ flow.flowId }}</td>
                        <td class="py-3">{{ flow.roleType }}</td>
                        <td class="py-3 font-mono text-xs">{{ flow.clientId }}</td>
                        <td class="py-3 font-mono text-xs">{{ flow.promptId || '-' }}</td>
                        <td class="py-3">{{ flow.sortOrder }}</td>
                        <td class="py-3">{{ flow.status === 1 ? '启用' : '禁用' }}</td>
                        <td class="py-3 text-right">
                          <button class="mr-3 text-accent" type="button" @click="editFlow(flow)">编辑</button>
                          <button class="mr-3 text-text-secondary" type="button" @click="toggleFlow(flow)">{{ flow.status === 1 ? '禁用' : '启用' }}</button>
                          <button class="text-red-600" type="button" @click="deleteFlow(flow)">删除</button>
                        </td>
                      </tr>
                      <tr v-if="flows.length === 0">
                        <td class="py-8 text-center text-text-tertiary" colspan="7">暂无 Flow</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>

              <div class="rounded-card-lg border border-border-default bg-elevated p-5 shadow-card">
                <h2 class="mb-4 font-bold">{{ editingFlowId ? '编辑 Flow' : '新增 Flow' }}</h2>
                <div class="grid gap-3 md:grid-cols-2">
                  <input v-model="flowForm.flowId" :disabled="Boolean(editingFlowId)" class="rounded-card border border-border-default bg-surface px-3 py-2 text-sm outline-none focus:border-accent" placeholder="flowId" />
                  <select v-model="flowForm.roleType" class="rounded-card border border-border-default bg-surface px-3 py-2 text-sm outline-none focus:border-accent">
                    <option v-for="role in roleOptions" :key="role" :value="role">{{ role }}</option>
                  </select>
                  <select v-model="flowForm.clientId" class="rounded-card border border-border-default bg-surface px-3 py-2 text-sm outline-none focus:border-accent">
                    <option value="">选择 Client</option>
                    <option v-for="client in options.clients" :key="client.configId" :value="client.configId">{{ optionLabel(client) }}</option>
                  </select>
                  <select v-model="flowForm.promptId" class="rounded-card border border-border-default bg-surface px-3 py-2 text-sm outline-none focus:border-accent">
                    <option value="">不绑定 Prompt</option>
                    <option v-for="prompt in options.prompts" :key="prompt.configId" :value="prompt.configId">{{ optionLabel(prompt) }}</option>
                  </select>
                  <input v-model.number="flowForm.sortOrder" class="rounded-card border border-border-default bg-surface px-3 py-2 text-sm outline-none focus:border-accent" min="1" type="number" placeholder="sortOrder" />
                  <select v-model.number="flowForm.status" class="rounded-card border border-border-default bg-surface px-3 py-2 text-sm outline-none focus:border-accent">
                    <option :value="1">启用</option>
                    <option :value="0">禁用</option>
                  </select>
                </div>
                <div class="mt-4 flex flex-wrap items-center gap-3">
                  <UiButton :disabled="!selectedAgentId" :loading="loading" @click="saveFlow">保存 Flow</UiButton>
                  <UiButton variant="secondary" @click="resetFlowForm">重置</UiButton>
                  <p v-if="flowMessage" class="rounded-card bg-emerald-50 px-3 py-2 text-xs text-emerald-700">{{ flowMessage }}</p>
                </div>
                <div class="mt-5 rounded-card border border-border-subtle bg-surface p-4">
                  <div class="mb-2 text-sm font-semibold text-text-secondary">MCP 绑定提示</div>
                  <p class="text-sm leading-relaxed text-text-secondary">
                    MCP 仍通过 Config 页面绑定到 Client。当前页面只负责 Flow 到 Client 和 Prompt 的绑定，Work 执行会沿用 Client 的 MCP 配置。
                  </p>
                </div>
              </div>
            </section>
          </div>
        </section>
      </main>
      <Footer />
    </div>
  </div>
</template>
