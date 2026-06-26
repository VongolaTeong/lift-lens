<script setup lang="ts">
defineProps<{
  label: string
  value: string | number
  sub?: string
  tone?: 'good' | 'bad' | 'warn' | 'neutral'
}>()
</script>

<template>
  <div class="card stat">
    <span class="stat-label">{{ label }}</span>
    <span class="stat-value" :class="tone === 'good' ? 'text-good' : tone === 'bad' ? 'text-bad' : tone === 'warn' ? 'text-warn' : ''">
      {{ value }}
    </span>
    <span v-if="sub" class="stat-delta muted">{{ sub }}</span>
  </div>
</template>
