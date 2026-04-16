<template>
  <div class="detail-section surface">
    <div class="detail-section__title">
      <h3>{{ title }}</h3>
      <p v-if="description">{{ description }}</p>
    </div>
    <div class="detail-section__body">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  title: string
  description?: string
}

defineProps<Props>()
</script>

<style scoped>
.detail-section {
  padding: 20px 22px;
}

.detail-section__title {
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  position: relative;
  z-index: 1;
}

.detail-section__body {
  padding-top: 18px;
  position: relative;
  z-index: 1;
}

.detail-section h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
}

.detail-section p {
  margin: 6px 0 0;
  font-size: var(--erp-font-size-body-sm);
  color: var(--erp-color-text-secondary);
}
</style>
