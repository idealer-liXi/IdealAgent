<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import request from '../request/request'
import { getToken } from '../utils/auth'
import Sidebar from './Sidebar.vue'
import Footer from './Footer.vue'
import UiButton from './ui/UiButton.vue'

const overviewTypes = new Set(['summarizer_overview', 'replier_overview', 'evaluator_overview'])

const agents = ref([])
const ragTags = ref([])
const agentId = ref('')
const selectedRagTag = ref('')
const content = ref('')
const cards = ref([])
const messages = ref([])
const finalAnswer = ref('')
const error = ref('')
const agentError = ref('')
const loading = ref(false)
const maxRetry = ref(2)
const maxRound = ref(2)
const maxPace = ref(3)
const sessionId = ref('')
const processListRef = ref(null)

const selectedAgent = computed(() => agents.value.find(item => item.agentId === agentId.value) || null)
const canSend = computed(() => Boolean(agentId.value && content.value.trim() && !loading.value))

onMounted(() => {
  loadAgents()
  loadRagTags()
})

watch(cards, scrollProcessToBottom, { deep: true })
watch(messages, scrollProcessToBottom, { deep: true })

async function loadAgents() {
  agentError.value = ''
  try {
    const response = await request.get('/ai/work/agents')
    agents.value = response.data.data || []
    if (!agentId.value) {
      agentId.value = agents.value[0]?.agentId || ''
    }
  } catch (e) {
    agentError.value = e.response?.data?.message || 'Work Agent 列表加载失败'
  }
}

async function loadRagTags() {
  try {
    const response = await request.get('/ai/rag/tags')
    ragTags.value = response.data.data || []
    if (selectedRagTag.value && !ragTags.value.some(tag => tag.ragTag === selectedRagTag.value)) {
      selectedRagTag.value = ''
    }
  } catch (e) {
    ragTags.value = []
  }
}

function scrollProcessToBottom() {
  nextTick(() => {
    if (processListRef.value) {
      processListRef.value.scrollTop = processListRef.value.scrollHeight
    }
  })
}

function newRun() {
  cards.value = []
  messages.value = []
  finalAnswer.value = ''
  error.value = ''
  sessionId.value = ''
  content.value = ''
}

function selectedAgentDesc() {
  return selectedAgent.value?.agentDesc || '暂无'
}

function clampNumber(value, min, max, fallback) {
  const numberValue = Number(value)
  if (!Number.isFinite(numberValue)) {
    return fallback
  }
  return Math.min(max, Math.max(min, Math.floor(numberValue)))
}

async function send() {
  const userMessage = content.value.trim()
  if (!userMessage) {
    error.value = '请输入任务内容'
    return
  }
  if (!agentId.value) {
    error.value = '请选择 Work Agent'
    return
  }
  error.value = ''
  loading.value = true
  cards.value = []
  finalAnswer.value = ''
  messages.value.push({ role: 'user', content: userMessage })
  content.value = ''
  try {
    const response = await fetch('/api/v1/ai/work/execute', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(getToken() ? { Authorization: `Bearer ${getToken()}` } : {})
      },
      body: JSON.stringify({
        agentId: agentId.value,
        agentDesc: selectedAgentDesc(),
        userMessage,
        ragTag: selectedRagTag.value || null,
        sessionId: sessionId.value || null,
        maxRetry: clampNumber(maxRetry.value, 1, 5, 2),
        maxRound: clampNumber(maxRound.value, 1, 5, 2),
        maxPace: clampNumber(maxPace.value, 1, 5, 3)
      })
    })
    if (!response.ok || !response.body) {
      throw new Error('Work Agent 执行失败')
    }
    await readStream(response)
  } catch (e) {
    error.value = e.message || 'Work Agent 执行失败'
  } finally {
    loading.value = false
  }
}

