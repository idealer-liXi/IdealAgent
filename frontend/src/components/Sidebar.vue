<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { clearToken, getStoredProfile } from '../utils/auth'

const route = useRoute()
const router = useRouter()
const profile = computed(() => getStoredProfile())

const navItems = [
  { path: '/welcome', label: 'Welcome', description: '仪表盘' },
  { path: '/chat', label: 'Chat', description: '对话会话' },
  { path: '/work', label: 'Work', description: '智能体任务' },
  { path: '/agents', label: 'Agent', description: '编排管理' },
  { path: '/config', label: 'Config', description: '配置管理' }
]

function logout() {
  clearToken()
  router.push('/auth')
}
</script>

<template>
  <aside class="flex h-screen w-60 shrink-0 flex-col bg-dark text-white shadow-sidebar">
    <!-- Logo -->
    <div class="px-5 py-6">
      <RouterLink class="flex items-center gap-3" to="/welcome">
        <div class="flex h-10 w-10 items-center justify-center rounded-card-md bg-accent text-base font-bold">
          IA
        </div>
        <div>
          <div class="text-base font-bold leading-tight tracking-tight">IdealAgent</div>
        </div>
      </RouterLink>
    </div>

    <!-- Navigation -->
    <nav class="flex-1 px-3 space-y-1">
      <RouterLink
        v-for="item in navItems"
        :key="item.path"
        :to="item.path"
        class="group relative flex items-center gap-3 rounded-card-md px-4 py-3 transition-all duration-200 ease-smooth"
        :class="route.path === item.path
          ? 'bg-white/10 text-white'
          : 'text-white/60 hover:text-white hover:bg-white/5'
        "
      >
        <!-- Active indicator line -->
        <div
          class="absolute left-0 top-1/2 -translate-y-1/2 h-5 w-0.5 rounded-full bg-accent transition-all duration-200 ease-out"
          :class="route.path === item.path ? 'opacity-100 scale-100' : 'opacity-0 scale-0 group-hover:opacity-50 group-hover:scale-100'"
        />
        <div class="flex flex-col">
          <span class="text-sm font-semibold">{{ item.label }}</span>
          <span class="text-xs text-white/40">{{ item.description }}</span>
        </div>
      </RouterLink>
    </nav>

    <!-- User -->
    <div class="px-4 pb-5">
      <div class="rounded-card-lg border border-white/10 bg-white/5 px-4 py-3">
        <div class="flex items-center gap-3">
          <div class="flex h-8 w-8 items-center justify-center rounded-full bg-accent/20 text-xs font-bold text-accent">
            {{ (profile?.userName || 'U').charAt(0).toUpperCase() }}
          </div>
          <div class="min-w-0 flex-1">
            <div class="truncate text-sm font-medium">{{ profile?.userName || 'User' }}</div>
            <div class="text-xs text-white/40">{{ profile?.userRole || 'user' }}</div>
          </div>
        </div>
        <button
          class="mt-3 w-full rounded-card-sm py-1.5 text-xs font-medium text-white/50 transition-colors hover:text-white hover:bg-white/10"
          type="button"
          @click="logout"
        >
          退出登录
        </button>
      </div>
    </div>
  </aside>
</template>
