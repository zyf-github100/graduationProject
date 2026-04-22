import { createApp } from 'vue'
import {
  ElAlert,
  ElAvatar,
  ElBadge,
  ElBreadcrumb,
  ElBreadcrumbItem,
  ElButton,
  ElCol,
  ElDatePicker,
  ElDescriptions,
  ElDescriptionsItem,
  ElDialog,
  ElDivider,
  ElEmpty,
  ElForm,
  ElFormItem,
  ElIcon,
  ElInput,
  ElInputNumber,
  ElMenu,
  ElMenuItem,
  ElOption,
  ElPagination,
  ElRow,
  ElScrollbar,
  ElSelect,
  ElSkeleton,
  ElSkeletonItem,
  ElSubMenu,
  ElSwitch,
  ElTabPane,
  ElTable,
  ElTableColumn,
  ElTabs,
  ElTag,
  ElTimeline,
  ElTimelineItem,
} from 'element-plus'
import 'element-plus/dist/index.css'
import './styles/tokens.css'
import './styles/base.css'
import App from './App.vue'
import router from './router'
import { pinia } from './stores/pinia'

const app = createApp(App)

app.use(pinia)
app.use(router)

const elementPlusComponents = [
  ElAlert,
  ElAvatar,
  ElBadge,
  ElBreadcrumb,
  ElBreadcrumbItem,
  ElButton,
  ElCol,
  ElDatePicker,
  ElDescriptions,
  ElDescriptionsItem,
  ElDialog,
  ElDivider,
  ElEmpty,
  ElForm,
  ElFormItem,
  ElIcon,
  ElInput,
  ElInputNumber,
  ElMenu,
  ElMenuItem,
  ElOption,
  ElPagination,
  ElRow,
  ElScrollbar,
  ElSelect,
  ElSkeleton,
  ElSkeletonItem,
  ElSubMenu,
  ElSwitch,
  ElTabPane,
  ElTable,
  ElTableColumn,
  ElTabs,
  ElTag,
  ElTimeline,
  ElTimelineItem,
]

elementPlusComponents.forEach((component) => {
  app.use(component)
})

app.mount('#app')
