<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '../request/request'
import Sidebar from './Sidebar.vue'
import Footer from './Footer.vue'
import UiButton from './ui/UiButton.vue'

const route = useRoute()
const router = useRouter()

const roleMap = {
  step: ['INSPECTOR', 'PLANNER', 'RUNNER', 'REPLIER'],
  loop: ['ANALYZER', 'PERFORMER', 'SUPERVISOR', 'SUMMARIZER'],
  react: ['OBSERVER', 'REASONER', 'ACTOR', 'EVALUATOR']
}

const agents = ref([])
const selectedAgent = ref(null)
const flows = ref([])
const graph = ref(null)
const promptDialog = reactive({ visible: false, title: '', content: '' })
const loading = ref(false)
const error = ref('')

const roleOrder = computed(() => roleMap[selectedAgent.value?.agentType || 'step'] || roleMap.step)
const slotList = computed(() => roleOrder.value.map((role, index) => {
  const lower = role.toLowerCase()
  const flow = flows.value.find(item => item.clientRole === lower) || flows.value.find(item => item.flowSeq === index + 1)
  return { role, seq: index + 1, flow }
}))
const missingSlots = computed(() => slotList.value.filter(slot => !slot.flow))
const flowComplete = computed(() => selectedAgent.value && missingSlots.value.length === 0)
const clients = computed(() => new Map((graph.value?.clients || []).map(item => [item.configId, item])))
const models = computed(() => new Map((graph.value?.models || []).map(item => [item.configId, item])))
const apis = computed(() => new Map((graph.value?.apis || []).map(item => [item.configId, item])))
const configGroups = computed(() => {
  const groups = new Map()
  ;(graph.value?.configs || []).forEach(item => {
    const clientId = item.content || item.ownerId
    if (!clientId) return
    if (!groups.has(clientId)) groups.set(clientId, [])
    groups.get(clientId).push(item)
  })
  return groups
})

onMounted(async () => {
  await loadAgents()
  const agentId = String(route.query.agentId || '')
  if (agentId) {
    const agent = agents.value.find(item => item.agentId === agentId)
    if (agent) await selectAgent(agent)
  }
})

async function loadAgents() {
  loading.value = true
  error.value = ''
  try {
    const response = await request.get('/ai/admin/flows/agents')
    agents.value = response.data.data || []
  } catch (e) {
    error.value = e.response?.data?.message || 'Agent 列表加载失败'
  } finally {
    loading.value = false
  }
}

async function selectAgent(agent) {
  selectedAgent.value = agent
  await loadFlows(agent.agentId)
  router.replace({ path: '/agents/flow', query: { agentId: agent.agentId } })
}

async function loadFlows(agentId) {
  loading.value = true
  error.value = ''
  try {
    const [flowResponse, graphResponse] = await Promise.all([
      request.get(`/ai/admin/flows/${agentId}`),
      request.get(`/ai/admin/canvas/${agentId}`)
    ])
    flows.value = flowResponse.data.data || []
    graph.value = graphResponse.data.data || null
  } catch (e) {
    error.value = e.response?.data?.message || 'Flow 加载失败'
  } finally {
    loading.value = false
  }
}

function backToGrid() {
  selectedAgent.value = null
  flows.value = []
  router.replace('/agents/flow')
}

function openPrompt(slot) {
  promptDialog.title = `${slot.role} Flow Prompt`
  promptDialog.content = slot.flow?.userPrompt || ''
  promptDialog.visible = true
}

function clientOf(flow) {
  return flow ? clients.value.get(flow.clientId) : null
}

function modelOf(flow) {
  const client = clientOf(flow)
  return client?.refId ? models.value.get(client.refId) : null
}

function apiOf(flow) {
  const model = modelOf(flow)
  return model?.refId ? apis.value.get(model.refId) : null
}

function configIds(flow, type) {
  if (!flow) return []
  return (configGroups.value.get(flow.clientId) || [])
    .filter(item => (item.configType || item.secret) === type)
    .map(item => item.refId)
    .filter(Boolean)
}

