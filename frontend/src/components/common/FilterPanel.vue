<template>
  <div class="filter-panel surface">
    <div class="filter-panel__header">
      <div>
        <h3>{{ title }}</h3>
        <p>{{ description }}</p>
      </div>
      <div class="filter-panel__actions">
        <slot name="actions" />
      </div>
    </div>
    <div class="filter-panel__body">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  title?: string
  description?: string
}

withDefaults(defineProps<Props>(), {
  title: '筛选条件',
  description: '使用学期、院系、状态和时间范围快速定位业务数据。',
})
</script>

<style scoped>
.filter-panel {
  padding: 20px 22px;
}

.filter-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--erp-space-4);
  position: relative;
  z-index: 1;
}

.filter-panel__body {
  margin-top: 18px;
  position: relative;
  z-index: 1;
}

.filter-panel h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
  letter-spacing: -0.02em;
}

.filter-panel p {
  margin: 6px 0 0;
  color: var(--erp-color-text-secondary);
  font-size: var(--erp-font-size-body-sm);
}

.filter-panel__actions {
  display: flex;
  gap: var(--erp-space-3);
  flex-wrap: wrap;
}
</style>
