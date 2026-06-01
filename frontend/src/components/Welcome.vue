<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../request/request'
import { clearToken, getStoredProfile, setAuth } from '../utils/auth'

const router = useRouter()
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

function logout() {
  clearToken()
  router.push('/auth')
}
</script>

<template>
  <main class="min-h-screen bg-slate-950 px-6 py-16 text-slate-100">
    <section class="mx-auto max-w-4xl rounded-3xl border border-slate-800 bg-slate-900/70 p-10 shadow-2xl shadow-black/30">
      <div class="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <p class="text-sm font-semibold uppercase tracking-[0.35em] text-cyan-300">IdealAgent</p>
        <div class="flex items-center gap-3 text-sm text-slate-300">
          <span v-if="profile">{{ profile.userName }} · {{ profile.userRole }}</span>
          <RouterLink class="rounded-full border border-slate-700 px-4 py-2 text-cyan-200" to="/config">配置中心</RouterLink>
          <button class="rounded-full border border-slate-700 px-4 py-2 text-cyan-200" @click="logout">退出</button>
        </div>
      </div>
      <h1 class="mt-5 text-4xl font-black tracking-tight md:text-6xl">MiniAgent learning rebuild</h1>
      <p class="mt-6 max-w-2xl text-lg leading-8 text-slate-300">
        Stage 3 adds API, model, client, prompt, advisor, MCP, and binding configuration management. Later milestones connect these records to chat, RAG, MCP tools, and Work Agent workflows.
      </p>
      <div class="mt-8 grid gap-4 md:grid-cols-3">
        <div class="rounded-2xl bg-slate-800/70 p-5">
          <h2 class="font-bold text-cyan-200">Backend</h2>
          <p class="mt-2 text-sm text-slate-400">Spring Boot DDD modules.</p>
        </div>
        <div class="rounded-2xl bg-slate-800/70 p-5">
          <h2 class="font-bold text-cyan-200">Frontend</h2>
          <p class="mt-2 text-sm text-slate-400">Vue 3 and Vite shell.</p>
        </div>
        <div class="rounded-2xl bg-slate-800/70 p-5">
          <h2 class="font-bold text-cyan-200">Data</h2>
          <p class="mt-2 text-sm text-slate-400">MySQL and pgvector SQL included.</p>
        </div>
      </div>
    </section>
  </main>
</template>
