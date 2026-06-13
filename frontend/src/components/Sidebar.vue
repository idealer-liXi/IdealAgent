<script setup>
import { computed, ref, watchEffect } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { clearToken, getStoredProfile, isAdmin } from '../utils/auth'
import { buildSidebarItems, groupIsActive, itemIsActive } from './sidebar/sidebarNav'

const route = useRoute()
const router = useRouter()
const profile = computed(() => getStoredProfile())
const expandedGroups = ref({})
const navItems = computed(() => buildSidebarItems(isAdmin()))

watchEffect(() => {
  const next = { ...expandedGroups.value }
  navItems.value.forEach(item => {
    if (item.group && groupIsActive(item, route.path)) {
      next[item.label] = true
    }
  })
  expandedGroups.value = next
})

function logout() {
  clearToken()
  router.push('/auth')
}

function isActive(item) {
  return itemIsActive(item, route.path)
}

function isGroupExpanded(item) {
  return Boolean(expandedGroups.value[item.label])
}

function toggleGroup(item) {
  expandedGroups.value = {
    ...expandedGroups.value,
    [item.label]: !isGroupExpanded(item)
  }
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
      <template v-for="item in navItems" :key="item.label">
        <div v-if="item.group" class="space-y-1">
          <button
            class="group relative flex w-full items-center justify-between gap-3 rounded-card-md px-4 py-3 text-left transition-all duration-200 ease-smooth"
            :class="groupIsActive(item, route.path) ? 'bg-white/10 text-white' : 'text-white/60 hover:text-white hover:bg-white/5'"
            type="button"
            @click="toggleGroup(item)"
          >
            <div
              class="absolute left-0 top-1/2 h-5 w-0.5 -translate-y-1/2 rounded-full bg-accent transition-all duration-200 ease-out"
              :class="groupIsActive(item, route.path) ? 'scale-100 opacity-100' : 'scale-0 opacity-0 group-hover:scale-100 group-hover:opacity-50'"
            />
            <div class="flex flex-col">
              <span class="text-sm font-semibold">{{ item.label }}</span>
              <span class="text-xs text-white/40">{{ item.description }}</span>
            </div>
            <span class="text-xs transition-transform duration-200" :class="isGroupExpanded(item) ? 'rotate-90' : ''">›</span>
          </button>
          <div v-if="isGroupExpanded(item)" class="space-y-1 pl-3">
            <RouterLink
              v-for="child in item.children"
              :key="child.path"
              :to="child.path"
              class="group relative flex items-center gap-3 rounded-card-md px-4 py-2.5 transition-all duration-200 ease-smooth"
              :class="isActive(child) ? 'bg-white/10 text-white' : 'text-white/50 hover:bg-white/5 hover:text-white'"
            >
              <div
                class="absolute left-0 top-1/2 h-4 w-0.5 -translate-y-1/2 rounded-full bg-accent transition-all duration-200 ease-out"
                :class="isActive(child) ? 'scale-100 opacity-100' : 'scale-0 opacity-0 group-hover:scale-100 group-hover:opacity-50'"
              />
              <div class="flex flex-col">
                <span class="text-sm font-semibold">{{ child.label }}</span>
                <span class="text-xs text-white/35">{{ child.description }}</span>
              </div>
            </RouterLink>
          </div>
        </div>
        <RouterLink
          v-else
          :to="item.path"
          class="group relative flex items-center gap-3 rounded-card-md px-4 py-3 transition-all duration-200 ease-smooth"
          :class="isActive(item) ? 'bg-white/10 text-white' : 'text-white/60 hover:text-white hover:bg-white/5'"
        >
          <div
            class="absolute left-0 top-1/2 -translate-y-1/2 h-5 w-0.5 rounded-full bg-accent transition-all duration-200 ease-out"
            :class="isActive(item) ? 'opacity-100 scale-100' : 'opacity-0 scale-0 group-hover:opacity-50 group-hover:scale-100'"
          />
          <div class="flex flex-col">
            <span class="text-sm font-semibold">{{ item.label }}</span>
            <span class="text-xs text-white/40">{{ item.description }}</span>
          </div>
        </RouterLink>
      </template>
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
