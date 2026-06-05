<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import request from '../request/request'
import Sidebar from './Sidebar.vue'
import Footer from './Footer.vue'
import UiButton from './ui/UiButton.vue'
import UiInput from './ui/UiInput.vue'
import ConfigSection from './config/ConfigSection.vue'

const kinds = [
  { key: 'api', label: 'API', description: 'LLM 服务接入配置', icon: '🔌' },
  { key: 'model', label: 'Model', description: '模型参数与选择', icon: '🧠' },
  { key: 'client', label: 'Client', description: 'ChatClient 实例配置', icon: '💬' },
  { key: 'prompt', label: 'Prompt', description: '系统提示词模板', icon: '📝' },
  { key: 'advisor', label: 'Advisor', description: '记忆与增强策略', icon: '🔧' },
  { key: 'mcp', label: 'MCP', description: 'MCP 工具接入', icon: '🛠️' },
  { key: 'config', label: 'Binding', description: '配置绑定关系', icon: '🔗' }
]

const templates = {
  api: { configId: 'api_deepseek', name: 'DeepSeek API', type: 'openai', content: 'https://api.deepseek.com', secret: '', status: 1, ownerId: 0 },
  model: { configId: 'model_deepseek_v4_flash', name: 'deepseek-v4-flash', type: 'chat', refId: '', status: 1, ownerId: 0 },
  client: { configId: 'client_deepseek_v4_flash', name: 'DeepSeek V4 Flash', type: 'chat', content: 'assistant', refId: '', secret: '', status: 1, ownerId: 0 },
  prompt: { configId: 'prompt_system_local', name: 'System Prompt', type: 'system', content: 'You are IdealAgent.', status: 1, ownerId: 0 },
  advisor: { configId: 'advisor_memory_local', name: 'Memory Advisor', type: 'memory', content: '{"retrieveSize":20}', status: 1, ownerId: 0 },
  mcp: { configId: 'mcp_amap', name: 'Amap Weather', type: 'sse', content: '{"baseUri":"http://localhost:9003","sseEndpoint":"/sse","timeoutMinutes":3}', secret: '{"key":""}', status: 1, ownerId: 0 },
  config: { configId: 'config_client_prompt_local', ownerType: 'client', content: '', configType: 'prompt', refId: '', status: 1 }
}

const mcpSseDefaults = { baseUri: 'http://localhost:9003', sseEndpoint: '/sse', timeoutMinutes: 3 }
const mcpStdioDefaults = { command: 'node', args: '[]', env: '{}', timeoutMinutes: 3 }

const selectedKind = ref('api')
const form = ref(cloneTemplate('api'))
const editingConfigId = ref('')
const records = ref([])
const allRecords = ref(emptyRecordMap())
const error = ref('')
const message = ref('')
const loading = ref(false)
const mcpSseForm = ref({ ...mcpSseDefaults })
const mcpStdioForm = ref({ ...mcpStdioDefaults })

const selectedKindMeta = computed(() => kinds.find(item => item.key === selectedKind.value) || kinds[0])
const ownerOptions = computed(() => optionsFor(form.value.ownerType))
const bindingTargetOptions = computed(() => optionsFor(form.value.configType))
const isEditing = computed(() => Boolean(editingConfigId.value))

const selectClass = 'w-full rounded-card-md border border-border-default bg-surface px-4 py-3 text-sm text-text-primary outline-none transition-all duration-150 ease-out focus:border-accent focus:bg-elevated focus:ring-2 focus:ring-accent-light'

watch(selectedKind, async kind => {
  resetForm(kind)
  message.value = ''
  error.value = ''
  await loadRecords()
})

onMounted(refreshAll)

async function refreshAll() {
  await Promise.all([loadAllRecords(), loadRecords()])
}

async function loadAllRecords() {
  const entries = await Promise.all(kinds.map(async kind => {
    try {
      const response = await request.get(`/ai/config/${kind.key}`)
      return [kind.key, response.data.data || []]
    } catch (e) {
      return [kind.key, []]
    }
  }))
  allRecords.value = Object.fromEntries(entries)
}

async function loadRecords() {
  error.value = ''
  try {
    const response = await request.get(`/ai/config/${selectedKind.value}`)
    records.value = response.data.data || []
  } catch (e) {
    error.value = e.response?.data?.message || '配置列表加载失败'
  }
}

