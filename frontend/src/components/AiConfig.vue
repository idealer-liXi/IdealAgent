<script setup>
import { onMounted, ref, watch } from 'vue'
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

const kindsWithTemplate = [
  { key: 'api', label: 'API', template: { configId: 'api_openai_local', name: 'OpenAI Compatible', type: 'openai', content: 'https://api.openai.com', secret: '', status: 1, ownerId: 0 } },
  { key: 'model', label: 'Model', template: { configId: 'model_gpt_local', name: 'gpt-4o-mini', type: 'chat', refId: 'api_openai_local', status: 1, ownerId: 0 } },
  { key: 'client', label: 'Client', template: { configId: 'client_chat_local', name: 'Local Chat Client', type: 'chat', content: 'assistant', refId: 'model_gpt_local', secret: 'gpt-4o-mini', status: 1, ownerId: 0 } },
  { key: 'prompt', label: 'Prompt', template: { configId: 'prompt_system_local', name: 'System Prompt', type: 'system', content: 'You are IdealAgent.', status: 1, ownerId: 0 } },
  { key: 'advisor', label: 'Advisor', template: { configId: 'advisor_memory_local', name: 'Memory Advisor', type: 'memory', content: '{"retrieveSize":20}', status: 1 } },
  { key: 'mcp', label: 'MCP', template: { configId: 'mcp_stdio_local', name: 'Local MCP', type: 'stdio', content: '{"command":"node","args":[]}', secret: '', status: 1, ownerId: 0 } },
  { key: 'config', label: 'Binding', template: { configId: 'config_client_prompt_local', type: 'client', content: 'client_chat_local', secret: 'prompt', refId: 'prompt_system_local', status: 1 } }
]

const selectedKind = ref(kindsWithTemplate[0].key)
const payload = ref(JSON.stringify(kindsWithTemplate[0].template, null, 2))
const records = ref([])
const error = ref('')
const message = ref('')
const loading = ref(false)

watch(selectedKind, kind => {
  const config = kindsWithTemplate.find(item => item.key === kind)
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
</script>

<template>
  <div class="flex h-screen bg-surface text-text-primary">
    <Sidebar />
    <div class="flex min-w-0 flex-1 flex-col">
      <main class="flex-1 overflow-auto p-5">
        <section class="mx-auto max-w-7xl page-enter">
          <!-- Header -->
          <div class="mb-6">
            <p class="text-xs font-semibold uppercase tracking-widest text-accent">Config</p>
            <h1 class="mt-2 text-2xl font-bold">配置管理</h1>
            <p class="mt-2 text-sm text-text-secondary max-w-3xl">
              管理 API、模型、客户端、提示词、Advisor、MCP 和绑定关系。真实调用链会在后续阶段接入。
            </p>
          </div>

          <!-- Kind Selector -->
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

          <!-- Content Grid -->
          <div class="grid gap-5 lg:grid-cols-[380px_1fr]">
            <!-- Left: Editor -->
            <ConfigSection
              :title="kinds.find(k => k.key === selectedKind)?.label + ' 配置'"
              description="编辑 JSON 配置并保存"
            >
              <UiInput
                v-model="payload"
                type="textarea"
                :rows="16"
                class="mt-4 font-mono text-sm"
              />

              <div class="mt-4 flex gap-3">
                <UiButton variant="primary" :loading="loading" full-width @click="createRecord">
                  {{ loading ? '保存中...' : '保存配置' }}
                </UiButton>
              </div>

              <div v-if="message" class="mt-4 rounded-card-md bg-emerald-50 px-4 py-3 text-sm text-emerald-600">
                {{ message }}
              </div>
              <div v-if="error" class="mt-4 rounded-card-md bg-error-bg px-4 py-3 text-sm text-error">
                {{ error }}
              </div>
            </ConfigSection>

            <!-- Right: Records Table -->
            <ConfigSection title="当前记录" description="已保存的配置列表">
              <template #action>
                <UiButton variant="secondary" size="sm" @click="loadRecords">刷新</UiButton>
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
                    </tr>
                    <tr v-if="records.length === 0">
                      <td class="px-4 py-10 text-center text-text-tertiary" colspan="5">暂无配置</td>
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
