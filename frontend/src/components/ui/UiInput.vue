<script setup>
/**
 * UiInput - 统一输入框组件
 * Supports textarea via type="textarea"
 */
const props = defineProps({
  modelValue: { type: String, default: '' },
  type: { type: String, default: 'text' },
  placeholder: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
  rows: { type: Number, default: 3 },
  autocomplete: { type: String, default: '' },
})

const emit = defineEmits(['update:modelValue'])

const baseClasses = 'w-full rounded-card-md border border-border-default bg-surface px-4 py-3 text-sm text-text-primary placeholder:text-text-tertiary outline-none transition-all duration-150 ease-out focus:border-accent focus:bg-elevated focus:ring-2 focus:ring-accent-light'
</script>

<template>
  <label class="block">
    <span v-if="$slots.label" class="block mb-2 text-sm font-medium text-text-secondary">
      <slot name="label" />
    </span>
    <textarea
      v-if="type === 'textarea'"
      :class="baseClasses"
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      :rows="rows"
      @input="$emit('update:modelValue', $event.target.value)"
      @keydown="$emit('keydown', $event)"
    />
    <input
      v-else
      :type="type"
      :class="baseClasses"
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      :autocomplete="autocomplete"
      @input="$emit('update:modelValue', $event.target.value)"
      @keydown="$emit('keydown', $event)"
    />
  </label>
</template>
