<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import request from '../request/request'
import { clearToken } from '../utils/auth'

const router = useRouter()
const kinds = [
  { key: 'api', label: 'API', template: { configId: 'api_openai_local', name: 'OpenAI Compatible', type: 'openai', content: 'https://api.openai.com', secret: '', status: 1, ownerId: 0 } },
  { key: 'model', label: 'Model', template: { configId: 'model_gpt_local', name: 'gpt-4o-mini', type: 'chat', refId: 'api_openai_local', status: 1, ownerId: 0 } },
  { key: 'client', label: 'Client', template: { configId: 'client_chat_local', name: 'Local Chat Client', type: 'chat', content: 'assistant', refId: 'model_gpt_local', secret: 'gpt-4o-mini', status: 1, ownerId: 0 } },
  { key: 'prompt', label: 'Prompt', template: { configId: 'prompt_system_local', name: 'System Prompt', type: 'system', content: 'You are IdealAgent.', status: 1, ownerId: 0 } },
  { key: 'advisor', label: 'Advisor', template: { configId: 'advisor_memory_local', name: 'Memory Advisor', type: 'memory', content: '{"retrieveSize":20}', status: 1 } },
  { key: 'mcp', label: 'MCP', template: { configId: 'mcp_stdio_local', name: 'Local MCP', type: 'stdio', content: '{"command":"node","args":[]}', secret: '', status: 1, ownerId: 0 } },
  { key: 'config', label: 'Binding', template: { configId: 'config_client_prompt_local', type: 'client', content: 'client_chat_local', secret: 'prompt', refId: 'prompt_system_local', status: 1 } }
]

const selectedKind = ref(kinds[0].key)
const payload = ref(JSON.stringify(kinds[0].template, null, 2))
const records = ref([])
const error = ref('')
const message = ref('')
const loading = ref(false)

watch(selectedKind, kind => {
  const config = kinds.find(item => item.key === kind)
  payload.value = JSON.stringify(config.template, null, 2)
  loadRecords()
})

onMounted(loadRecords)

async function loadRecords() {
  error.value = ''
  try {
    const response = await request.get(`/ai/config/${selectedKind.value}`)
    records.value = response.data.data || []
  } catch (e) {
    error.value = e.response?.data?.message || '配置列表加载失败'
  }
}

async function createRecord() {
  error.value = ''
  message.value = ''
  loading.value = true
  try {
    const body = JSON.parse(payload.value)
    const response = await request.post(`/ai/config/${selectedKind.value}`, body)
    if (response.data.code !== '0000') {
      error.value = response.data.message || '保存失败'
      return
    }
    message.value = `已保存 ${response.data.data.configId}`
    await loadRecords()
  } catch (e) {
    error.value = e instanceof SyntaxError ? 'JSON 格式不正确' : (e.response?.data?.message || '保存失败')
  } finally {
    loading.value = false
  }
}

function logout() {
  clearToken()
  router.push('/auth')
}
</script>

<template>
  <main class="min-h-screen bg-slate-950 px-5 py-8 text-slate-100 md:px-8">
    <section class="mx-auto max-w-7xl">
      <div class="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <p class="text-sm font-semibold uppercase tracking-[0.35em] text-cyan-300">Stage 3</p>
          <h1 class="mt-3 text-3xl font-black md:text-5xl">AI 配置中心</h1>
          <p class="mt-3 max-w-3xl text-slate-400">管理 API、模型、客户端、提示词、Advisor、MCP 和绑定关系。真实调用链会在后续阶段接入。</p>
        </div>
        <div class="flex gap-3">
          <RouterLink class="rounded-full border border-slate-700 px-4 py-2 text-sm text-cyan-200" to="/">首页</RouterLink>
          <button class="rounded-full border border-slate-700 px-4 py-2 text-sm text-cyan-200" @click="logout">退出</button>
        </div>
      </div>

      <div class="mt-8 grid gap-6 lg:grid-cols-[360px_1fr]">
        <aside class="rounded-3xl border border-slate-800 bg-slate-900/70 p-5">
          <p class="text-sm font-bold text-slate-300">配置类型</p>
          <div class="mt-4 grid grid-cols-2 gap-3">
            <button
              v-for="kind in kinds"
              :key="kind.key"
              class="rounded-2xl border px-4 py-3 text-left text-sm font-bold transition"
              :class="selectedKind === kind.key ? 'border-cyan-300 bg-cyan-300 text-slate-950' : 'border-slate-800 bg-slate-950 text-slate-300'"
              @click="selectedKind = kind.key"
            >
              {{ kind.label }}
            </button>
          </div>

          <label class="mt-6 block">
            <span class="text-sm font-bold text-slate-300">JSON Payload</span>
            <textarea v-model="payload" class="mt-3 min-h-96 w-full rounded-2xl border border-slate-800 bg-slate-950 p-4 font-mono text-sm leading-6 text-slate-200 outline-none focus:border-cyan-300" />
          </label>

          <button class="mt-4 w-full rounded-2xl bg-cyan-300 px-4 py-3 font-black text-slate-950 disabled:opacity-60" :disabled="loading" @click="createRecord">
            {{ loading ? '保存中...' : '保存配置' }}
          </button>
          <p v-if="message" class="mt-4 rounded-2xl border border-emerald-400/40 bg-emerald-400/10 px-4 py-3 text-sm text-emerald-200">{{ message }}</p>
          <p v-if="error" class="mt-4 rounded-2xl border border-red-400/40 bg-red-400/10 px-4 py-3 text-sm text-red-200">{{ error }}</p>
        </aside>

        <section class="rounded-3xl border border-slate-800 bg-slate-900/70 p-5">
          <div class="flex items-center justify-between">
            <h2 class="text-xl font-black">当前记录</h2>
            <button class="rounded-full border border-slate-700 px-4 py-2 text-sm text-cyan-200" @click="loadRecords">刷新</button>
          </div>
          <div class="mt-5 overflow-hidden rounded-2xl border border-slate-800">
            <table class="w-full min-w-[760px] text-left text-sm">
              <thead class="bg-slate-950 text-slate-400">
                <tr>
                  <th class="px-4 py-3">ID</th>
                  <th class="px-4 py-3">名称</th>
                  <th class="px-4 py-3">类型</th>
                  <th class="px-4 py-3">引用</th>
                  <th class="px-4 py-3">状态</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-800">
                <tr v-for="record in records" :key="record.configId" class="bg-slate-900/40">
                  <td class="px-4 py-3 font-mono text-cyan-200">{{ record.configId }}</td>
                  <td class="px-4 py-3">{{ record.name || record.content || '-' }}</td>
                  <td class="px-4 py-3">{{ record.type || record.configType || '-' }}</td>
                  <td class="px-4 py-3 font-mono text-slate-300">{{ record.refId || '-' }}</td>
                  <td class="px-4 py-3">{{ record.status }}</td>
                </tr>
                <tr v-if="records.length === 0">
                  <td class="px-4 py-10 text-center text-slate-500" colspan="5">暂无配置</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </div>
    </section>
  </main>
</template>
