<script setup lang="ts">
import type { Insight } from '~/types/api'

const props = withDefaults(
  defineProps<{ insight: Insight; dismissible?: boolean }>(),
  { dismissible: false },
)

const emit = defineEmits<{ dismiss: [id: number] }>()

const meta = computed(() => insightMeta(props.insight.type))
const accent = computed(() => severityColor(props.insight.severity))

const deepLink = computed(() =>
  props.insight.exerciseId ? `/exercises/${props.insight.exerciseId}` : '/muscles',
)

const subject = computed(
  () => props.insight.exerciseName ?? titleCase(props.insight.muscle) ?? null,
)
</script>

<template>
  <article class="insight" :style="{ '--accent-bar': accent }">
    <div class="insight-head">
      <span class="glyph">{{ meta.glyph }}</span>
      <span class="type">{{ meta.label }}</span>
      <SeverityBadge :severity="insight.severity" />
      <span class="spacer" />
      <button
        v-if="dismissible"
        class="btn-ghost btn-sm"
        title="Dismiss"
        @click="emit('dismiss', insight.id)"
      >
        ✕
      </button>
    </div>

    <h3 class="insight-title">{{ insight.title }}</h3>
    <p v-if="insight.detail" class="insight-detail muted">{{ insight.detail }}</p>

    <div class="insight-foot">
      <NuxtLink v-if="subject" :to="deepLink" class="subject">
        {{ subject }} →
      </NuxtLink>
      <span class="spacer" />
      <span v-if="insight.windowEnd" class="dim">to {{ fmtDate(insight.windowEnd) }}</span>
    </div>
  </article>
</template>

<style scoped>
.insight {
  position: relative;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 16px 16px 14px 18px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: 8px;
  height: 100%;
}

.insight::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--accent-bar);
}

.insight-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.glyph {
  font-size: 16px;
}

.type {
  font-weight: 650;
  font-size: 14px;
}

.insight-title {
  font-size: 15px;
  font-weight: 600;
  line-height: 1.35;
}

.insight-detail {
  font-size: 13.5px;
  margin: 0;
}

.insight-foot {
  display: flex;
  align-items: center;
  margin-top: auto;
  padding-top: 6px;
  font-size: 13px;
}

.subject {
  color: var(--accent);
  font-weight: 600;
}

.subject:hover {
  text-decoration: underline;
}
</style>
