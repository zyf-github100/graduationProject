<template>
  <div class="app-shell">
    <SidebarNav
      :menus="store.menus"
      :collapsed="store.collapsed"
      :caption="shellCaption"
    />
    <div class="app-shell__main">
      <GlobalHeader
        :current-user="shellUser"
        :search-placeholder="searchPlaceholder"
        @logout="handleLogout"
        @toggle-sidebar="store.toggleSidebar"
      />
      <main class="app-shell__content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import GlobalHeader from '../components/layout/GlobalHeader.vue'
import SidebarNav from '../components/layout/SidebarNav.vue'
import { useAppStore } from '../stores/app'

const route = useRoute()
const router = useRouter()
const store = useAppStore()

const shellUser = computed(() => {
  return (
    store.currentUser ?? {
      displayName: '访客',
      roles: ['未登录'],
      username: '',
    }
  )
})

const shellCaption = computed(() => {
  if (route.path.startsWith('/student')) {
    return '学生服务'
  }

  if (route.path.startsWith('/teacher')) {
    return '教师服务'
  }

  return '管理后台'
})

const searchPlaceholder = computed(() => {
  if (route.path.startsWith('/student')) {
    return '搜索课程 / 通知 / 账单'
  }

  if (route.path.startsWith('/teacher')) {
    return '搜索班级 / 学生 / 成绩任务'
  }

  return '搜索学生 / 审批 / 账单'
})

const handleLogout = async () => {
  await store.logout()
  router.push('/login')
}
</script>

<style scoped>
.app-shell {
  display: flex;
  min-height: 100%;
  padding: 14px;
  gap: 14px;
}

.app-shell__main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  border-radius: 28px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.04), rgba(255, 255, 255, 0.02));
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: var(--erp-shadow-soft), inset 0 1px 0 rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(12px);
  overflow: hidden;
}

.app-shell__content {
  flex: 1;
  min-width: 0;
  padding: var(--erp-page-padding);
  overflow-x: auto;
}

@media (max-width: 1024px) {
  .app-shell {
    padding: 10px;
  }
}
</style>
