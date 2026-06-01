<script setup>
import { onMounted, ref } from 'vue'
import request from '../request/request'
import Sidebar from './Sidebar.vue'
import Footer from './Footer.vue'
import UiButton from './ui/UiButton.vue'
import UiInput from './ui/UiInput.vue'
import ChatMessage from './chat/ChatMessage.vue'
import ChatInput from './chat/ChatInput.vue'
import SessionList from './chat/SessionList.vue'

const clientId = ref('client_default_chat')
const sessionId = ref('')
const content = ref('')
const messages = ref([])
const sessions = ref([])
const error = ref('')
const loading = ref(false)

onMounted(loadSessions)

async function loadSessions() {
  try {
    const response = await request.get('/chat/sessions')
    sessions.value = response.data.data || []
  } catch (e) {
    error.value = e.response?.data?.message || '会话加载失败'
  }
}

async function loadMessages(nextSessionId) {
  sessionId.value = nextSessionId
  error.value = ''
  try {
    const response = await request.get(`/chat/messages/${nextSessionId}`)
    messages.value = response.data.data || []
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
  messages.value.push(userMessage)
  const currentContent = content.value
  content.value = ''
  try {
    const response = await request.post('/chat/send', {
      sessionId: sessionId.value || null,
      clientId: clientId.value,
      content: currentContent
    })
    if (response.data.code !== '0000') {
      error.value = response.data.message || '发送失败'
      return
    }
    sessionId.value = response.data.data.sessionId
    messages.value.push(response.data.data.assistantMessage)
    await loadSessions()
  } catch (e) {
    error.value = e.response?.data?.message || '发送失败'
  } finally {
    loading.value = false
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
      <main class="flex-1 overflow-auto p-5">
        <section class="grid min-h-full gap-5 lg:grid-cols-[280px_1fr] page-enter">
          <!-- Left Panel -->
          <aside class="flex flex-col gap-5">
            <div class="rounded-card-lg border border-border-default bg-elevated p-5 shadow-card">
              <div class="flex items-center justify-between mb-4">
                <div>
                  <p class="text-xs font-semibold uppercase tracking-widest text-accent">Chat</p>
                  <h1 class="mt-1 text-xl font-bold">Stage 4 对话</h1>
                </div>
              </div>
              <p class="text-sm text-text-secondary leading-relaxed">
                使用本地 ChatClient 适配器，消息会写入 ai_session 和 ai_message。
              </p>

              <UiInput
                v-model="clientId"
                class="mt-5"
              >
                <template #label>Client ID</template>
              </UiInput>

              <UiButton variant="primary" full-width class="mt-4" @click="newSession">
                新会话
              </UiButton>

              <div class="mt-6">
                <div class="flex items-center justify-between mb-3">
                  <h2 class="text-sm font-semibold text-text-secondary">历史会话</h2>
                  <button class="text-sm font-medium text-accent hover:text-accent-hover transition-colors" @click="loadSessions">
                    刷新
                  </button>
                </div>
                <SessionList
                  :sessions="sessions"
                  :active-session-id="sessionId"
                  @select="loadMessages"
                />
              </div>
            </div>
          </aside>

          <!-- Right Panel -->
          <section class="flex min-h-[calc(100vh-6.5rem)] flex-col rounded-card-lg border border-border-default bg-elevated shadow-card">
            <div class="border-b border-border-subtle px-5 py-4">
              <p class="font-mono text-sm text-text-tertiary">{{ sessionId || '新会话' }}</p>
            </div>

            <div class="flex-1 space-y-4 overflow-y-auto p-5 scroll-smooth-thin">
              <ChatMessage
                v-for="message in messages"
                :key="message.messageId"
                :role="message.role"
                :content="message.content"
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
