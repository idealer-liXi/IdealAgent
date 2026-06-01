<script setup>
/**
 * UiButton - 统一按钮组件
 * Variants: primary | secondary | ghost
 * Sizes: sm | md | lg
 */
const props = defineProps({
  variant: { type: String, default: 'primary' },
  size: { type: String, default: 'md' },
  disabled: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
  fullWidth: { type: Boolean, default: false },
})

const emit = defineEmits(['click'])

const variantClasses = {
  primary: 'bg-accent text-white hover:bg-accent-hover shadow-card',
  secondary: 'bg-white text-text-primary border border-border-default hover:border-accent hover:text-accent shadow-card',
  ghost: 'bg-transparent text-text-secondary hover:bg-surface hover:text-text-primary',
}

const sizeClasses = {
  sm: 'px-3 py-1.5 text-xs rounded-card-sm',
  md: 'px-4 py-2.5 text-sm rounded-card-md',
  lg: 'px-6 py-3 text-sm font-semibold rounded-card-md',
}

function handleClick(e) {
  if (!props.disabled && !props.loading) {
    emit('click', e)
  }
}
</script>

<template>
  <button
    :class="[
      'inline-flex items-center justify-center gap-2 transition-all duration-150 ease-out select-none',
      'active:scale-[0.97] disabled:opacity-50 disabled:cursor-not-allowed disabled:active:scale-100',
      variantClasses[variant],
      sizeClasses[size],
      fullWidth ? 'w-full' : '',
    ]"
    :disabled="disabled || loading"
    @click="handleClick"
  >
    <slot name="prefix" />
    <slot>
      <span v-if="loading">处理中...</span>
    </slot>
    <slot name="suffix" />
  </button>
</template>
