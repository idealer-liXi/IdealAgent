<script setup>
import UiCard from '../ui/UiCard.vue'

const props = defineProps({
  profile: { type: Object, default: null },
})

const stats = [
  { label: '项目阶段', value: 'Stage 4', icon: '🚀', color: 'bg-accent/10 text-accent' },
  { label: '后端状态', value: '已连接', icon: '✅', color: 'bg-emerald-50 text-emerald-600' },
  { label: '用户角色', value: props.profile?.userRole || 'user', icon: '👤', color: 'bg-amber-50 text-amber-600' },
  { label: '当前用户', value: props.profile?.userName || 'IdealAgent', icon: '🏷️', color: 'bg-sky-50 text-sky-600' },
]
</script>

<template>
  <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
    <UiCard v-for="stat in stats" :key="stat.label" :hoverable="false" padding="md">
      <div class="flex items-center gap-3">
        <div :class="['flex h-10 w-10 shrink-0 items-center justify-center rounded-card-md text-lg', stat.color]">
          {{ stat.icon }}
        </div>
        <div class="min-w-0">
          <div class="text-xs text-text-secondary">{{ stat.label }}</div>
          <div class="truncate text-base font-bold text-text-primary">{{ stat.value }}</div>
        </div>
      </div>
    </UiCard>
  </div>
</template>
