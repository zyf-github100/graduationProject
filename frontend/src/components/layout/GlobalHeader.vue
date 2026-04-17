<template>
  <header class="global-header">
    <div class="global-header__left">
      <el-button text @click="$emit('toggle-sidebar')">
        <el-icon><Fold /></el-icon>
      </el-button>
      <el-input
        v-model="store.searchKeyword"
        class="global-header__search"
        :placeholder="searchPlaceholder"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>
    <div class="global-header__right">
      <el-select v-model="campusModel" size="small" style="width: 132px">
        <el-option label="主校区" value="主校区" />
        <el-option label="实训校区" value="实训校区" />
      </el-select>
      <el-select v-model="termModel" size="small" style="width: 220px">
        <el-option label="2025-2026 学年第二学期" value="2025-2026 学年第二学期" />
        <el-option label="2025-2026 学年第一学期" value="2025-2026 学年第一学期" />
      </el-select>
      <el-badge :value="store.unreadCount" :hidden="store.unreadCount === 0">
        <el-button text @click="goToNotifications">
          <el-icon><Bell /></el-icon>
        </el-button>
      </el-badge>
      <div class="global-header__user">
        <el-avatar size="small">{{ userInitial }}</el-avatar>
        <div>
          <div class="global-header__user-name">{{ currentUser.displayName }}</div>
          <div class="global-header__user-role">{{ currentUser.roles[0] }}</div>
        </div>
      </div>
      <el-button text @click="$emit('logout')">退出</el-button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { Bell, Fold, Search } from '@element-plus/icons-vue'
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '../../stores/app'

interface Props {
  currentUser: {
    displayName: string
    roles: string[]
  }
  searchPlaceholder?: string
}

withDefaults(defineProps<Props>(), {
  searchPlaceholder: '搜索课程 / 通知 / 账单',
})

defineEmits<{
  (e: 'logout'): void
  (e: 'toggle-sidebar'): void
}>()

const store = useAppStore()
const router = useRouter()

const campusModel = computed({
  get: () => store.campus,
  set: (value: string) => store.setCampus(value),
})

const termModel = computed({
  get: () => store.term,
  set: (value: string) => store.setTerm(value),
})

const userInitial = computed(() => store.currentUser?.displayName.slice(0, 1) || 'A')

const goToNotifications = () => {
  const destination = (() => {
    switch (store.currentUser?.userType) {
      case 'STUDENT':
        return '/student/notices'
      case 'TEACHER':
        return '/teacher/notices'
      default:
        return '/notifications/inbox'
    }
  })()

  if (router.currentRoute.value.path !== destination) {
    void router.push(destination)
  }
}
</script>

<style scoped>
.global-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--erp-space-4);
  min-height: var(--erp-header-height);
  padding: 16px var(--erp-page-padding) 10px;
}

.global-header__left,
.global-header__right {
  display: flex;
  align-items: center;
  gap: var(--erp-space-3);
}

.global-header__search {
  width: 320px;
}

:deep(.global-header__search .el-input__wrapper) {
  min-height: 40px;
  border-radius: 14px;
}

:deep(.global-header__right .el-select__wrapper) {
  min-height: 40px;
  border-radius: 14px;
}

:deep(.global-header__right .el-badge__content) {
  border: none;
  box-shadow: none;
}

.global-header__user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px 8px 8px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 18px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.global-header__user-name {
  font-size: 13px;
  font-weight: 600;
}

.global-header__user-role {
  font-size: 12px;
  color: var(--erp-color-text-tertiary);
}

@media (max-width: 1024px) {
  .global-header {
    flex-wrap: wrap;
    padding: 14px var(--erp-page-padding) 10px;
  }

  .global-header__search {
    width: 240px;
  }
}
</style>
