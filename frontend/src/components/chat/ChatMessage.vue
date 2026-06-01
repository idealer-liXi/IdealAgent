<script setup>
import { computed } from 'vue'

const props = defineProps({
  role: { type: String, required: true },
  content: { type: String, required: true },
})

const isUser = computed(() => props.role === 'user')
</script>

<template>
  <article
    class="message-appear max-w-3xl rounded-card-lg px-5 py-4 leading-7"
    :class="isUser
      ? 'ml-auto border-l-4 border-l-accent bg-elevated text-text-primary'
      : 'mr-auto bg-surface text-text-primary'
    "
  >
    <div class="flex items-center gap-2 mb-2">
      <div
        class="flex h-5 w-5 items-center justify-center rounded-full text-[10px] font-bold"
        :class="isUser ? 'bg-accent text-white' : 'bg-text-tertiary text-white'"
      >
        {{ isUser ? 'U' : 'A' }}
      </div>
      <span class="text-xs font-semibold uppercase tracking-wider text-text-tertiary">
        {{ role }}
      </span>
    </div>
    <p class="whitespace-pre-wrap text-sm">{{ content }}</p>
  </article>
</template>
