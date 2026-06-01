<script setup>
import { onMounted, ref } from 'vue'
import request from '../request/request'
import { getStoredProfile, setAuth } from '../utils/auth'
import Sidebar from './Sidebar.vue'
import Footer from './Footer.vue'
import UiCard from './ui/UiCard.vue'
import WelcomeStats from './welcome/WelcomeStats.vue'

const profile = ref(getStoredProfile())

onMounted(async () => {
  try {
    const response = await request.get('/user/profile')
    if (response.data.code === '0000') {
      profile.value = response.data.data
      setAuth(localStorage.getItem('ideal_agent_token'), response.data.data)
    }
  } catch {
    profile.value = null
  }
})
</script>

<template>
  <div class="flex h-screen bg-surface text-text-primary">
    <Sidebar />
    <div class="flex min-w-0 flex-1 flex-col">
      <main class="flex-1 overflow-auto px-6 py-8">
        <section class="mx-auto max-w-6xl page-enter">
          <!-- Header -->
          <div class="mb-8">
            <p class="text-xs font-semibold uppercase tracking-widest text-accent">Welcome</p>
            <h1 class="mt-3 text-3xl font-bold tracking-tight md:text-4xl">IdealAgent 仪表盘</h1>
            <p class="mt-3 max-w-2xl text-sm leading-relaxed text-text-secondary">
              当前已完成项目骨架、认证、配置管理和基础 Chat。后续阶段会继续补齐 RAG、MCP、Work Agent、Workspace 和 Admin。
            </p>
            <p v-if="profile" class="mt-2 text-sm text-text-tertiary">
              当前登录：{{ profile.userName }} · {{ profile.userRole }}
            </p>
          </div>

          <!-- Stats -->
          <WelcomeStats :profile="profile" />

          <!-- Quick Actions -->
          <div class="mt-6 grid gap-5 md:grid-cols-2">
            <RouterLink to="/chat" class="group block">
              <UiCard padding="lg" class="h-full">
                <div class="flex items-start gap-4">
                  <div class="flex h-12 w-12 shrink-0 items-center justify-center rounded-card-lg bg-accent-light text-xl">
                    💬
                  </div>
                  <div class="min-w-0 flex-1">
                    <h2 class="text-lg font-bold text-text-primary">新建 Chat</h2>
                    <p class="mt-1 text-sm text-text-secondary leading-relaxed">
                      进入对话页，使用当前本地 ChatClient 完成消息发送和会话持久化。
                    </p>
                    <div class="mt-4 inline-flex items-center gap-1 text-sm font-semibold text-accent group-hover:gap-2 transition-all">
                      <span>立即前往</span>
                      <span>→</span>
                    </div>
                  </div>
                </div>
              </UiCard>
            </RouterLink>

            <RouterLink to="/config" class="group block">
              <UiCard padding="lg" class="h-full">
                <div class="flex items-start gap-4">
                  <div class="flex h-12 w-12 shrink-0 items-center justify-center rounded-card-lg bg-accent-light text-xl">
                    ⚙️
                  </div>
                  <div class="min-w-0 flex-1">
                    <h2 class="text-lg font-bold text-text-primary">配置中心</h2>
                    <p class="mt-1 text-sm text-text-secondary leading-relaxed">
                      管理 API、Model、Client、Prompt、Advisor、MCP 和绑定关系。
                    </p>
                    <div class="mt-4 inline-flex items-center gap-1 text-sm font-semibold text-accent group-hover:gap-2 transition-all">
                      <span>立即前往</span>
                      <span>→</span>
                    </div>
                  </div>
                </div>
              </UiCard>
            </RouterLink>
          </div>
        </section>
      </main>
      <Footer />
    </div>
  </div>
</template>
