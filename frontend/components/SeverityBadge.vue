<script setup lang="ts">
import type { Severity } from '~/types/api'

const props = defineProps<{ severity: Severity }>()
</script>

<template>
  <span class="badge" :class="severityClass(props.severity)">{{ props.severity }}</span>
</template>