async function readStream(response) {
  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  while (true) {
    const { value, done } = await reader.read()
    if (done) {
      break
    }
    buffer += decoder.decode(value, { stream: true })
    buffer = consumeSseBuffer(buffer)
  }
  buffer += decoder.decode()
  consumeSseBuffer(buffer)
}

function consumeSseBuffer(buffer) {
  let next = buffer.replace(/\r\n/g, '\n')
  let separatorIndex = next.indexOf('\n\n')
  while (separatorIndex >= 0) {
    const block = next.slice(0, separatorIndex)
    handleSseBlock(block)
    next = next.slice(separatorIndex + 2)
    separatorIndex = next.indexOf('\n\n')
  }
  return next
}

function handleSseBlock(block) {
  const lines = block.split(/\r?\n/)
  let eventName = 'message'
  const data = []
  lines.forEach(line => {
    if (line.startsWith('event:')) {
      eventName = line.slice('event:'.length).trim()
    }
    if (line.startsWith('data:')) {
      data.push(line.slice('data:'.length).trimStart())
    }
  })
  const payload = data.join('\n')
  if (eventName === 'message' || eventName === 'complete') {
    appendWorkPayload(payload, eventName)
    return
  }
  if (eventName === 'error') {
    throw new Error(payload || 'Work Agent 执行失败')
  }
}

function appendWorkPayload(payload, eventName) {
  let parsed
  try {
    parsed = JSON.parse(payload)
  } catch (e) {
    cards.value.push({ sectionType: `${eventName}_raw`, sectionContent: payload })
    return
  }
  if (!parsed || typeof parsed !== 'object') {
    return
  }
  if (parsed.sessionId && !sessionId.value) {
    sessionId.value = parsed.sessionId
  }
  if (overviewTypes.has(parsed.sectionType)) {
    finalAnswer.value = parsed.sectionContent || finalAnswer.value
    upsertAssistantMessage(finalAnswer.value || '（无内容）')
    return
  }
  if (eventName === 'complete') {
    return
  }
  cards.value.push(parsed)
}

function upsertAssistantMessage(answer) {
  const last = messages.value[messages.value.length - 1]
  if (last?.role === 'assistant') {
    last.content = answer
    return
  }
  messages.value.push({ role: 'assistant', content: answer })
}

function cardMeta(card) {
  if (card.step !== null && card.step !== undefined) {
    return `step ${card.step}`
  }
  if (card.round !== null && card.round !== undefined) {
    return `round ${card.round}`
  }
  if (card.pace !== null && card.pace !== undefined) {
    return `pace ${card.pace}`
  }
  return ''
}
</script>