async function saveRecord() {
  error.value = ''
  message.value = ''
  loading.value = true
  try {
    const body = toPayload(selectedKind.value, form.value)
    const response = isEditing.value
      ? await request.put(`/ai/config/${selectedKind.value}/${editingConfigId.value}`, body)
      : await request.post(`/ai/config/${selectedKind.value}`, body)
    if (response.data.code !== '0000') {
      error.value = response.data.message || '保存失败'
      return
    }
    message.value = `${isEditing.value ? '已更新' : '已保存'} ${response.data.data.configId}`
    await refreshAll()
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '保存失败'
  } finally {
    loading.value = false
  }
}

async function toggleStatus(record) {
  error.value = ''
  message.value = ''
  const nextStatus = record.status === 1 ? 0 : 1
  try {
    await request.patch(`/ai/config/${selectedKind.value}/${record.configId}/status`, { status: nextStatus })
    message.value = `${record.configId} 已${nextStatus === 1 ? '启用' : '禁用'}`
    await refreshAll()
  } catch (e) {
    error.value = e.response?.data?.message || '状态更新失败'
  }
}

async function deleteRecord(record) {
  if (!window.confirm(`确认删除 ${record.configId}？`)) {
    return
  }
  error.value = ''
  message.value = ''
  try {
    await request.delete(`/ai/config/${selectedKind.value}/${record.configId}`)
    message.value = `${record.configId} 已删除`
    if (editingConfigId.value === record.configId) {
      resetForm(selectedKind.value)
    }
    await refreshAll()
  } catch (e) {
    error.value = e.response?.data?.message || '删除失败'
  }
}

function editRecord(record) {
  editingConfigId.value = record.configId
  form.value = fromRecord(selectedKind.value, record)
  if (selectedKind.value === 'mcp') {
    syncMcpStructuredForm(form.value)
  }
  message.value = `正在编辑 ${record.configId}`
  error.value = ''
}

function resetForm(kind = selectedKind.value) {
  editingConfigId.value = ''
  form.value = cloneTemplate(kind)
  if (kind === 'mcp') {
    syncMcpStructuredForm(form.value)
  }
}

function cloneTemplate(kind) {
  return JSON.parse(JSON.stringify(templates[kind]))
}

function fromRecord(kind, record) {
  const next = cloneTemplate(kind)
  next.configId = record.configId || ''
  next.name = record.name || ''
  next.type = record.type || ''
  next.content = record.content || ''
  next.secret = record.secret || ''
  next.refId = record.refId || ''
  next.status = record.status ?? 1
  next.ownerId = record.ownerId ?? 0
  next.ownerType = record.ownerType || record.type || next.ownerType
  next.configType = record.configType || record.secret || next.configType
  return next
}

function emptyRecordMap() {
  return Object.fromEntries(kinds.map(kind => [kind.key, []]))
}

function optionsFor(kind) {
  return allRecords.value[kind] || []
}

function optionLabel(record) {
  const label = record.name || record.content || record.configId
  return `${label} (${record.configId})`
}

function onClientModelChange() {
  const model = optionsFor('model').find(item => item.configId === form.value.refId)
  form.value.secret = model?.name || ''
}

function onOwnerTypeChange() {
  form.value.content = ''
}

function onConfigTypeChange() {
  form.value.refId = ''
}

function onMcpTypeChange() {
  if (form.value.type === 'sse') {
    mcpSseForm.value = { ...mcpSseDefaults }
    return
  }
  if (form.value.type === 'stdio') {
    mcpStdioForm.value = { ...mcpStdioDefaults }
  }
}

function syncMcpStructuredForm(record) {
  const content = parseJsonObject(record.content)
  if (record.type === 'sse') {
    mcpSseForm.value = {
      baseUri: stringOrDefault(content.baseUri, mcpSseDefaults.baseUri),
      sseEndpoint: stringOrDefault(content.sseEndpoint, mcpSseDefaults.sseEndpoint),
      timeoutMinutes: positiveNumberOrDefault(content.timeoutMinutes, mcpSseDefaults.timeoutMinutes)
    }
    return
  }
  mcpStdioForm.value = {
    command: stringOrDefault(content.command, mcpStdioDefaults.command),
    args: JSON.stringify(Array.isArray(content.args) ? content.args : [], null, 2),
    env: JSON.stringify(content.env && typeof content.env === 'object' && !Array.isArray(content.env) ? content.env : {}, null, 2),
    timeoutMinutes: positiveNumberOrDefault(content.timeoutMinutes, mcpStdioDefaults.timeoutMinutes)
  }
}

