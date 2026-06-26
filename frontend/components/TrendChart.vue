<script setup lang="ts">
import VChart from 'vue-echarts'
import type { EChartsOption } from 'echarts'
import type { ExerciseTrend } from '~/types/api'
import { AXIS_LABEL, AXIS_LINE, BASE_GRID, CHART_PALETTE, SPLIT_LINE, TOOLTIP } from '~/utils/chart'

const props = defineProps<{ trend: ExerciseTrend }>()

const labels = computed(() => props.trend.points.map((p) => fmtDateShort(p.weekStart)))

// Index of the peak week (best e1RM for weighted, best reps for bodyweight) → PR marker.
const peakIndex = computed(() => {
  const pts = props.trend.points
  let best = -Infinity
  let idx = -1
  pts.forEach((p, i) => {
    const v = props.trend.weighted ? p.bestE1rm : p.bestReps
    if (v != null && v > best) {
      best = v
      idx = i
    }
  })
  return idx
})

const option = computed<EChartsOption>(() => {
  const pts = props.trend.points
  const peak = peakIndex.value

  const markPoint =
    peak >= 0
      ? {
          symbolSize: 46,
          itemStyle: { color: 'rgba(56, 210, 154, 0.18)' },
          label: { color: '#38d29a', fontSize: 11, formatter: 'PR' },
          data: [{ name: 'PR', xAxis: peak, yAxis: 0 }],
        }
      : undefined

  if (props.trend.weighted) {
    return {
      color: [CHART_PALETTE[0], CHART_PALETTE[1]],
      tooltip: { trigger: 'axis', ...TOOLTIP },
      legend: { textStyle: { color: '#9aa6b9' }, top: 4, data: ['Top e1RM (kg)', 'Volume (kg)'] },
      grid: BASE_GRID,
      xAxis: {
        type: 'category',
        data: labels.value,
        axisLine: AXIS_LINE,
        axisLabel: AXIS_LABEL,
      },
      yAxis: [
        {
          type: 'value',
          name: 'e1RM',
          nameTextStyle: { color: '#6b7689' },
          axisLabel: AXIS_LABEL,
          splitLine: SPLIT_LINE,
          scale: true,
        },
        {
          type: 'value',
          name: 'Volume',
          nameTextStyle: { color: '#6b7689' },
          axisLabel: AXIS_LABEL,
          splitLine: { show: false },
        },
      ],
      series: [
        {
          name: 'Top e1RM (kg)',
          type: 'line',
          smooth: true,
          symbolSize: 6,
          connectNulls: true,
          data: pts.map((p) => p.bestE1rm),
          lineStyle: { width: 2.5 },
          markPoint,
        },
        {
          name: 'Volume (kg)',
          type: 'line',
          yAxisIndex: 1,
          smooth: true,
          symbolSize: 4,
          connectNulls: true,
          areaStyle: { opacity: 0.08 },
          data: pts.map((p) => p.volume),
          lineStyle: { width: 1.5, type: 'dashed' },
        },
      ],
    }
  }

  // Bodyweight: trend on reps (CLAUDE.md §5/§8 — label it "reps progression").
  return {
    color: [CHART_PALETTE[1], CHART_PALETTE[4]],
    tooltip: { trigger: 'axis', ...TOOLTIP },
    legend: { textStyle: { color: '#9aa6b9' }, top: 4, data: ['Best set reps', 'Total reps'] },
    grid: BASE_GRID,
    xAxis: {
      type: 'category',
      data: labels.value,
      axisLine: AXIS_LINE,
      axisLabel: AXIS_LABEL,
    },
    yAxis: [
      {
        type: 'value',
        name: 'reps',
        nameTextStyle: { color: '#6b7689' },
        axisLabel: AXIS_LABEL,
        splitLine: SPLIT_LINE,
        scale: true,
      },
      {
        type: 'value',
        name: 'total',
        nameTextStyle: { color: '#6b7689' },
        axisLabel: AXIS_LABEL,
        splitLine: { show: false },
      },
    ],
    series: [
      {
        name: 'Best set reps',
        type: 'line',
        smooth: true,
        symbolSize: 6,
        connectNulls: true,
        data: pts.map((p) => p.bestReps),
        lineStyle: { width: 2.5 },
        markPoint,
      },
      {
        name: 'Total reps',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbolSize: 4,
        connectNulls: true,
        areaStyle: { opacity: 0.08 },
        data: pts.map((p) => p.totalReps),
        lineStyle: { width: 1.5, type: 'dashed' },
      },
    ],
  }
})
</script>

<template>
  <VChart class="chart" :option="option" autoresize />
</template>
