<template>
  <div class="page-header">
    <div class="page-header__main">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item
          v-for="item in breadcrumbs"
          :key="item"
        >
          {{ item }}
        </el-breadcrumb-item>
      </el-breadcrumb>
      <div>
        <h1>{{ title }}</h1>
        <p>{{ description }}</p>
      </div>
    </div>
    <div
      v-if="$slots.actions"
      class="page-header__actions"
    >
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  title: string
  description: string
  breadcrumbs: string[]
}

defineProps<Props>()
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--erp-space-4);
  flex-wrap: wrap;
}

.page-header__main {
  display: flex;
  flex-direction: column;
  gap: var(--erp-space-3);
}

:deep(.el-breadcrumb) {
  font-size: 12px;
}

.page-header h1 {
  margin: 0;
  font-size: var(--erp-font-size-page-title);
  line-height: var(--erp-line-height-tight);
  font-weight: 520;
  letter-spacing: -0.03em;
}

.page-header p {
  margin: 6px 0 0;
  color: var(--erp-color-text-secondary);
  font-size: var(--erp-font-size-body);
  max-width: 720px;
}

.page-header__actions {
  display: flex;
  align-items: center;
  gap: var(--erp-space-3);
  flex-wrap: wrap;
}
</style>
