<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import request from '../request/request'
import Sidebar from './Sidebar.vue'
import Footer from './Footer.vue'
import UiButton from './ui/UiButton.vue'
import { agentCreateFailureMessage, agentCreateSuccessMessage, requireSuccessResult } from './agent/agentFeedback'

const strategies = ['step', 'loop', 'react']
const models = ref([])
const mcps = ref([])
const loading = ref(false)
const initError = ref('')
const message = ref('')
const error = ref('')
const createdAgent = ref(null)

const form = reactive({
  agentName: '',
  agentDesc: '',
  strategy: 'step',
  modelId: '',
  mcpIdList: []
})

const selectedModelName = computed(() => models.value.find(item => item.configId === form.modelId)?.name || '')
const canCreate = computed(() => Boolean(form.agentName.trim() && form.agentDesc.trim() && form.strategy && form.modelId && !loading.value))

onMounted(loadOptions)

async function loadOptions() {
  initError.value = ''
  try {
    const [modelResponse, mcpResponse] = await Promise.all([
      request.get('/ai/models'),
      request.get('/ai/mcps')
    ])
    models.value = modelResponse.data.data || []
    mcps.value = mcpResponse.data.data || []
    if (!form.modelId && models.value.length > 0) {
      form.modelId = models.value[0].configId
    }
  } catch (e) {
    initError.value = e.response?.data?.message || 'Workshop 初始化失败'
  }
}

function toggleMcp(mcpId) {
  const next = new Set(form.mcpIdList)
  if (next.has(mcpId)) next.delete(mcpId)
  else next.add(mcpId)
  form.mcpIdList = [...next]
}

async function createAgent() {
  error.value = ''
  message.value = ''
  createdAgent.value = null
  if (!form.agentName.trim()) {
    error.value = '请输入 Agent 名称'
    return
  }
  if (!form.agentDesc.trim()) {
    error.value = '请输入任务描述'
    return
  }
  if (!form.modelId) {
    error.value = '请选择模型'
    return
  }
  loading.value = true
  try {
    const response = await request.post('/ai/workshop/agents', {
      agentName: form.agentName.trim(),
      agentDesc: form.agentDesc.trim(),
      strategy: form.strategy,
      modelId: form.modelId,
      mcpIdList: [...new Set(form.mcpIdList)]
    })
    createdAgent.value = requireSuccessResult(response.data)
    message.value = agentCreateSuccessMessage(createdAgent.value)
  } catch (e) {
    error.value = agentCreateFailureMessage(e)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="flex h-screen bg-surface text-text-primary">
    <Sidebar />
    <div class="flex min-w-0 flex-1 flex-col">
      <main class="flex-1 overflow-auto px-6 py-8">
        <section class="mx-auto max-w-6xl page-enter">
          <div class="mb-6">
            <p class="text-xs font-semibold uppercase tracking-widest text-accent">WORKSHOP</p>
            <h1 class="mt-3 text-3xl font-bold tracking-tight">一键创建 Work Agent</h1>
            <p class="mt-3 max-w-2xl text-sm leading-relaxed text-text-secondary">
              输入名称、策略和任务描述，选择模型与 MCP 工具，系统自动生成角色 Client、Prompt、Flow 和工具绑定。
            </p>
          </div>

          <p v-if="initError" class="mb-4 rounded-card border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{{ initError }}</p>
          <div class="rounded-card-lg border border-border-default bg-elevated p-5 shadow-card">
            <div class="space-y-5">
              <label class="block">
                <span class="mb-2 block text-sm font-semibold text-text-secondary">Agent 名称</span>
                <input v-model="form.agentName" class="w-full rounded-card border border-border-default bg-surface px-4 py-3 text-sm outline-none transition-colors focus:border-accent" placeholder="例如：新闻查询推送智能体" />
              </label>

              <div>
                <span class="mb-2 block text-sm font-semibold text-text-secondary">模型选择</span>
                <div class="flex flex-wrap gap-2">
                  <button
                    v-for="model in models"
                    :key="model.configId"
                    class="rounded-full border px-4 py-2 text-sm font-semibold transition-colors"
                    :class="form.modelId === model.configId ? 'border-accent bg-accent-light text-accent' : 'border-border-default bg-surface text-text-secondary hover:border-accent hover:text-accent'"
                    type="button"
                    @click="form.modelId = model.configId"
                  >
                    {{ model.name || model.configId }}
                  </button>
                  <span v-if="models.length === 0" class="rounded-full border border-dashed border-border-default px-4 py-2 text-sm text-text-tertiary">暂无可用模型</span>
                </div>
              </div>

              <div>
                <span class="mb-2 block text-sm font-semibold text-text-secondary">工具选择</span>
                <div class="flex flex-wrap gap-2">
                  <button
                    v-for="mcp in mcps"
                    :key="mcp.configId"
                    class="rounded-full border px-4 py-2 text-sm font-semibold transition-colors"
                    :class="form.mcpIdList.includes(mcp.configId) ? 'border-accent bg-accent-light text-accent' : 'border-border-default bg-surface text-text-secondary hover:border-accent hover:text-accent'"
                    type="button"
                    @click="toggleMcp(mcp.configId)"
                  >
                    {{ mcp.name || mcp.configId }}
                  </button>
                  <span v-if="mcps.length === 0" class="rounded-full border border-dashed border-border-default px-4 py-2 text-sm text-text-tertiary">暂无启用 MCP</span>
                </div>
              </div>

              <div>
                <span class="mb-2 block text-sm font-semibold text-text-secondary">执行策略</span>
                <div class="flex flex-wrap gap-2">
                  <button
                    v-for="strategy in strategies"
                    :key="strategy"
                    class="min-w-24 rounded-card border px-4 py-2 text-sm font-bold uppercase transition-colors"
                    :class="form.strategy === strategy ? 'border-accent bg-accent-light text-accent' : 'border-border-default bg-surface text-text-secondary hover:border-accent hover:text-accent'"
                    type="button"
                    @click="form.strategy = strategy"
                  >
                    {{ strategy }}
                  </button>
                </div>
              </div>

              <label class="block">
                <span class="mb-2 block text-sm font-semibold text-text-secondary">任务描述</span>
                <textarea
                  v-model="form.agentDesc"
                  class="min-h-[220px] w-full resize-none rounded-card border border-border-default bg-surface px-4 py-3 text-sm leading-relaxed outline-none transition-colors focus:border-accent"
                  placeholder="描述 Agent 要完成的任务目标、输入上下文、执行约束和产出格式。例如：查询网页最新新闻后，将新闻通过邮箱发送给指定用户。"
                />
              </label>

              <div class="flex flex-col gap-3 rounded-card border border-border-subtle bg-surface px-4 py-3 text-xs text-text-secondary md:flex-row md:items-center md:justify-between">
                <span>当前模型：{{ selectedModelName || form.modelId || '-' }}；选中工具：{{ form.mcpIdList.length }}</span>
                <UiButton :disabled="!canCreate" :loading="loading" @click="createAgent">一键创建</UiButton>
              </div>
              <p v-if="message" class="rounded-card border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-semibold text-emerald-700">{{ message }}</p>
              <p v-if="error" class="rounded-card border border-red-200 bg-red-50 px-4 py-3 text-sm font-semibold text-red-700">{{ error }}</p>
            </div>
          </div>
        </section>
      </main>
      <Footer />
    </div>
  </div>
</template>
