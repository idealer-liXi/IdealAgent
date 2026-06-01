<script setup>
import { computed } from 'vue'

const props = defineProps({
  role: { type: String, required: true },
  content: { type: String, required: true },
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
      class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full select-none overflow-hidden"
      :class="isUser
        ? 'bg-accent text-white text-xs font-bold'
        : 'bg-surface border border-border-default'
      "
    >
      <span v-if="isUser">我</span>
      <span v-else class="text-lg">🤖</span>
    </div>

    <!-- Bubble Column -->
    <div class="flex flex-col max-w-[75%]" :class="isUser ? 'items-end' : 'items-start'">
      <!-- Name Tag -->
      <span class="text-[11px] text-text-tertiary mb-1 px-0.5">
        {{ isUser ? '我' : 'IdealAgent' }}
      </span>

      <!-- Message Bubble -->
      <div
        class="relative px-4 py-2.5 text-sm leading-6 whitespace-pre-wrap break-words"
        :class="isUser
          ? 'bg-accent text-white rounded-t-2xl rounded-l-2xl rounded-br-sm'
          : 'bg-white border border-border-subtle text-text-primary rounded-t-2xl rounded-r-2xl rounded-bl-sm'
        "
      >
        {{ content }}
      </div>
    </div>
  </div>
</template>