function closePrompt() {
  promptDialog.visible = false
  promptDialog.title = ''
  promptDialog.content = ''
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
              <p class="text-xs font-semibold uppercase tracking-widest text-accent">FLOW</p>
              <h1 class="mt-2 text-2xl font-bold">FLOW 管理</h1>
              <p class="mt-2 text-sm text-text-secondary">按 Agent 查看 MiniAgent 风格角色槽位。</p>
            </div>
            <div class="flex gap-2">
              <UiButton variant="secondary" @click="router.push('/agents')">返回 AGENT</UiButton>
              <UiButton variant="secondary" :loading="loading" @click="selectedAgent ? loadFlows(selectedAgent.agentId) : loadAgents()">刷新</UiButton>
            </div>
          </div>

          <p v-if="error" class="mb-4 rounded-card border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{{ error }}</p>

          <div v-if="!selectedAgent" class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            <button
              v-for="agent in agents"
              :key="agent.agentId"
              class="h-44 rounded-card-lg border border-border-default bg-elevated p-4 text-left shadow-card transition hover:-translate-y-1 hover:border-accent"
              type="button"
              @click="selectAgent(agent)"
            >
              <div class="flex items-center justify-between">
                <span class="rounded-full bg-accent/10 px-3 py-1 text-xs font-bold uppercase text-accent">{{ agent.agentType }}</span>
                <span :class="agent.status === 1 ? 'text-emerald-600' : 'text-red-600'" class="text-xs font-semibold">{{ agent.status === 1 ? '启用' : '禁用' }}</span>
              </div>
              <div class="mt-8 text-lg font-bold">{{ agent.agentName || agent.agentId }}</div>
              <p class="mt-3 line-clamp-2 text-sm text-text-secondary">{{ agent.agentDesc || '暂无描述' }}</p>
            </button>
            <div v-if="agents.length === 0" class="col-span-full rounded-card border border-dashed border-border-default p-10 text-center text-text-tertiary">暂无 Agent</div>
          </div>

          <div v-else class="space-y-5">
            <div class="flex flex-wrap items-center justify-between gap-3 rounded-card-lg border border-border-default bg-elevated p-5 shadow-card">
              <div>
                <div class="text-lg font-bold">{{ selectedAgent.agentName || selectedAgent.agentId }}</div>
                <div class="mt-1 text-sm text-text-secondary">{{ selectedAgent.agentId }} / {{ selectedAgent.agentType }}</div>
                <div class="mt-2 text-sm" :class="flowComplete ? 'text-emerald-600' : 'text-red-600'">
                  {{ flowComplete ? 'Flow 策略已完整，可作为 Work Agent 执行' : `Flow 策略未完整，缺少：${missingSlots.map(slot => slot.role).join(', ')}` }}
                </div>
              </div>
              <div class="flex gap-2">
                <UiButton variant="secondary" @click="router.push({ path: '/agents/canvas', query: { agentId: selectedAgent.agentId } })">查看配置图</UiButton>
                <UiButton variant="secondary" @click="backToGrid">返回列表</UiButton>
              </div>
            </div>

            <div class="grid gap-4 lg:grid-cols-4">
              <div v-for="slot in slotList" :key="slot.role" class="rounded-card-lg border border-border-default bg-elevated p-4 shadow-card">
                <div class="mb-4 text-center text-xl font-bold">{{ slot.role }}</div>
                <div v-if="slot.flow" class="space-y-3 rounded-card border border-border-subtle bg-surface p-4 text-sm">
                  <div><span class="text-text-tertiary">Client：</span><span class="font-mono text-xs">{{ slot.flow.clientId }}</span></div>
                  <div><span class="text-text-tertiary">API：</span><span class="font-mono text-xs">{{ apiOf(slot.flow)?.configId || '-' }}</span></div>
                  <div><span class="text-text-tertiary">Model：</span><span class="font-mono text-xs">{{ modelOf(slot.flow)?.configId || '-' }}</span></div>
                  <div><span class="text-text-tertiary">MCP：</span><span class="font-mono text-xs">{{ configIds(slot.flow, 'mcp').join(', ') || '-' }}</span></div>
                  <div><span class="text-text-tertiary">Advisor：</span><span class="font-mono text-xs">{{ configIds(slot.flow, 'advisor').join(', ') || '-' }}</span></div>
                  <div><span class="text-text-tertiary">Prompt：</span><span class="font-mono text-xs">{{ configIds(slot.flow, 'prompt').join(', ') || '-' }}</span></div>
                  <div><span class="text-text-tertiary">Role：</span>{{ slot.flow.clientRole }}</div>
                  <div><span class="text-text-tertiary">Seq：</span>{{ slot.flow.flowSeq }}</div>
                  <p class="line-clamp-4 text-text-secondary">{{ slot.flow.userPrompt }}</p>
                  <button class="text-sm font-semibold text-accent" type="button" @click="openPrompt(slot)">查看设定</button>
                </div>
                <div v-else class="flex h-44 items-center justify-center rounded-card border border-dashed border-border-default text-text-tertiary">未配置</div>
              </div>
            </div>
          </div>
        </section>
      </main>
      <Footer />
    </div>

    <div v-if="promptDialog.visible" class="fixed inset-0 z-50 grid place-items-center bg-black/30 p-4" @click="closePrompt">
      <div class="w-full max-w-2xl rounded-card-lg bg-elevated p-5 shadow-card" @click.stop>
        <div class="mb-3 text-lg font-bold">{{ promptDialog.title }}</div>
        <pre class="max-h-[420px] overflow-auto whitespace-pre-wrap rounded-card bg-surface p-4 text-sm text-text-secondary">{{ promptDialog.content }}</pre>
        <div class="mt-4 text-right"><UiButton @click="closePrompt">关闭</UiButton></div>
      </div>
    </div>
  </div>
</template>
