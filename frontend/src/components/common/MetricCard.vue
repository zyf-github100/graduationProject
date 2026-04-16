<template>
  <div
    class="metric-card"
    :class="`metric-card--${tone}`"
  >
    <div class="metric-card__head">
      <div class="metric-card__label">{{ title }}</div>
      <div class="metric-card__direction">
        {{ directionSymbol }}
      </div>
    </div>
    <div class="metric-card__value">{{ value }}</div>
    <div class="metric-card__trend">{{ trend }}</div>
    <div class="metric-card__chart">
      <svg
        viewBox="0 0 100 42"
        preserveAspectRatio="none"
        aria-hidden="true"
      >
        <defs>
          <linearGradient :id="gradientId" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" :stop-color="startColor" stop-opacity="0.42" />
            <stop offset="100%" :stop-color="endColor" stop-opacity="0.9" />
          </linearGradient>
          <linearGradient :id="areaId" x1="0%" y1="0%" x2="0%" y2="100%">
            <stop offset="0%" :stop-color="endColor" stop-opacity="0.3" />
            <stop offset="100%" :stop-color="endColor" stop-opacity="0.02" />
          </linearGradient>
        </defs>
        <path :d="areaPath" :fill="`url(#${areaId})`" />
        <path
          :d="linePath"
          fill="none"
          :stroke="`url(#${gradientId})`"
          stroke-width="2.1"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
      </svg>
    </div>
    <div class="metric-card__caption">{{ caption }}</div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  title: string
  value: string
  trend: string
  caption: string
  tone?: 'primary' | 'success' | 'warning' | 'danger'
  series?: number[]
  direction?: 'up' | 'down' | 'flat'
}

const props = withDefaults(defineProps<Props>(), {
  tone: 'primary',
  direction: 'flat',
  series: () => [30, 45, 41, 56, 49, 62, 57, 69],
})

const gradientId = computed(() => `${props.title}-line`.replace(/\s+/g, '-'))
const areaId = computed(() => `${props.title}-area`.replace(/\s+/g, '-'))

const palette = computed(() => {
  switch (props.tone) {
    case 'success':
      return ['#82e6b1', '#5fe49d']
    case 'warning':
      return ['#f0cf9d', '#d8af77']
    case 'danger':
      return ['#f3a28f', '#d86d59']
    default:
      return ['#84c6f0', '#6da6d0']
  }
})

const directionSymbol = computed(() => {
  if (props.direction === 'up') return '↗'
  if (props.direction === 'down') return '↘'
  return '→'
})

const normalizedPoints = computed(() => {
  const values = props.series
  const min = Math.min(...values)
  const max = Math.max(...values)
  const range = Math.max(max - min, 1)

  return values.map((value, index) => {
    const x = (index / Math.max(values.length - 1, 1)) * 100
    const y = 34 - ((value - min) / range) * 22
    return `${x},${y}`
  })
})

const linePath = computed(() => {
  return normalizedPoints.value.reduce((path, point, index) => {
    const [x, y] = point.split(',')
    return `${path}${index === 0 ? `M ${x} ${y}` : ` L ${x} ${y}`}`
  }, '')
})

const areaPath = computed(() => {
  const points = normalizedPoints.value
  const first = points[0]?.split(',') ?? ['0', '34']
  const last = points[points.length - 1]?.split(',') ?? ['100', '34']
  return `${linePath.value} L ${last[0]} 40 L ${first[0]} 40 Z`
})

const startColor = computed(() => palette.value[0])
const endColor = computed(() => palette.value[1])
</script>

<style scoped>
.metric-card {
  position: relative;
  min-height: 180px;
  padding: 18px 20px 16px;
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  background:
    linear-gradient(145deg, rgba(232, 240, 246, 0.2), rgba(232, 240, 246, 0.08) 55%, rgba(232, 240, 246, 0.12));
  box-shadow: var(--erp-shadow-float), inset 0 1px 0 rgba(255, 255, 255, 0.14);
  backdrop-filter: blur(24px);
  overflow: hidden;
}

.metric-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 50% -10%, rgba(255, 255, 255, 0.34), transparent 40%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.04), transparent 40%);
  pointer-events: none;
}

.metric-card__head,
.metric-card__value,
.metric-card__trend,
.metric-card__caption,
.metric-card__chart {
  position: relative;
  z-index: 1;
}

.metric-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.metric-card__label {
  font-size: 15px;
  color: var(--erp-color-text-secondary);
}

.metric-card__direction {
  font-size: 22px;
  line-height: 1;
}

.metric-card__value {
  margin-top: 14px;
  font-size: 46px;
  line-height: 1;
  letter-spacing: -0.05em;
  font-weight: 520;
}

.metric-card__trend {
  margin-top: 10px;
  font-size: 13px;
  font-weight: 500;
}

.metric-card__chart {
  height: 76px;
  margin-top: 8px;
}

.metric-card__chart svg {
  width: 100%;
  height: 100%;
}

.metric-card__caption {
  margin-top: 2px;
  color: var(--erp-color-text-tertiary);
  font-size: 12px;
}

.metric-card--primary .metric-card__direction,
.metric-card--primary .metric-card__trend {
  color: #9ed6fb;
}

.metric-card--success .metric-card__direction,
.metric-card--success .metric-card__trend {
  color: #89ebb7;
}

.metric-card--warning .metric-card__direction,
.metric-card--warning .metric-card__trend {
  color: #f0cf9d;
}

.metric-card--danger .metric-card__direction,
.metric-card--danger .metric-card__trend {
  color: #f3a28f;
}
</style>
