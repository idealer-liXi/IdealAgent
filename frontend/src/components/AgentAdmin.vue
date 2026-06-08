<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../request/request'
import Sidebar from './Sidebar.vue'
import Footer from './Footer.vue'
import UiButton from './ui/UiButton.vue'

const router = useRouter()
const agents = ref([])
const modelOptions = ref([])
const editingAgentId = ref('')
const form = ref(emptyAgent())
const loading = ref(false)
const error = ref('')
const message = ref('')

onMounted(() => {
  loadAgents()
  loadModelOptions()
})

function emptyAgent() {
  return { agentId: '', agentName: '', agentType: 'step', agentDesc: '', modelId: '', templateId: '', status: 1 }
}

async function loadAgents() {
  error.value = ''
  try {
    const response = await request.get('/ai/admin/agents')
    agents.value = response.data.data || []
  } catch (e) {
    error.value = e.response?.data?.message || 'Agent 列表加载失败'
  }
}

async function loadModelOptions() {
  try {
    const response = await request.get('/ai/config/model')
    modelOptions.value = response.data.data || []
  } catch (e) {
    modelOptions.value = []
  }
}

function editAgent(agent) {
  editingAgentId.value = agent.agentId
  form.value = {
    agentId: agent.agentId || '',
    agentName: agent.agentName || '',
    agentType: agent.agentType || 'step',
    agentDesc: agent.agentDesc || '',
    modelId: agent.modelId || '',
    templateId: agent.templateId || '',
    status: agent.status ?? 1
  }
}

function resetForm() {
  editingAgentId.value = ''
  form.value = emptyAgent()
}

function payload() {
  return {
    agentId: editingAgentId.value ? trim(form.value.agentId) : '',
    agentName: trim(form.value.agentName),
    agentType: trim(form.value.agentType),
    agentDesc: trim(form.value.agentDesc),
    modelId: trim(form.value.modelId),
    templateId: trim(form.value.templateId),
    status: Number(form.value.status ?? 1)
  }
}

async function saveAgent() {
  loading.value = true
  error.value = ''
  message.value = ''
  try {
    const body = payload()
    if (!body.modelId) {
      error.value = 'Model ID 不能为空，创建 Agent 会按策略生成 Work Client'
      return
    }
    const response = editingAgentId.value
      ? await request.put(`/ai/admin/agents/${editingAgentId.value}`, body)
      : await request.post('/ai/admin/agents', body)
    message.value = `${editingAgentId.value ? '已更新' : '已创建'} ${response.data.data.agentId}`
    await loadAgents()
    editAgent(response.data.data)
  } catch (e) {
    error.value = e.response?.data?.message || 'Agent 保存失败'
  } finally {
    loading.value = false
  }
}

async function toggleAgent(agent) {
  try {
    await request.patch(`/ai/admin/agents/${agent.agentId}/status`, { status: agent.status === 1 ? 0 : 1 })
    await loadAgents()
  } catch (e) {
    error.value = e.response?.data?.message || 'Agent 状态更新失败'
  }
}

async function deleteAgent(agent) {
  if (!window.confirm(`确认删除 Agent ${agent.agentId}？`)) return
  try {
    await request.delete(`/ai/admin/agents/${agent.agentId}`)
    if (editingAgentId.value === agent.agentId) resetForm()
    await loadAgents()
  } catch (e) {
    error.value = e.response?.data?.message || 'Agent 删除失败'
  }
}

function trim(value) {
  return typeof value === 'string' ? value.trim() : value
}

