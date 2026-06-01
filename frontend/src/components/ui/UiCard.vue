<script setup>
/**
 * UiCard - 统一卡片容器
 * hoverable: 是否开启悬浮效果
 */
const props = defineProps({
  hoverable: { type: Boolean, default: true },
  padding: { type: String, default: 'lg' }, // sm | md | lg | xl
})

const paddingClasses = {
  sm: 'p-4',
  md: 'p-5',
  lg: 'p-6',
  xl: 'p-8',
}
</script>

<template>
  <div
    :class="[
      'bg-elevated rounded-card-lg border border-border-default',
      'transition-all duration-200 ease-smooth',
      hoverable ? 'shadow-card hover:shadow-card-hover hover:-translate-y-0.5' : 'shadow-card',
      paddingClasses[padding],
    ]"
  >
    <div v-if="$slots.header" class="mb-4">
      <slot name="header" />
    </div>
    <slot />
    <div v-if="$slots.footer" class="mt-4 pt-4 border-t border-border-subtle">
      <slot name="footer" />
    </div>
  </div>
</template>
