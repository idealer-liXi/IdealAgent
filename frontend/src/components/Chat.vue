<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import request from '../request/request'
import { getStoredProfile, getToken } from '../utils/auth'
import Sidebar from './Sidebar.vue'
import Footer from './Footer.vue'
import UiButton from './ui/UiButton.vue'
import ChatMessage from './chat/ChatMessage.vue'
import ChatInput from './chat/ChatInput.vue'
import SessionList from './chat/SessionList.vue'

const profile = ref(getStoredProfile())
const userName = computed(() => profile.value?.userName || '')

const clientId = ref('')
const clients = ref([])
const clientError = ref('')
const ragTags = ref([])
const selectedRagTag = ref('')
const mcpTools = ref([])
const selectedMcpIds = ref([])
const mcpError = ref('')
const ragPanelExpanded = ref(false)
const mcpPanelExpanded = ref(false)
const uploadPanelOpen = ref(false)
const ragTagInput = ref('')
const ragFiles = ref([])
const repoUrl = ref('')
const ragError = ref('')
const ragMessage = ref('')
const ragLoading = ref(false)
const sessionId = ref('')
const content = ref('')
const messages = ref([])
const sessions = ref([])
const error = ref('')
const loading = ref(false)
const isLocalEchoMode = computed(() => !clientId.value)

const messageListRef = ref(null)

