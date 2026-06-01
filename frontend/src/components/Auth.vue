<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../request/request'
import { setAuth } from '../utils/auth'
import UiButton from './ui/UiButton.vue'
import UiInput from './ui/UiInput.vue'

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
  <main class="flex min-h-screen items-center justify-center bg-elevated px-6 py-12">
    <section class="w-full max-w-md page-enter">
      <div class="rounded-card-xl border border-border-default bg-elevated p-8 shadow-card-lg">
        <!-- Logo -->
        <div class="text-center">
          <div class="mx-auto flex h-14 w-14 items-center justify-center rounded-card-lg bg-accent text-2xl font-bold text-white">
            IA
          </div>
          <h1 class="mt-5 text-2xl font-bold text-text-primary">IdealAgent</h1>
          <p class="mt-2 text-sm text-text-secondary">{{ mode === 'login' ? '登录到您的账户' : '创建新账户' }}</p>
        </div>

        <form class="mt-8 space-y-5" @submit.prevent="submit">
          <UiInput
            v-model="userName"
            placeholder="请输入用户名"
            autocomplete="username"
          >
            <template #label>用户名</template>
          </UiInput>

          <UiInput
            v-model="password"
            type="password"
            placeholder="请输入密码"
            autocomplete="current-password"
          >
            <template #label>密码</template>
          </UiInput>

          <div v-if="error" class="rounded-card-md bg-error-bg px-4 py-3 text-sm text-error">
            {{ error }}
          </div>

          <UiButton variant="primary" size="lg" full-width :loading="loading" @click="submit">
            {{ loading ? '处理中...' : (mode === 'login' ? '登录' : '注册并登录') }}
          </UiButton>
        </form>

        <div class="mt-6 text-center">
          <button
            class="text-sm text-text-secondary hover:text-accent transition-colors"
            @click="mode = mode === 'login' ? 'register' : 'login'"
          >
            {{ mode === 'login' ? '没有账号？注册' : '已有账号？登录' }}
          </button>
        </div>
      </div>
    </section>
  </main>
</template>
