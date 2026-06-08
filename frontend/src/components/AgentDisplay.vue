<script setup>
import { computed, onMounted, ref } from 'vue'
import request from '../request/request'
import Sidebar from './Sidebar.vue'
import Footer from './Footer.vue'
import UiButton from './ui/UiButton.vue'

const agents = ref([])
const loading = ref(false)
const error = ref('')

const enabledAgents = computed(() => agents.value.filter(agent => agent.status === 1))

const strategyName = {
  step: 'Step',
  loop: 'Loop',
  react: 'ReAct'
}

onMounted(loadAgents)

async function loadAgents() {
  loading.value = true
  error.value = ''
  try {
    const response = await request.get('/ai/agents')
    agents.value = response.data.data || []
  } catch (e) {
    agents.value = []
    error.value = e.response?.data?.message || 'Agent 展示加载失败'
  } finally {
    loading.value = false
  }
}

function strategyLabel(type) {
  return strategyName[String(type || '').toLowerCase()] || (type || 'unknown')
}
</script>

<template>
  <div class="flex h-screen bg-surface text-text-primary">
    <Sidebar />
    <div class="flex min-w-0 flex-1 flex-col">
      <main class="flex-1 overflow-auto px-6 py-8">
        <section class="mx-auto max-w-6xl page-enter">
          <div class="mb-6 flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
            <div>
              <p class="text-xs font-semibold uppercase tracking-widest text-accent">AGENT DISPLAY</p>
              <h1 class="mt-3 text-3xl font-bold tracking-tight">Agent 展示</h1>
              <p class="mt-3 max-w-2xl text-sm leading-relaxed text-text-secondary">
                查看当前可用智能体的策略、模型和描述。这里是只读视图，管理入口仅管理员可用。
              </p>
            </div>
            <UiButton variant="secondary" :loading="loading" @click="loadAgents">刷新</UiButton>
          </div>

          <p v-if="error" class="mb-4 rounded-card border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{{ error }}</p>

          <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            <article v-for="agent in enabledAgents" :key="agent.agentId" class="rounded-card-lg border border-border-default bg-elevated p-5 shadow-card">
              <div class="flex items-start justify-between gap-3">
                <div class="min-w-0">
                  <h2 class="truncate text-lg font-bold">{{ agent.agentName || agent.agentId }}</h2>
                  <p class="mt-1 font-mono text-xs text-text-tertiary">{{ agent.agentId }}</p>
                </div>
                <span class="rounded-full bg-accent-light px-3 py-1 text-xs font-semibold text-accent">{{ strategyLabel(agent.agentType) }}</span>
              </div>
              <p class="mt-4 min-h-12 text-sm leading-relaxed text-text-secondary">{{ agent.agentDesc || '暂无描述' }}</p>
              <div class="mt-5 space-y-2 rounded-card border border-border-subtle bg-surface p-3 text-xs text-text-secondary">
                <div class="flex justify-between gap-3">
                  <span class="text-text-tertiary">Model</span>
                  <span class="truncate font-mono text-text-primary">{{ agent.modelId || '-' }}</span>
                </div>
                <div class="flex justify-between gap-3">
                  <span class="text-text-tertiary">Template</span>
                  <span class="truncate font-mono text-text-primary">{{ agent.templateId || '内置策略模板' }}</span>
                </div>
              </div>
            </article>
          </div>

          <div v-if="!loading && enabledAgents.length === 0" class="rounded-card-lg border border-dashed border-border-default bg-elevated px-6 py-12 text-center text-sm text-text-tertiary">
            暂无启用 Agent
          </div>
        </section>
      </main>
      <Footer />
    </div>
  </div>
</template>
