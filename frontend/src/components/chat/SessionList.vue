<script setup>
const props = defineProps({
  sessions: { type: Array, default: () => [] },
  activeSessionId: { type: String, default: '' },
})

const emit = defineEmits(['select', 'refresh'])
</script>

<template>
  <div class="space-y-2">
    <button
      v-for="item in sessions"
      :key="item.sessionId"
      class="group relative w-full rounded-card-md px-4 py-3 text-left text-sm transition-all duration-150"
      :class="activeSessionId === item.sessionId
        ? 'bg-accent-light'
        : 'bg-elevated hover:bg-surface'
      "
      @click="$emit('select', item.sessionId)"
    >
      <!-- Active indicator -->
      <div
        class="absolute left-0 top-1/2 -translate-y-1/2 h-4 w-0.5 rounded-full bg-accent transition-all duration-200"
        :class="activeSessionId === item.sessionId ? 'opacity-100' : 'opacity-0'"
      />
      <span class="block font-semibold text-text-primary truncate">{{ item.title || '未命名会话' }}</span>
      <span class="mt-1 block truncate font-mono text-xs text-text-tertiary">{{ item.sessionId }}</span>
    </button>
    <p v-if="sessions.length === 0" class="rounded-card-md border border-border-subtle bg-surface px-4 py-8 text-center text-sm text-text-tertiary">
      暂无历史会话
    </p>
  </div>
</template>