function buildMcpContent(type) {
  if (type === 'sse') {
    return JSON.stringify({
      baseUri: trimmed(mcpSseForm.value.baseUri),
      sseEndpoint: trimmed(mcpSseForm.value.sseEndpoint) || '/sse',
      timeoutMinutes: positiveNumberOrDefault(mcpSseForm.value.timeoutMinutes, 3)
    })
  }
  const args = parseJsonArray(mcpStdioForm.value.args, 'stdio args')
  const env = parseJsonObjectStrict(mcpStdioForm.value.env, 'stdio env')
  return JSON.stringify({
    command: trimmed(mcpStdioForm.value.command),
    args,
    env,
    timeoutMinutes: positiveNumberOrDefault(mcpStdioForm.value.timeoutMinutes, 3)
  })
}

function parseJsonObject(value) {
  try {
    const parsed = JSON.parse(value || '{}')
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  } catch (e) {
    return {}
  }
}

function parseJsonObjectStrict(value, label) {
  const parsed = JSON.parse(value || '{}')
  if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
    throw new Error(`${label} 必须是 JSON Object`)
  }
  return parsed
}

function parseJsonArray(value, label) {
  const parsed = JSON.parse(value || '[]')
  if (!Array.isArray(parsed)) {
    throw new Error(`${label} 必须是 JSON Array`)
  }
  return parsed
}

function stringOrDefault(value, fallback) {
  return typeof value === 'string' && value.trim() ? value : fallback
}

function positiveNumberOrDefault(value, fallback) {
  const numberValue = Number(value)
  return Number.isFinite(numberValue) && numberValue > 0 ? Math.floor(numberValue) : fallback
}

function trimmed(value) {
  return typeof value === 'string' ? value.trim() : value
}

function statusOf(value) {
  return Number(value ?? 1)
}

function ownerIdOf(value) {
  return Number(value ?? 0)
}

function toPayload(kind, data) {
  const base = {
    configId: trimmed(data.configId),
    status: statusOf(data.status)
  }
  if (kind === 'api') {
    return { ...base, name: trimmed(data.name), type: trimmed(data.type), content: trimmed(data.content), secret: trimmed(data.secret), ownerId: ownerIdOf(data.ownerId) }
  }
  if (kind === 'model') {
    return { ...base, name: trimmed(data.name), type: 'chat', refId: trimmed(data.refId), ownerId: ownerIdOf(data.ownerId) }
  }
  if (kind === 'client') {
    return { ...base, name: trimmed(data.name), type: 'chat', content: trimmed(data.content), refId: trimmed(data.refId), secret: trimmed(data.secret), ownerId: ownerIdOf(data.ownerId) }
  }
  if (kind === 'prompt') {
    return { ...base, name: trimmed(data.name), type: trimmed(data.type), content: data.content, ownerId: ownerIdOf(data.ownerId) }
  }
  if (kind === 'advisor') {
    return { ...base, name: trimmed(data.name), type: trimmed(data.type), content: data.content, ownerId: ownerIdOf(data.ownerId) }
  }
  if (kind === 'mcp') {
    return { ...base, name: trimmed(data.name), type: trimmed(data.type), content: buildMcpContent(trimmed(data.type)), secret: trimmed(data.secret), ownerId: ownerIdOf(data.ownerId) }
  }
  return {
    ...base,
    type: trimmed(data.ownerType),
    content: trimmed(data.content),
    secret: trimmed(data.configType),
    refId: trimmed(data.refId),
    ownerType: trimmed(data.ownerType),
    configType: trimmed(data.configType)
  }
}
</script>

