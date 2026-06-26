import type { InsightType, Severity } from '~/types/api'

/** Compact number, e.g. 12,540 → "12,540" and 1234.5 → "1,234.5". */
export function fmtNumber(value: number | null | undefined, digits = 0): string {
  if (value === null || value === undefined || Number.isNaN(value)) return '—'
  return value.toLocaleString(undefined, { maximumFractionDigits: digits })
}

/** Kilograms with a unit suffix. */
export function fmtKg(value: number | null | undefined, digits = 1): string {
  if (value === null || value === undefined) return '—'
  return `${fmtNumber(value, digits)} kg`
}

/** Signed value for deltas/slopes, e.g. +2.4 / −1.1. */
export function fmtSigned(value: number | null | undefined, digits = 1): string {
  if (value === null || value === undefined || Number.isNaN(value)) return '—'
  const rounded = Number(value.toFixed(digits))
  const sign = rounded > 0 ? '+' : rounded < 0 ? '−' : ''
  return `${sign}${Math.abs(rounded).toLocaleString(undefined, { maximumFractionDigits: digits })}`
}

/** Percent change between two volumes; null when there's no baseline. */
export function pctChange(now: number, prev: number): number | null {
  if (!prev) return now > 0 ? 100 : null
  return ((now - prev) / prev) * 100
}

/** "2026-06-22" → "22 Jun 2026". */
export function fmtDate(iso: string | null | undefined): string {
  if (!iso) return '—'
  const d = new Date(iso.length <= 10 ? `${iso}T00:00:00` : iso)
  if (Number.isNaN(d.getTime())) return iso
  return d.toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })
}

/** "22 Jun" (no year), for dense chart axes/labels. */
export function fmtDateShort(iso: string | null | undefined): string {
  if (!iso) return ''
  const d = new Date(iso.length <= 10 ? `${iso}T00:00:00` : iso)
  if (Number.isNaN(d.getTime())) return iso
  return d.toLocaleDateString('en-GB', { day: 'numeric', month: 'short' })
}

/** Whole days since an ISO date, or null. */
export function daysAgo(iso: string | null | undefined): number | null {
  if (!iso) return null
  const d = new Date(iso.length <= 10 ? `${iso}T00:00:00` : iso)
  if (Number.isNaN(d.getTime())) return null
  return Math.floor((Date.now() - d.getTime()) / 86_400_000)
}

export function severityClass(severity: Severity): string {
  return { HIGH: 'badge-bad', WARN: 'badge-warn', INFO: 'badge-info' }[severity] ?? 'badge-neutral'
}

export function severityColor(severity: Severity): string {
  return { HIGH: '#f76d6d', WARN: '#f0b34a', INFO: '#5b8cff' }[severity] ?? '#9aa6b9'
}

/** Display label + emoji glyph per insight type, for cards and the feed. */
export function insightMeta(type: InsightType): { label: string; glyph: string } {
  const map: Record<InsightType, { label: string; glyph: string }> = {
    PROGRESS: { label: 'Progress', glyph: '📈' },
    REGRESSION: { label: 'Regression', glyph: '📉' },
    PLATEAU: { label: 'Plateau', glyph: '➖' },
    IMBALANCE: { label: 'Imbalance', glyph: '⚖️' },
    NEGLECT: { label: 'Neglect', glyph: '🥶' },
    DROPOFF: { label: 'Drop-off', glyph: '🚪' },
    PR: { label: 'PR', glyph: '🏆' },
  }
  return map[type] ?? { label: type, glyph: '•' }
}

/** Title-case a SCREAMING_SNAKE muscle/equipment token: "LATS" → "Lats". */
export function titleCase(value: string | null | undefined): string {
  if (!value) return '—'
  return value
    .toLowerCase()
    .split(/[_\s]+/)
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(' ')
}
