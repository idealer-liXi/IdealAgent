<script setup>
import UiButton from '../ui/UiButton.vue'
import UiInput from '../ui/UiInput.vue'
import UiLoading from '../ui/UiLoading.vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  error: { type: String, default: '' },
})

const emit = defineEmits(['update:modelValue', 'send'])

function handleSend() {
  if (!props.loading) {
    emit('send')
  }
}
</script>

<template>
  <div class="border-t border-border-subtle bg-elevated p-5">
    <div v-if="error" class="mb-3 rounded-card-md bg-error-bg px-4 py-3 text-sm text-error">
      {{ error }}
    </div>
    <form class="flex flex-col gap-3 md:flex-row" @submit.prevent="handleSend">
      <UiInput
        type="textarea"
        :model-value="modelValue"
        placeholder="输入消息，Enter+点击发送"
        :rows="3"
        class="flex-1"
        @update:model-value="$emit('update:modelValue', $event)"
      />
      <UiButton
        variant="primary"
        size="lg"
        :loading="loading"
        :disabled="!modelValue.trim()"
        @click="handleSend"
      >
        <template v-if="loading">
          <UiLoading size="sm" />
          <span>发送中</span>
        </template>
        <template v-else>发送</template>
      </UiButton>
    </form>
  </div>
</template>