<template>
  <div class="flex h-screen bg-surface text-text-primary">
    <Sidebar />
    <div class="flex min-w-0 flex-1 flex-col">
      <main class="flex-1 overflow-auto p-5">
        <section class="mx-auto max-w-7xl page-enter">
          <div class="mb-6">
            <p class="text-xs font-semibold uppercase tracking-widest text-accent">Config</p>
            <h1 class="mt-2 text-2xl font-bold">配置管理</h1>
            <p class="mt-2 text-sm text-text-secondary max-w-3xl">
              使用表单管理 API、模型、客户端、提示词、Advisor、MCP 和绑定关系。引用字段通过下拉框选择已有配置。
            </p>
          </div>

          <div class="mb-6 flex flex-wrap gap-2">
            <button
              v-for="kind in kinds"
              :key="kind.key"
              class="flex items-center gap-2 rounded-card-md px-4 py-2.5 text-sm font-medium transition-all duration-150"
              :class="selectedKind === kind.key
                ? 'bg-accent text-white shadow-card'
                : 'bg-elevated border border-border-default text-text-secondary hover:text-text-primary hover:border-accent'
              "
              @click="selectedKind = kind.key"
            >
              <span>{{ kind.icon }}</span>
              <span>{{ kind.label }}</span>
            </button>
          </div>

          <div class="grid gap-5 lg:grid-cols-[420px_1fr]">
            <ConfigSection
              :title="selectedKindMeta.label + ' 配置'"
              description="填写表单并保存"
            >
              <div class="mt-4 space-y-4">
                <UiInput v-model="form.configId" placeholder="配置唯一 ID">
                  <template #label>配置 ID</template>
                </UiInput>

                <template v-if="selectedKind === 'api'">
                  <UiInput v-model="form.name" placeholder="DeepSeek API">
                    <template #label>名称</template>
                  </UiInput>
                  <label class="block">
                    <span class="mb-2 block text-sm font-medium text-text-secondary">类型</span>
                    <select v-model="form.type" :class="selectClass">
                      <option value="openai">OpenAI Compatible</option>
                    </select>
                  </label>
                  <UiInput v-model="form.content" placeholder="https://api.deepseek.com">
                    <template #label>Base URL</template>
                  </UiInput>
                  <UiInput v-model="form.secret" type="password" placeholder="sk-...">
                    <template #label>API Key</template>
                  </UiInput>
                </template>

                <template v-else-if="selectedKind === 'model'">
                  <UiInput v-model="form.name" placeholder="deepseek-v4-flash">
                    <template #label>模型 ID</template>
                  </UiInput>
                  <label class="block">
                    <span class="mb-2 block text-sm font-medium text-text-secondary">引用 API</span>
                    <select v-model="form.refId" :class="selectClass">
                      <option value="">请选择 API</option>
                      <option v-for="record in optionsFor('api')" :key="record.configId" :value="record.configId">
                        {{ optionLabel(record) }}
                      </option>
                    </select>
                  </label>
                </template>

                <template v-else-if="selectedKind === 'client'">
                  <UiInput v-model="form.name" placeholder="DeepSeek V4 Flash">
                    <template #label>名称</template>
                  </UiInput>
                  <label class="block">
                    <span class="mb-2 block text-sm font-medium text-text-secondary">角色</span>
                    <select v-model="form.content" :class="selectClass">
                      <option value="assistant">Assistant</option>
                    </select>
                  </label>
                  <label class="block">
                    <span class="mb-2 block text-sm font-medium text-text-secondary">引用 Model</span>
                    <select v-model="form.refId" :class="selectClass" @change="onClientModelChange">
                      <option value="">请选择 Model</option>
                      <option v-for="record in optionsFor('model')" :key="record.configId" :value="record.configId">
                        {{ optionLabel(record) }}
                      </option>
                    </select>
                  </label>
                  <UiInput v-model="form.secret" disabled placeholder="选择 Model 后自动填充">
                    <template #label>展示模型名</template>
                  </UiInput>
                </template>

                <template v-else-if="selectedKind === 'prompt'">
                  <UiInput v-model="form.name" placeholder="System Prompt">
                    <template #label>名称</template>
                  </UiInput>
                  <label class="block">
                    <span class="mb-2 block text-sm font-medium text-text-secondary">类型</span>
                    <select v-model="form.type" :class="selectClass">
                      <option value="system">System</option>
                      <option value="user">User</option>
                    </select>
                  </label>
                  <UiInput v-model="form.content" type="textarea" :rows="7" placeholder="You are IdealAgent.">
                    <template #label>提示词内容</template>
                  </UiInput>
                </template>

                <template v-else-if="selectedKind === 'advisor'">
                  <UiInput v-model="form.name" placeholder="Memory Advisor">
                    <template #label>名称</template>
                  </UiInput>
                  <label class="block">
                    <span class="mb-2 block text-sm font-medium text-text-secondary">类型</span>
                    <select v-model="form.type" :class="selectClass">
                      <option value="memory">Memory</option>
                    </select>
                  </label>
                  <UiInput v-model="form.content" type="textarea" :rows="5" placeholder='{"retrieveSize":20}'>
                    <template #label>配置内容</template>
                  </UiInput>
                </template>

                <template v-else-if="selectedKind === 'mcp'">
                  <UiInput v-model="form.name" placeholder="Local MCP">
                    <template #label>名称</template>
                  </UiInput>
                  <label class="block">
                    <span class="mb-2 block text-sm font-medium text-text-secondary">类型</span>
                    <select v-model="form.type" :class="selectClass" @change="onMcpTypeChange">
                      <option value="stdio">stdio</option>
                      <option value="sse">sse</option>
                    </select>
                  </label>

                  <template v-if="form.type === 'sse'">
                    <UiInput v-model="mcpSseForm.baseUri" placeholder="http://localhost:9003">
                      <template #label>baseUri</template>
                    </UiInput>
                    <UiInput v-model="mcpSseForm.sseEndpoint" placeholder="/sse">
                      <template #label>sseEndpoint</template>
                    </UiInput>
                    <UiInput v-model.number="mcpSseForm.timeoutMinutes" type="number" placeholder="3">
                      <template #label>timeoutMinutes</template>
                    </UiInput>
                    <p class="rounded-card bg-surface px-3 py-2 text-xs text-text-tertiary">
                      保存时自动生成 content：{"baseUri":"...","sseEndpoint":"/sse","timeoutMinutes":3}
                    </p>
                  </template>

                  <template v-else>
                    <UiInput v-model="mcpStdioForm.command" placeholder="node">
                      <template #label>command</template>
                    </UiInput>
                    <UiInput v-model="mcpStdioForm.args" type="textarea" :rows="4" placeholder='["server.js"]'>
                      <template #label>args JSON Array</template>
                    </UiInput>
                    <UiInput v-model="mcpStdioForm.env" type="textarea" :rows="4" placeholder='{"NODE_ENV":"production"}'>
                      <template #label>env JSON Object</template>
                    </UiInput>
                    <UiInput v-model.number="mcpStdioForm.timeoutMinutes" type="number" placeholder="3">
                      <template #label>timeoutMinutes</template>
                    </UiInput>
                    <p class="rounded-card bg-surface px-3 py-2 text-xs text-text-tertiary">
                      保存时自动生成 content：{"command":"node","args":[],"env":{},"timeoutMinutes":3}
                    </p>
                  </template>

                  <UiInput v-model="form.secret" type="textarea" :rows="6" placeholder='{"cookie":"...","categories":"后端","tags":"IdealAgent,MCP","coverUrl":"https://..."}'>
                    <template #label>密钥</template>
                  </UiInput>
                </template>

                <template v-else>
                  <label class="block">
                    <span class="mb-2 block text-sm font-medium text-text-secondary">Owner 类型</span>
                    <select v-model="form.ownerType" :class="selectClass" @change="onOwnerTypeChange">
                      <option value="client">Client</option>
                    </select>
                  </label>
                  <label class="block">
                    <span class="mb-2 block text-sm font-medium text-text-secondary">Owner</span>
                    <select v-model="form.content" :class="selectClass">
                      <option value="">请选择 Owner</option>
                      <option v-for="record in ownerOptions" :key="record.configId" :value="record.configId">
                        {{ optionLabel(record) }}
                      </option>
                    </select>
                  </label>
                  <label class="block">
                    <span class="mb-2 block text-sm font-medium text-text-secondary">绑定类型</span>
                    <select v-model="form.configType" :class="selectClass" @change="onConfigTypeChange">
                      <option value="prompt">Prompt</option>
                      <option value="advisor">Advisor</option>
                      <option value="mcp">MCP</option>
                    </select>
                  </label>
                  <label class="block">
                    <span class="mb-2 block text-sm font-medium text-text-secondary">引用对象</span>
                    <select v-model="form.refId" :class="selectClass">
                      <option value="">请选择引用对象</option>
                      <option v-for="record in bindingTargetOptions" :key="record.configId" :value="record.configId">
                        {{ optionLabel(record) }}
                      </option>
                    </select>
                  </label>
                </template>

                <label class="block">
                  <span class="mb-2 block text-sm font-medium text-text-secondary">状态</span>
                  <select v-model.number="form.status" :class="selectClass">
                    <option :value="1">启用</option>
                    <option :value="0">禁用</option>
                  </select>
                </label>

                <div class="flex gap-3">
                  <UiButton variant="primary" :loading="loading" full-width @click="saveRecord">
                    {{ loading ? '保存中...' : (isEditing ? '更新配置' : '保存配置') }}
                  </UiButton>
                  <UiButton v-if="isEditing" variant="secondary" @click="resetForm()">
                    取消编辑
                  </UiButton>
                </div>
              </div>

              <div v-if="message" class="mt-4 rounded-card-md bg-emerald-50 px-4 py-3 text-sm text-emerald-600">
                {{ message }}
              </div>
              <div v-if="error" class="mt-4 rounded-card-md bg-error-bg px-4 py-3 text-sm text-error">
                {{ error }}
              </div>
            </ConfigSection>

            <ConfigSection title="当前记录" description="已保存的配置列表">
              <template #action>
                <UiButton variant="secondary" size="sm" @click="refreshAll">刷新</UiButton>
              </template>

              <div class="mt-4 overflow-hidden rounded-card-md border border-border-subtle">
                <table class="w-full min-w-[640px] text-left text-sm">
                  <thead class="bg-surface text-text-secondary">
                    <tr>
                      <th class="px-4 py-3 font-medium">ID</th>
                      <th class="px-4 py-3 font-medium">名称</th>
                      <th class="px-4 py-3 font-medium">类型</th>
                      <th class="px-4 py-3 font-medium">引用</th>
                      <th class="px-4 py-3 font-medium">状态</th>
                      <th class="px-4 py-3 font-medium">操作</th>
                    </tr>
                  </thead>
                  <tbody class="divide-y divide-border-subtle">
                    <tr v-for="record in records" :key="record.configId" class="bg-elevated hover:bg-surface transition-colors">
                      <td class="px-4 py-3 font-mono text-xs text-accent">{{ record.configId }}</td>
                      <td class="px-4 py-3 text-text-primary">{{ record.name || record.content || '-' }}</td>
                      <td class="px-4 py-3">
                        <span class="inline-flex items-center rounded-full bg-surface px-2 py-0.5 text-xs font-medium text-text-secondary">
                          {{ record.type || record.configType || '-' }}
                        </span>
                      </td>
                      <td class="px-4 py-3 font-mono text-xs text-text-tertiary">{{ record.refId || '-' }}</td>
                      <td class="px-4 py-3">
                        <span
                          class="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium"
                          :class="record.status === 1 ? 'bg-emerald-50 text-emerald-600' : 'bg-gray-100 text-gray-500'"
                        >
                          {{ record.status === 1 ? '启用' : '禁用' }}
                        </span>
                      </td>
                      <td class="px-4 py-3">
                        <div class="flex flex-wrap gap-2">
                          <button class="text-xs font-medium text-accent hover:text-accent-hover" type="button" @click="editRecord(record)">编辑</button>
                          <button class="text-xs font-medium text-text-secondary hover:text-text-primary" type="button" @click="toggleStatus(record)">
                            {{ record.status === 1 ? '禁用' : '启用' }}
                          </button>
                          <button class="text-xs font-medium text-red-600 hover:text-red-700" type="button" @click="deleteRecord(record)">删除</button>
                        </div>
                      </td>
                    </tr>
                    <tr v-if="records.length === 0">
                      <td class="px-4 py-10 text-center text-text-tertiary" colspan="6">暂无配置</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </ConfigSection>
          </div>
        </section>
      </main>
      <Footer />
    </div>
  </div>
</template>
