<script setup>
import { onMounted, ref } from 'vue'
import request from '../request/request'
import Sidebar from './Sidebar.vue'
import Footer from './Footer.vue'
import UiButton from './ui/UiButton.vue'

const mcps = ref([])
const loading = ref(false)
const error = ref('')

onMounted(loadMcps)

async function loadMcps() {
  loading.value = true
  error.value = ''
  try {
    const response = await request.get('/ai/mcps')
    mcps.value = response.data.data || []
  } catch (e) {
    mcps.value = []
    error.value = e.response?.data?.message || 'MCP 展示加载失败'
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
          <div class="mb-6 flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
            <div>
              <p class="text-xs font-semibold uppercase tracking-widest text-accent">MCP DISPLAY</p>
              <h1 class="mt-3 text-3xl font-bold tracking-tight">MCP 展示</h1>
              <p class="mt-3 max-w-2xl text-sm leading-relaxed text-text-secondary">
                查看当前启用的 MCP 工具目录。此页面不会展示 Secret、API Key 或运行时环境变量。
              </p>
            </div>
            <UiButton variant="secondary" :loading="loading" @click="loadMcps">刷新</UiButton>
          </div>

          <p v-if="error" class="mb-4 rounded-card border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{{ error }}</p>

          <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            <article v-for="mcp in mcps" :key="mcp.configId" class="rounded-card-lg border border-border-default bg-elevated p-5 shadow-card">
              <div class="flex items-start justify-between gap-3">
                <div class="min-w-0">
                  <h2 class="truncate text-lg font-bold">{{ mcp.name || mcp.configId }}</h2>
                  <p class="mt-1 font-mono text-xs text-text-tertiary">{{ mcp.configId }}</p>
                </div>
                <span class="rounded-full bg-accent-light px-3 py-1 text-xs font-semibold uppercase text-accent">{{ mcp.type || 'mcp' }}</span>
              </div>
              <div class="mt-5 rounded-card border border-border-subtle bg-surface p-3 text-xs text-text-secondary">
                <div class="flex justify-between gap-3">
                  <span class="text-text-tertiary">Status</span>
                  <span class="font-semibold text-emerald-600">启用</span>
                </div>
              </div>
            </article>
          </div>

          <div v-if="!loading && mcps.length === 0" class="rounded-card-lg border border-dashed border-border-default bg-elevated px-6 py-12 text-center text-sm text-text-tertiary">
            暂无启用 MCP 工具
          </div>
        </section>
      </main>
      <Footer />
    </div>
  </div>
</template>