function scrollToBottom() {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

watch(
  () => messages.value,
  () => scrollToBottom(),
  { deep: true }
)

onMounted(async () => {
  await Promise.all([loadSessions(), loadClients(), loadMcpTools()])
})

async function loadClients() {
  clientError.value = ''
  try {
    const response = await request.get('/ai/chat/clients')
    clients.value = response.data.data || []
    const defaultClient = clients.value.find(item => item.clientId === 'client_default_chat') || clients.value[0]
    clientId.value = defaultClient?.clientId || ''
    if (!clientId.value) {
      clientError.value = '暂无可用 Client，将使用本地回声模式'
      ragTags.value = []
      selectedRagTag.value = ''
      selectedMcpIds.value = []
      uploadPanelOpen.value = false
    } else {
      await loadRagTags()
    }
  } catch (e) {
    clientError.value = e.response?.data?.message || 'Client 列表加载失败'
  }
}

async function onClientChange() {
  if (isLocalEchoMode.value) {
    selectedRagTag.value = ''
    selectedMcpIds.value = []
    uploadPanelOpen.value = false
    ragError.value = ''
    return
  }
  await loadRagTags()
}

async function loadMcpTools() {
  mcpError.value = ''
  try {
    const response = await request.get('/ai/mcps')
    const list = response.data.data || []
    mcpTools.value = list
    const enabledIds = new Set(mcpTools.value.map(item => item.configId))
    selectedMcpIds.value = selectedMcpIds.value.filter(id => enabledIds.has(id))
  } catch (e) {
    mcpTools.value = []
    selectedMcpIds.value = []
    mcpError.value = e.response?.data?.message || 'MCP 工具列表加载失败'
  }
}

async function loadRagTags() {
  ragError.value = ''
  try {
    const response = await request.get('/ai/rag/tags')
    ragTags.value = response.data.data || []
  } catch (e) {
    ragError.value = e.response?.data?.message || '知识库列表加载失败'
  }
}

function onRagFilesChange(event) {
  ragFiles.value = Array.from(event.target.files || [])
}

function toggleUploadPanel() {
  uploadPanelOpen.value = !uploadPanelOpen.value
}

async function uploadRagFiles() {
  if (!ragTagInput.value.trim()) {
    ragError.value = '请输入知识库标签'
    return
  }
  if (ragFiles.value.length === 0) {
    ragError.value = '请选择文件'
    return
  }
  ragLoading.value = true
  ragError.value = ''
  ragMessage.value = ''
  try {
    const formData = new FormData()
    formData.append('ragTag', ragTagInput.value.trim())
    ragFiles.value.forEach(file => formData.append('fileList', file))
    await request.post('/ai/rag/file', formData)
    ragMessage.value = '文件已导入知识库'
    selectedRagTag.value = ragTagInput.value.trim()
    await loadRagTags()
  } catch (e) {
    ragError.value = e.response?.data?.message || '文件导入失败'
  } finally {
    ragLoading.value = false
  }
}

async function uploadGitRepo() {
  if (!repoUrl.value.trim()) {
    ragError.value = '请输入 Git 仓库地址'
    return
  }
  ragLoading.value = true
  ragError.value = ''
  ragMessage.value = ''
  try {
    await request.post('/ai/rag/git', {
      ragTag: ragTagInput.value.trim() || null,
      repoUrl: repoUrl.value.trim()
    })
    ragMessage.value = 'Git 仓库已导入知识库'
    await loadRagTags()
  } catch (e) {
    ragError.value = e.response?.data?.message || 'Git 导入失败'
  } finally {
    ragLoading.value = false
  }
}

function clientLabel(client) {
  const name = client.clientName || client.clientId
  const model = client.modelName || client.modelId || '未绑定模型'
  return `${name} / ${model}`
}

function mcpLabel(mcp) {
  return `${mcp.name || mcp.configId} (${mcp.type || 'mcp'})`
}

function selectedRagLabel() {
  return selectedRagTag.value || '未选择知识库'
}

function selectedMcpLabel() {
  if (!selectedMcpIds.value.length) return '未选择 MCP 工具'
  if (selectedMcpIds.value.length === 1) {
    const tool = mcpTools.value.find(item => item.configId === selectedMcpIds.value[0])
    return tool ? mcpLabel(tool) : selectedMcpIds.value[0]
  }
  return `已选择 ${selectedMcpIds.value.length} 个 MCP 工具`
}

async function loadSessions() {
  try {
    const response = await request.get('/ai/chat/sessions')
    sessions.value = response.data.data || []
  } catch (e) {
    error.value = e.response?.data?.message || '会话加载失败'
  }
}

async function loadMessages(nextSessionId) {
  sessionId.value = nextSessionId
  error.value = ''
  try {
    const response = await request.get(`/ai/chat/messages/${nextSessionId}`)
    messages.value = response.data.data || []
    scrollToBottom()
  } catch (e) {
    error.value = e.response?.data?.message || '消息加载失败'
  }
}

async function send() {
  if (!content.value.trim()) {
    error.value = '请输入消息内容'
    return
  }
  error.value = ''
  loading.value = true
  const userMessage = {
    messageId: `local_${Date.now()}`,
    sessionId: sessionId.value,
    role: 'user',
    content: content.value
  }
  const assistantMessage = {
    messageId: `local_assistant_${Date.now()}`,
    sessionId: sessionId.value,
    role: 'assistant',
    content: ''
  }
  messages.value.push(userMessage)
  messages.value.push(assistantMessage)
  // 获取响应式 proxy 引用，确保流式内容更新能触发 UI 渲染
  const reactiveAssistant = messages.value[messages.value.length - 1]
  const currentContent = content.value
  content.value = ''
  try {
    const response = await fetch('/api/v1/ai/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(getToken() ? { Authorization: `Bearer ${getToken()}` } : {})
      },
      body: JSON.stringify({
        sessionId: sessionId.value || null,
        clientId: clientId.value || null,
        content: currentContent,
        ragTag: isLocalEchoMode.value ? null : selectedRagTag.value || null,
        mcpIdList: isLocalEchoMode.value ? [] : selectedMcpIds.value
      })
    })
    if (!response.ok || !response.body) {
      throw new Error('发送失败')
    }
    await readStream(response, reactiveAssistant)
    await loadSessions()
  } catch (e) {
    error.value = e.message || '发送失败'
    if (!reactiveAssistant.content) {
      messages.value = messages.value.filter(message => message.messageId !== assistantMessage.messageId)
    }
  } finally {
    loading.value = false
  }
}

async function readStream(response, assistantMessage) {
  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  while (true) {
    const { value, done } = await reader.read()
    if (done) {
      break
    }
    buffer += decoder.decode(value, { stream: true })
    buffer = consumeSseBuffer(buffer, assistantMessage)
  }
  buffer += decoder.decode()
  consumeSseBuffer(buffer, assistantMessage)
}

function consumeSseBuffer(buffer, assistantMessage) {
  let next = buffer.replace(/\r\n/g, '\n')
  let separatorIndex = next.indexOf('\n\n')
  while (separatorIndex >= 0) {
    const block = next.slice(0, separatorIndex)
    handleSseBlock(block, assistantMessage)
    next = next.slice(separatorIndex + 2)
    separatorIndex = next.indexOf('\n\n')
  }
  return next
}