<template>
  <div class="flex h-screen bg-surface text-text-primary">
    <Sidebar />
    <div class="flex min-w-0 flex-1 flex-col">
      <main class="flex-1 overflow-hidden p-5 flex flex-col">
        <section class="grid flex-1 gap-5 lg:grid-cols-[360px_minmax(0,1fr)] page-enter min-h-0">
          <aside class="flex flex-col min-h-0 overflow-hidden">
            <div class="rounded-card-lg border border-border-default bg-elevated p-5 shadow-card flex-1 flex flex-col overflow-hidden min-h-0">
              <div class="flex-shrink-0">
                <div class="flex items-center justify-between mb-4">
                  <div>
                    <p class="text-xs font-semibold uppercase tracking-widest text-accent">Work</p>
                    <h1 class="mt-1 text-xl font-bold">智能体任务</h1>
                  </div>
                  <button class="text-sm font-medium text-accent hover:text-accent-hover" type="button" @click="loadAgents">
                    刷新
                  </button>
                </div>
                <p class="text-sm text-text-secondary leading-relaxed">
                  选择 Work Agent 后执行任务，实时查看 MiniAgent 风格的多角色执行过程。
                </p>

                <label class="mt-5 block">
                  <span class="mb-2 block text-sm font-semibold text-text-secondary">Agent</span>
                  <select
                    v-model="agentId"
                    class="w-full rounded-card border border-border-default bg-surface px-4 py-3 text-sm text-text-primary outline-none transition-colors focus:border-accent focus:ring-2 focus:ring-accent/10"
                    :disabled="loading"
                  >
                    <option value="">请选择 Work Agent</option>
                    <option v-for="agent in agents" :key="agent.agentId" :value="agent.agentId">
                      {{ agent.agentName || agent.agentId }} / {{ (agent.agentType || '').toUpperCase() }}
                    </option>
                  </select>
                </label>

                <div v-if="selectedAgent" class="mt-4 rounded-card border border-border-subtle bg-surface p-4">
                  <div class="flex items-center justify-between gap-3">
                    <span class="text-sm font-semibold">{{ selectedAgent.agentName || selectedAgent.agentId }}</span>
                    <span class="rounded-full bg-accent/10 px-2.5 py-1 text-xs font-bold uppercase text-accent">
                      {{ selectedAgent.agentType }}
                    </span>
                  </div>
                  <p class="mt-3 text-sm leading-relaxed text-text-secondary">
                    {{ selectedAgent.agentDesc || '暂无描述' }}
                  </p>
                </div>

                <label class="mt-4 block">
                  <span class="mb-2 block text-sm font-semibold text-text-secondary">RAG 知识库</span>
                  <select
                    v-model="selectedRagTag"
                    class="w-full rounded-card border border-border-default bg-surface px-4 py-3 text-sm text-text-primary outline-none transition-colors focus:border-accent focus:ring-2 focus:ring-accent/10"
                    :disabled="loading"
                  >
                    <option value="">不使用知识库</option>
                    <option v-for="tag in ragTags" :key="tag.ragTag" :value="tag.ragTag">
                      {{ tag.ragTag }}
                    </option>
                  </select>
                  <p class="mt-2 text-xs text-text-tertiary">RAG Advisor 只控制检索参数；这里决定本次 Agent 是否使用哪个知识库。</p>
                </label>

                <p v-if="agentError" class="mt-3 rounded-card border border-red-200 bg-red-50 px-3 py-2 text-xs text-red-700">{{ agentError }}</p>
                <p v-else-if="agents.length === 0" class="mt-3 rounded-card border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-700">
                  暂无启用的 Work Agent，请先启用 step、loop 或 react 类型 Agent。
                </p>

                <div class="mt-5 grid grid-cols-3 gap-3">
                  <label class="block">
                    <span class="mb-1 block text-xs font-semibold text-text-secondary">Retry</span>
                    <input v-model.number="maxRetry" class="w-full rounded-card border border-border-default bg-surface px-3 py-2 text-sm outline-none focus:border-accent" max="5" min="1" type="number" />
                  </label>
                  <label class="block">
                    <span class="mb-1 block text-xs font-semibold text-text-secondary">Round</span>
                    <input v-model.number="maxRound" class="w-full rounded-card border border-border-default bg-surface px-3 py-2 text-sm outline-none focus:border-accent" max="5" min="1" type="number" />
                  </label>
                  <label class="block">
                    <span class="mb-1 block text-xs font-semibold text-text-secondary">Pace</span>
                    <input v-model.number="maxPace" class="w-full rounded-card border border-border-default bg-surface px-3 py-2 text-sm outline-none focus:border-accent" max="5" min="1" type="number" />
                  </label>
                </div>

                <UiButton variant="secondary" full-width class="mt-4" :disabled="loading" @click="newRun">
                  新任务
                </UiButton>
              </div>

              <div class="mt-6 flex flex-col flex-1 min-h-0 overflow-hidden">
                <div class="mb-3 flex items-center justify-between text-sm text-text-secondary">
                  <span class="font-semibold">当前会话</span>
                  <span class="font-mono text-xs">{{ sessionId || '新会话' }}</span>
                </div>
                <div class="flex-1 min-h-0 overflow-y-auto scroll-smooth-thin -mr-2 pr-2 space-y-3">
                  <div v-for="message in messages" :key="message.role + message.content" class="rounded-card border border-border-subtle bg-surface px-4 py-3">
                    <div class="mb-1 text-xs font-semibold uppercase tracking-wide text-text-tertiary">{{ message.role }}</div>
                    <p class="whitespace-pre-wrap text-sm leading-relaxed">{{ message.content }}</p>
                  </div>
                  <div v-if="messages.length === 0" class="rounded-card border border-dashed border-border-default px-4 py-8 text-center text-sm text-text-tertiary">
                    输入任务后这里会显示用户请求和最终答案。
                  </div>
                </div>
              </div>
            </div>
          </aside>

          <section class="flex flex-col overflow-hidden min-h-0 rounded-card-lg border border-border-default bg-elevated shadow-card">
            <div class="flex-shrink-0 border-b border-border-subtle px-5 py-4">
              <div class="flex items-center justify-between gap-4">
                <div>
                  <p class="text-xs font-semibold uppercase tracking-widest text-accent">Execution Stream</p>
                  <h2 class="mt-1 text-lg font-bold">执行过程</h2>
                </div>
                <div class="text-sm text-text-secondary">
                  {{ cards.length }} 个过程事件
                </div>
              </div>
            </div>

            <div ref="processListRef" class="flex-1 min-h-0 overflow-y-auto px-6 py-5 scroll-smooth-thin">
              <div v-if="cards.length === 0" class="flex h-full min-h-[260px] flex-col items-center justify-center rounded-card border border-dashed border-border-default text-center text-text-tertiary">
                <p class="text-sm">执行后会实时展示 inspector、planner、runner 等角色输出。</p>
              </div>
              <div v-else class="space-y-4">
                <article v-for="(card, index) in cards" :key="`${card.sectionType}_${index}`" class="rounded-card-lg border border-border-default bg-surface p-4 shadow-card">
                  <div class="mb-3 flex flex-wrap items-center gap-2">
                    <span class="rounded-full bg-accent/10 px-2.5 py-1 text-xs font-bold text-accent">{{ card.clientType || 'system' }}</span>
                    <span class="text-sm font-semibold">{{ card.sectionType || 'message' }}</span>
                    <span v-if="cardMeta(card)" class="rounded-full bg-dark/5 px-2 py-0.5 text-xs font-medium text-text-secondary">{{ cardMeta(card) }}</span>
                  </div>
                  <pre class="whitespace-pre-wrap break-words text-sm leading-relaxed text-text-secondary font-sans">{{ card.sectionContent }}</pre>
                </article>
              </div>
            </div>

            <div class="flex-shrink-0 border-t border-border-subtle p-5">
              <div v-if="finalAnswer" class="mb-4 rounded-card border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800">
                <div class="mb-1 font-semibold">最终答案</div>
                <p class="whitespace-pre-wrap leading-relaxed">{{ finalAnswer }}</p>
              </div>
              <p v-if="error" class="mb-4 rounded-card border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{{ error }}</p>
              <textarea
                v-model="content"
                class="min-h-[96px] w-full resize-none rounded-card border border-border-default bg-surface px-4 py-3 text-sm text-text-primary outline-none transition-colors focus:border-accent focus:ring-2 focus:ring-accent/10"
                placeholder="输入要交给 Work Agent 执行的任务"
                :disabled="loading"
                @keydown.ctrl.enter.prevent="send"
                @keydown.meta.enter.prevent="send"
              />
              <div class="mt-3 flex items-center justify-between gap-3">
                <span class="text-xs text-text-tertiary">Ctrl/⌘ + Enter 执行</span>
                <UiButton :disabled="!canSend" :loading="loading" @click="send">
                  执行任务
                </UiButton>
              </div>
            </div>
          </section>
        </section>
      </main>
      <Footer />
    </div>
  </div>
</template>
