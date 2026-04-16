<template>
  <span
    class="status-tag"
    :class="`status-tag--${meta.type}`"
  >
    {{ meta.label }}
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { statusMeta } from '../../constants/status'

interface Props {
  status: string
}

const props = defineProps<Props>()

const meta = computed(() => {
  return statusMeta[props.status] ?? { label: props.status, type: 'info' as const }
})
</script>

<style scoped>
.status-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.01em;
  backdrop-filter: blur(12px);
}

.status-tag--primary,
.status-tag--info {
  background: rgba(137, 169, 194, 0.16);
  color: #dbe9f5;
}

.status-tag--success {
  background: rgba(113, 215, 164, 0.18);
  color: #cbf4df;
}

.status-tag--warning {
  background: rgba(223, 183, 133, 0.2);
  color: #f7ddbd;
}

.status-tag--danger {
  background: rgba(226, 140, 124, 0.2);
  color: #ffd7ce;
}
</style>
