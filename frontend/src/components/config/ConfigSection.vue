<script setup>
import UiCard from '../ui/UiCard.vue'

const props = defineProps({
  title: { type: String, required: true },
  description: { type: String, default: '' },
  bodyClass: { type: String, default: '' },
})
</script>

<template>
  <UiCard class="flex flex-col" :hoverable="false">
    <template #header>
      <div class="flex items-start justify-between">
        <div>
          <h2 class="text-lg font-bold text-text-primary">{{ title }}</h2>
          <p v-if="description" class="mt-1 text-sm text-text-secondary">{{ description }}</p>
        </div>
        <slot name="action" />
      </div>
    </template>
    <div class="min-h-0 flex-1" :class="bodyClass">
      <slot />
    </div>
  </UiCard>
</template>
