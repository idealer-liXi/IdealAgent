<script setup>
import { computed } from 'vue'

const props = defineProps({
  role: { type: String, required: true },
  content: { type: String, required: true },
  userName: { type: String, default: '' },
})

const isUser = computed(() => props.role === 'user')
</script>

<template>
  <div
    class="message-appear flex items-start gap-3"
    :class="isUser ? 'flex-row-reverse' : 'flex-row'"
  >
    <!-- Avatar -->
    <div
      class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full select-none overflow-hidden text-xs font-bold"
      :class="isUser
        ? 'bg-accent text-white'
        : 'bg-surface border border-border-default text-text-tertiary'
      "
    >
      <span v-if="isUser">me</span>
      <span v-else>IA</span>
    </div>

    <!-- Bubble Column -->
    <div class="flex flex-col max-w-[75%]" :class="isUser ? 'items-end' : 'items-start'">
      <!-- Name Tag -->
      <span class="text-[11px] text-text-tertiary mb-1 px-0.5">
        {{ isUser ? (userName || '我') : 'IdealAgent' }}
      </span>

      <!-- Message Bubble -->
      <div
        class="relative px-4 py-2.5 text-sm leading-6 whitespace-pre-wrap break-words"
        :class="isUser
          ? 'bg-accent text-white rounded-t-2xl rounded-l-2xl rounded-br-sm'
          : 'bg-surface border border-border-default text-text-primary rounded-t-2xl rounded-r-2xl rounded-bl-sm'
        "
      >
        {{ content }}
      </div>
    </div>
  </div>
</template>