function handleSseBlock(block, assistantMessage) {
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
  if (eventName === 'delta') {
    assistantMessage.content += payload
    return
  }
  if (eventName === 'done') {
    const result = JSON.parse(payload)
    sessionId.value = result.sessionId
    Object.assign(assistantMessage, result.assistantMessage)
    return
  }
  if (eventName === 'error') {
    throw new Error(payload || '发送失败')
  }
}

function newSession() {
  sessionId.value = ''
  messages.value = []
  content.value = ''
  error.value = ''
}
</script>

<template>
  <div class="flex h-screen bg-surface text-text-primary">
    <Sidebar />
    <div class="flex min-w-0 flex-1 flex-col">
      <main class="flex-1 overflow-hidden p-5 flex flex-col">
        <section class="grid flex-1 gap-5 lg:grid-cols-[380px_minmax(0,1fr)] page-enter min-h-0">
          <!-- Left Panel -->
          <aside class="flex flex-col min-h-0 overflow-hidden">
            <div class="rounded-card-lg border border-border-default bg-elevated p-5 shadow-card flex-1 flex flex-col overflow-hidden min-h-0">
              <div class="flex-shrink-0">
                <div class="flex items-center justify-between mb-4">
                  <div>
                    <p class="text-xs font-semibold uppercase tracking-widest text-accent">Chat</p>
                    <h1 class="mt-1 text-xl font-bold">Chat</h1>
                  </div>
                </div>
                <p class="text-sm text-text-secondary leading-relaxed">
                  选择 Chat Client 发起对话，可按本次请求临时挂载知识库和 MCP 工具。
                </p>

                <label class="mt-5 block">
                  <span class="mb-2 block text-sm font-semibold text-text-secondary">Client</span>
                  <select
                    v-model="clientId"
                    @change="onClientChange"
                    class="w-full rounded-card border border-border-default bg-surface px-4 py-3 text-sm text-text-primary outline-none transition-colors focus:border-accent focus:ring-2 focus:ring-accent/10"
                  >
                    <option value="">本地回声模式</option>
                    <option v-for="client in clients" :key="client.clientId" :value="client.clientId">
                      {{ clientLabel(client) }}
                    </option>
                  </select>
                </label>
                <p v-if="clientError" class="mt-3 rounded-card border border-red-200 bg-red-50 px-3 py-2 text-xs text-red-700">{{ clientError }}</p>

                <div v-if="!isLocalEchoMode" class="mt-5 rounded-card border border-border-subtle bg-surface p-3">
                  <div class="flex items-center justify-between">
                    <span class="text-sm font-semibold text-text-secondary">RAG 知识库</span>
                    <div class="flex items-center gap-3">
                      <button class="text-xs font-medium text-accent" type="button" @click="loadRagTags">刷新</button>
                      <button class="text-xs font-medium text-accent" type="button" @click="ragPanelExpanded = !ragPanelExpanded">{{ ragPanelExpanded ? '收起' : '添加/选择' }}</button>
                    </div>
                  </div>
                  <p class="mt-2 rounded-card bg-elevated px-3 py-2 text-xs text-text-tertiary">{{ selectedRagLabel() }}</p>

                  <div v-if="ragPanelExpanded" class="mt-3">
                    <select v-model="selectedRagTag" class="w-full rounded-card border border-border-default bg-elevated px-3 py-2 text-sm outline-none focus:border-accent">
                      <option value="">不使用知识库</option>
                      <option v-for="tag in ragTags" :key="tag.ragTag" :value="tag.ragTag">{{ tag.ragTag }}</option>
                    </select>

                    <UiButton variant="secondary" size="sm" full-width class="mt-3" @click="toggleUploadPanel">
                      {{ uploadPanelOpen ? '收起上传知识库' : '上传知识库' }}
                    </UiButton>

                    <div v-if="uploadPanelOpen" class="mt-3">
                      <input v-model="ragTagInput" class="w-full rounded-card border border-border-default bg-elevated px-3 py-2 text-sm outline-none focus:border-accent" placeholder="知识库标签" />
                      <input class="mt-3 block w-full text-xs text-text-secondary" type="file" multiple accept=".txt,.md,.java,.html" @change="onRagFilesChange" />
                      <UiButton variant="secondary" size="sm" full-width class="mt-3" :loading="ragLoading" @click="uploadRagFiles">上传文件</UiButton>

                      <input v-model="repoUrl" class="mt-3 w-full rounded-card border border-border-default bg-elevated px-3 py-2 text-sm outline-none focus:border-accent" placeholder="Git repo URL" />
                      <UiButton variant="secondary" size="sm" full-width class="mt-3" :loading="ragLoading" @click="uploadGitRepo">导入 Git</UiButton>
                    </div>
                  </div>

                  <p v-if="ragMessage" class="mt-3 rounded-card bg-emerald-50 px-3 py-2 text-xs text-emerald-700">{{ ragMessage }}</p>
                  <p v-if="ragError" class="mt-3 rounded-card bg-red-50 px-3 py-2 text-xs text-red-700">{{ ragError }}</p>
                </div>

                <div v-if="!isLocalEchoMode" class="mt-4 rounded-card border border-border-subtle bg-surface p-3">
                  <div class="flex items-center justify-between">
                    <span class="text-sm font-semibold text-text-secondary">MCP 工具</span>
                    <div class="flex items-center gap-3">
                      <button class="text-xs font-medium text-accent" type="button" @click="loadMcpTools">刷新</button>
                      <button class="text-xs font-medium text-accent" type="button" @click="mcpPanelExpanded = !mcpPanelExpanded">{{ mcpPanelExpanded ? '收起' : '添加/选择' }}</button>
                    </div>
                  </div>
                  <p class="mt-2 rounded-card bg-elevated px-3 py-2 text-xs text-text-tertiary">{{ selectedMcpLabel() }}</p>
                  <div v-if="mcpPanelExpanded && mcpTools.length" class="mt-3 max-h-36 space-y-2 overflow-y-auto pr-1 scroll-smooth-thin">
                    <label v-for="tool in mcpTools" :key="tool.configId" class="flex items-start gap-2 rounded-card border border-border-subtle bg-elevated px-3 py-2 text-sm text-text-secondary">
                      <input v-model="selectedMcpIds" class="mt-1" type="checkbox" :value="tool.configId" />
                      <span>
                        <span class="block font-semibold text-text-primary">{{ mcpLabel(tool) }}</span>
                        <span class="block text-xs text-text-tertiary">{{ tool.configId }}</span>
                      </span>
                    </label>
                  </div>
                  <p v-else-if="mcpPanelExpanded" class="mt-3 rounded-card border border-dashed border-border-default px-3 py-4 text-center text-xs text-text-tertiary">暂无启用的 MCP 工具</p>
                  <p v-if="mcpError" class="mt-3 rounded-card bg-red-50 px-3 py-2 text-xs text-red-700">{{ mcpError }}</p>
                </div>

                <UiButton variant="primary" full-width class="mt-4" @click="newSession">
                  新会话
                </UiButton>
              </div>

              <div class="mt-6 flex flex-col flex-1 min-h-0 overflow-hidden">
                <div class="flex items-center justify-between mb-3 flex-shrink-0">
                  <h2 class="text-sm font-semibold text-text-secondary">历史会话</h2>
                  <button class="text-sm font-medium text-accent hover:text-accent-hover transition-colors" @click="loadSessions">
                    刷新
                  </button>
                </div>
                <div class="flex-1 min-h-0 overflow-y-auto scroll-smooth-thin -mr-2 pr-2">
                  <SessionList
                    :sessions="sessions"
                    :active-session-id="sessionId"
                    @select="loadMessages"
                  />
                </div>
              </div>
            </div>
          </aside>

          <!-- Right Panel -->
          <section class="flex flex-col overflow-hidden min-h-0 rounded-card-lg border border-border-default bg-elevated shadow-card">
            <div class="flex-shrink-0 border-b border-border-subtle px-5 py-4">
              <p class="font-mono text-sm text-text-tertiary">{{ sessionId || '新会话' }}</p>
            </div>

            <div ref="messageListRef" class="flex-1 min-h-0 space-y-5 overflow-y-auto px-6 py-5 scroll-smooth-thin">
              <ChatMessage
                v-for="message in messages"
                :key="message.messageId"
                :role="message.role"
                :content="message.content"
                :user-name="userName"
              />
              <div v-if="messages.length === 0" class="flex flex-col items-center justify-center py-24 text-text-tertiary">
                <div class="mb-3 text-4xl opacity-30">💬</div>
                <p class="text-sm">发送第一条消息开始对话</p>
              </div>
            </div>

            <ChatInput
              v-model="content"
              :loading="loading"
              :error="error"
              @send="send"
            />
          </section>
        </section>
      </main>
      <Footer />
    </div>
  </div>
</template>
