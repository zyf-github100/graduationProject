<template>
  <div class="not-found">
    <div class="surface not-found__card">
      <el-empty description="未找到对应页面，请从左侧导航重新进入业务模块。">
        <el-button type="primary" @click="router.push(store.homePath)">返回首页</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAppStore } from '../stores/app'

const router = useRouter()
const store = useAppStore()
</script>

<style scoped>
.not-found {
  min-height: 100%;
  display: grid;
  place-items: center;
}

.not-found__card {
  width: min(520px, 100%);
  padding: 24px;
}
</style>
