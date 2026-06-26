<script setup lang="ts">
import VChart from 'vue-echarts'
import type { EChartsOption } from 'echarts'
import type { MuscleVolumeComparison } from '~/types/api'
import { AXIS_LABEL, AXIS_LINE, BASE_GRID, CHART_PALETTE, SPLIT_LINE, TOOLTIP } from '~/utils/chart'

const props = defineProps<{ comparisons: MuscleVolumeComparison[] }>()

// Sort by this-week volume, keep a sensible number of bars for readability.
const rows = computed(() =>
  [...props.comparisons].sort((a, b) => b.thisWeekVolume - a.thisWeekVolume).slice(0, 12).reverse(),
)

const option = computed<EChartsOption>(() => ({
  color: [CHART_PALETTE[0], '#3a4456'],
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, ...TOOLTIP },
  legend: { textStyle: { color: '#9aa6b9' }, top: 4, data: ['This week', 'Last week'] },
  grid: { ...BASE_GRID, left: 8 },
  xAxis: { type: 'value', axisLabel: AXIS_LABEL, splitLine: SPLIT_LINE },
  yAxis: {
    type: 'category',
    data: rows.value.map((r) => titleCase(r.muscle)),
    axisLine: AXIS_LINE,
    axisLabel: { color: '#9aa6b9', fontSize: 12 },
  },
  series: [
    {
      name: 'This week',
      type: 'bar',
      data: rows.value.map((r) => Math.round(r.thisWeekVolume)),
      barWidth: 9,
      itemStyle: { borderRadius: [0, 4, 4, 0] },
    },
    {
      name: 'Last week',
      type: 'bar',
      data: rows.value.map((r) => Math.round(r.lastWeekVolume)),
      barWidth: 9,
      itemStyle: { borderRadius: [0, 4, 4, 0] },
    },
  ],
}))
</script>

<template>
  <VChart class="chart" :option="option" autoresize />
</template>
