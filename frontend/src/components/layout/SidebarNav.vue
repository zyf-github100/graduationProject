<template>
  <aside
    class="sidebar"
    :class="{ 'sidebar--collapsed': collapsed }"
  >
    <div class="sidebar__brand">
      <div class="sidebar__mark">ERP</div>
      <div v-if="!collapsed">
        <div class="sidebar__name">广州软件学院</div>
        <div class="sidebar__caption">{{ caption || '智慧校园平台' }}</div>
      </div>
    </div>

    <el-scrollbar class="sidebar__menu-wrap">
      <el-menu
        :default-active="activePath"
        class="sidebar__menu"
        router
        unique-opened
        :collapse="collapsed"
      >
        <template
          v-for="item in menus"
          :key="item.menuId"
        >
          <el-sub-menu
            v-if="item.children?.length"
            :index="item.path"
          >
            <template #title>
              <el-icon><component :is="resolveIcon(item.icon)" /></el-icon>
              <span>{{ item.menuName }}</span>
            </template>
            <el-menu-item
              v-for="child in item.children"
              :key="child.menuId"
              :index="child.path"
            >
              {{ child.menuName }}
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item
            v-else
            :index="item.path"
          >
            <el-icon><component :is="resolveIcon(item.icon)" /></el-icon>
            <span>{{ item.menuName }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-scrollbar>
  </aside>
</template>

<script setup lang="ts">
import { Bell, Calendar, DocumentChecked, House, Money, Setting, UserFilled } from '@element-plus/icons-vue'
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import type { Component } from 'vue'
import type { MenuItem } from '../../types'

interface Props {
  menus: MenuItem[]
  collapsed: boolean
  caption?: string
}

const props = defineProps<Props>()
const route = useRoute()

const iconMap: Record<string, Component> = {
  Bell,
  Calendar,
  DocumentChecked,
  House,
  Money,
  Setting,
  UserFilled,
}

const activePath = computed(() => {
  if (route.path.startsWith('/students/')) {
    return '/students'
  }

  return route.path
})

const resolveIcon = (icon?: string | null) => {
  return (icon && iconMap[icon]) || House
}

const menus = computed(() => props.menus)
const caption = computed(() => props.caption)
</script>

<style scoped>
.sidebar {
  display: flex;
  flex-direction: column;
  width: var(--erp-sidebar-width);
  min-width: var(--erp-sidebar-width);
  background:
    linear-gradient(180deg, rgba(13, 29, 44, 0.84), rgba(11, 27, 42, 0.76) 56%, rgba(20, 35, 50, 0.64)),
    radial-gradient(circle at 10% 80%, rgba(220, 133, 95, 0.18), transparent 34%);
  color: #dce7f5;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 28px;
  box-shadow: var(--erp-shadow-float), inset 0 1px 0 rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(18px);
  transition: width 0.2s ease, min-width 0.2s ease;
  overflow: hidden;
}

.sidebar--collapsed {
  width: var(--erp-sidebar-collapsed-width);
  min-width: var(--erp-sidebar-collapsed-width);
}

.sidebar__brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 22px 18px 18px;
}

.sidebar__mark {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: linear-gradient(180deg, rgba(140, 193, 227, 0.54), rgba(77, 132, 171, 0.32));
  color: #ffffff;
  font-size: 12px;
  font-weight: 700;
  border: 1px solid rgba(255, 255, 255, 0.14);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.16);
}

.sidebar__name {
  font-size: 18px;
  font-weight: 520;
  letter-spacing: -0.02em;
}

.sidebar__caption {
  margin-top: 4px;
  font-size: 12px;
  color: rgba(220, 231, 245, 0.72);
}

.sidebar__menu-wrap {
  flex: 1;
  padding: 12px 12px 18px;
}

:deep(.sidebar__menu) {
  border-right: none;
  background: transparent;
}

:deep(.sidebar__menu .el-menu-item),
:deep(.sidebar__menu .el-sub-menu__title) {
  height: 48px;
  margin-bottom: 7px;
  border-radius: 16px;
  color: rgba(228, 236, 244, 0.86);
  font-size: 15px;
}

:deep(.sidebar__menu .el-menu-item.is-active) {
  background: linear-gradient(145deg, rgba(208, 224, 237, 0.18), rgba(208, 224, 237, 0.08));
  color: #ffffff;
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.1);
}

:deep(.sidebar__menu .el-sub-menu .el-menu-item) {
  min-width: auto;
  padding-left: 52px;
}
</style>
