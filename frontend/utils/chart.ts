/** Shared ECharts styling so every chart matches the dark design system. */

export const CHART_PALETTE = [
  '#5b8cff',
  '#38d29a',
  '#f0b34a',
  '#f76d6d',
  '#a78bfa',
  '#4dd0e1',
  '#ff9e7d',
  '#9ccc65',
  '#ec87c0',
  '#7c93b8',
]

export const AXIS_LINE = { lineStyle: { color: '#2e3848' } }
export const SPLIT_LINE = { lineStyle: { color: '#1b212d' } }
export const AXIS_LABEL = { color: '#9aa6b9', fontSize: 11 }

export const TOOLTIP = {
  backgroundColor: '#1b212d',
  borderColor: '#2e3848',
  borderWidth: 1,
  textStyle: { color: '#e6eaf2', fontSize: 12 },
}

export const TEXT_STYLE = { color: '#e6eaf2', fontFamily: 'inherit' }

export const BASE_GRID = { left: 12, right: 14, top: 36, bottom: 8, containLabel: true }
