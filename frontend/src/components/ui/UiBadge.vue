<script setup>
/**
 * UiBadge - 胶囊标签组件
 * Variants: accent | success | neutral
 */
const props = defineProps({
  variant: { type: String, default: 'neutral' },
})

const variantClasses = {
  accent: 'bg-accent-light text-accent',
  success: 'bg-emerald-50 text-emerald-600',
  neutral: 'bg-surface text-text-secondary',
}
</script>

<template>
  <span
    :class="[
      'inline-flex items-center px-2.5 py-0.5 text-xs font-medium rounded-full',
      variantClasses[variant],
    ]"
  >
    <slot />
  </span>
</template>
