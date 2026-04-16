<template>
  <section class="page-section surface">
    <div class="page-section__header">
      <div>
        <h2>{{ title }}</h2>
        <p v-if="description">{{ description }}</p>
      </div>
      <div
        v-if="$slots.actions"
        class="page-section__actions"
      >
        <slot name="actions" />
      </div>
    </div>
    <div class="page-section__body">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
interface Props {
  title: string
  description?: string
}

defineProps<Props>()
</script>

<style scoped>
.page-section {
  padding: 22px;
}

.page-section__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--erp-space-4);
  padding-bottom: 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  position: relative;
  z-index: 1;
}

.page-section__body {
  padding-top: 20px;
  position: relative;
  z-index: 1;
}

.page-section h2 {
  margin: 0;
  font-size: var(--erp-font-size-section-title);
  line-height: var(--erp-line-height-tight);
  font-weight: 500;
  letter-spacing: -0.02em;
}

.page-section p {
  margin: 6px 0 0;
  color: var(--erp-color-text-secondary);
}

.page-section__actions {
  display: flex;
  gap: var(--erp-space-3);
  flex-wrap: wrap;
}
</style>
