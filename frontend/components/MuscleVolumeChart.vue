<script setup lang="ts">
import VChart from 'vue-echarts'
import type { EChartsOption } from 'echarts'
import type { MuscleVolumePoint } from '~/types/api'
import { AXIS_LABEL, AXIS_LINE, BASE_GRID, CHART_PALETTE, SPLIT_LINE, TOOLTIP } from '~/utils/chart'

const props = defineProps<{ points: MuscleVolumePoint[]; metric?: 'volume' | 'sets' }>()

const weeks = computed(() =>
  [...new Set(props.points.map((p) => p.weekStart))].sort(),
)

const muscles = computed(() =>
  [...new Set(props.points.map((p) => p.muscle))].sort(),
)

const lookup = computed(() => {
  const map = new Map<string, MuscleVolumePoint>()
  for (const p of props.points) map.set(`${p.muscle}|${p.weekStart}`, p)
  return map
})

const option = computed<EChartsOption>(() => ({
  color: CHART_PALETTE,
  tooltip: { trigger: 'axis', ...TOOLTIP },
  legend: {
    type: 'scroll',
    textStyle: { color: '#9aa6b9' },
    top: 2,
    data: muscles.value.map((m) => titleCase(m)),
  },
  grid: { ...BASE_GRID, top: 48 },
  xAxis: {
    type: 'category',
    data: weeks.value.map((w) => fmtDateShort(w)),
    axisLine: AXIS_LINE,
    axisLabel: AXIS_LABEL,
  },
  yAxis: {
    type: 'value',
    axisLabel: AXIS_LABEL,
    splitLine: SPLIT_LINE,
    name: props.metric === 'sets' ? 'sets' : 'volume (kg)',
    nameTextStyle: { color: '#6b7689' },
  },
  series: muscles.value.map((m) => ({
    name: titleCase(m),
    type: 'line',
    smooth: true,
    symbolSize: 4,
    connectNulls: true,
    data: weeks.value.map((w) => {
      const pt = lookup.value.get(`${m}|${w}`)
      if (!pt) return null
      return props.metric === 'sets' ? pt.setCount : Math.round(pt.workingVolume)
    }),
  })),
}))
</script>

<template>
  <VChart class="chart" :option="option" autoresize />
</template>