function modelLabel(model) {
  const name = model.name || model.configId
  const api = model.refId ? ` / API: ${model.refId}` : ''
  return `${name} (${model.configId})${api}`
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
              <p class="text-xs font-semibold uppercase tracking-widest text-accent">AGENT</p>
              <h1 class="mt-2 text-2xl font-bold">AGENT 管理</h1>
              <p class="mt-2 text-sm text-text-secondary">管理智能体元信息；Flow 与 Canvas 在独立页面配置。</p>
            </div>
            <div class="flex gap-2">
              <UiButton variant="secondary" @click="router.push('/agents/flow')">FLOW 管理</UiButton>
              <UiButton variant="secondary" :loading="loading" @click="loadAgents">刷新</UiButton>
            </div>
          </div>

          <p v-if="error" class="mb-4 rounded-card border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{{ error }}</p>
          <p v-if="message" class="mb-4 rounded-card border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{{ message }}</p>

          <div class="grid gap-5 lg:grid-cols-[minmax(0,1fr)_360px]">
            <div class="overflow-hidden rounded-card-lg border border-border-default bg-elevated shadow-card">
              <table class="w-full text-left text-sm">
                <thead class="bg-surface text-xs uppercase tracking-wide text-text-tertiary">
                  <tr>
                    <th class="px-4 py-3">Agent</th>
                    <th class="px-4 py-3">策略</th>
                    <th class="px-4 py-3">描述</th>
                    <th class="px-4 py-3">状态</th>
                    <th class="px-4 py-3 text-right">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="agent in agents" :key="agent.agentId" class="border-t border-border-subtle">
                    <td class="px-4 py-3">
                      <div class="font-semibold">{{ agent.agentName || agent.agentId }}</div>
                      <div class="font-mono text-xs text-text-tertiary">{{ agent.agentId }}</div>
                    </td>
                    <td class="px-4 py-3 uppercase text-accent">{{ agent.agentType }}</td>
                    <td class="px-4 py-3 text-text-secondary">{{ agent.agentDesc || '-' }}</td>
                    <td class="px-4 py-3">{{ agent.status === 1 ? '启用' : '禁用' }}</td>
                    <td class="px-4 py-3 text-right">
                      <button class="mr-3 text-accent" type="button" @click="editAgent(agent)">编辑</button>
                      <button class="mr-3 text-accent" type="button" @click="router.push({ path: '/agents/flow', query: { agentId: agent.agentId } })">FLOW</button>
                      <button class="mr-3 text-text-secondary" type="button" @click="toggleAgent(agent)">{{ agent.status === 1 ? '禁用' : '启用' }}</button>
                      <button class="text-red-600" type="button" @click="deleteAgent(agent)">删除</button>
                    </td>
                  </tr>
                  <tr v-if="agents.length === 0">
                    <td colspan="5" class="px-4 py-10 text-center text-text-tertiary">暂无 Agent</td>
                  </tr>
                </tbody>
              </table>
            </div>

            <aside class="rounded-card-lg border border-border-default bg-elevated p-5 shadow-card">
              <div class="mb-4 flex items-center justify-between">
                <h2 class="font-bold">{{ editingAgentId ? '编辑 Agent' : '新增 Agent' }}</h2>
                <button class="text-sm text-accent" type="button" @click="resetForm">新增</button>
              </div>
              <div class="space-y-3">
                <div v-if="editingAgentId" class="rounded-card border border-border-default bg-surface px-3 py-2 text-sm text-text-secondary">
                  <div class="text-xs text-text-tertiary">Agent ID</div>
                  <div class="mt-1 font-mono text-text-primary">{{ form.agentId }}</div>
                </div>
                <input v-model="form.agentName" class="w-full rounded-card border border-border-default bg-surface px-3 py-2 text-sm outline-none focus:border-accent" placeholder="名称" />
                <select v-model="form.agentType" class="w-full rounded-card border border-border-default bg-surface px-3 py-2 text-sm outline-none focus:border-accent">
                  <option value="step">step</option>
                  <option value="loop">loop</option>
                  <option value="react">react</option>
                </select>
                <textarea v-model="form.agentDesc" class="min-h-[96px] w-full rounded-card border border-border-default bg-surface px-3 py-2 text-sm outline-none focus:border-accent" placeholder="描述" />
                <select v-model="form.modelId" class="w-full rounded-card border border-border-default bg-surface px-3 py-2 text-sm outline-none focus:border-accent">
                  <option value="" disabled>{{ modelOptions.length ? '请选择模型，用于生成策略 Client' : '暂无可用模型，请先创建 Model' }}</option>
                  <option v-for="model in modelOptions" :key="model.configId" :value="model.configId">{{ modelLabel(model) }}</option>
                </select>
                <select v-model="form.templateId" class="w-full rounded-card border border-border-default bg-surface px-3 py-2 text-sm outline-none focus:border-accent">
                  <option value="">使用内置策略模板</option>
                </select>
                <select v-model.number="form.status" class="w-full rounded-card border border-border-default bg-surface px-3 py-2 text-sm outline-none focus:border-accent">
                  <option :value="1">启用</option>
                  <option :value="0">禁用</option>
                </select>
                <UiButton full-width :loading="loading" @click="saveAgent">保存 Agent</UiButton>
              </div>
            </aside>
          </div>
        </section>
      </main>
      <Footer />
    </div>
  </div>
</template>
