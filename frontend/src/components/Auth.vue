<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../request/request'
import { setAuth } from '../utils/auth'

const router = useRouter()
const mode = ref('login')
const userName = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

async function submit() {
  error.value = ''
  loading.value = true
  try {
    const url = mode.value === 'login' ? '/auth/login' : '/auth/register'
    const response = await request.post(url, {
      userName: userName.value,
      password: password.value
    })
    if (response.data.code !== '0000') {
      error.value = response.data.message || '认证失败'
      return
    }
    setAuth(response.data.data.token, response.data.data.profile)
    router.push('/')
  } catch (e) {
    error.value = e.response?.data?.message || '服务暂不可用'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="flex min-h-screen items-center justify-center bg-slate-950 px-6 py-12 text-slate-100">
    <section class="w-full max-w-md rounded-3xl border border-slate-800 bg-slate-900/80 p-8 shadow-2xl shadow-black/40">
      <p class="text-sm font-semibold uppercase tracking-[0.35em] text-cyan-300">IdealAgent</p>
      <h1 class="mt-4 text-3xl font-black">{{ mode === 'login' ? '登录' : '注册' }}</h1>
      <p class="mt-2 text-sm text-slate-400">第二阶段认证闭环：Token、请求拦截、用户上下文。</p>

      <form class="mt-8 space-y-5" @submit.prevent="submit">
        <label class="block">
          <span class="text-sm text-slate-300">用户名</span>
          <input v-model="userName" class="mt-2 w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 outline-none focus:border-cyan-300" autocomplete="username" />
        </label>
        <label class="block">
          <span class="text-sm text-slate-300">密码</span>
          <input v-model="password" type="password" class="mt-2 w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 outline-none focus:border-cyan-300" autocomplete="current-password" />
        </label>

        <p v-if="error" class="rounded-xl border border-red-500/40 bg-red-500/10 px-4 py-3 text-sm text-red-200">{{ error }}</p>

        <button class="w-full rounded-xl bg-cyan-300 px-4 py-3 font-bold text-slate-950 disabled:opacity-60" :disabled="loading">
          {{ loading ? '处理中...' : (mode === 'login' ? '登录' : '注册并登录') }}
        </button>
      </form>

      <button class="mt-5 text-sm text-cyan-200" @click="mode = mode === 'login' ? 'register' : 'login'">
        {{ mode === 'login' ? '没有账号？注册' : '已有账号？登录' }}
      </button>
    </section>
  </main>
</template>
