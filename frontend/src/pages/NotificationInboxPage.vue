<template>
  <div class="page notification-inbox-page">
    <PageHeader
      title="消息收件箱"
      description="集中查看审批、提醒和系统事件，管理员可直接在这里跟踪最近一次业务流转。"
      :breadcrumbs="['管理后台', '消息收件箱']"
    >
      <template #actions>
        <span class="glass-chip">未读 {{ unreadCount }} 条</span>
      </template>
    </PageHeader>

    <div class="page-grid two-columns">
      <PageSection title="事件列表" description="默认按时间倒序展示，支持只看未读，并保留流程状态与来源服务。">
        <template #actions>
          <el-tabs v-model="activeTab" class="notice-tabs">
            <el-tab-pane label="全部" name="all" />
            <el-tab-pane label="未读" name="unread" />
          </el-tabs>
        </template>

        <el-empty v-if="!filteredRecords.length" description="当前没有可展示的事件" />

        <div v-else class="notice-list">
          <div
            v-for="item in filteredRecords"
            :key="item.id"
            class="notice-card"
            :class="{ 'notice-card--unread': !item.isRead }"
          >
            <div class="notice-card__head">
              <div class="notice-card__meta">
                <span class="notice-card__category">{{ item.bizType }}</span>
                <span class="notice-card__source">{{ item.sourceService }}</span>
                <span v-if="item.priority === 'high'" class="notice-card__priority">高优先级</span>
              </div>
              <span class="notice-card__time">{{ item.occurredAt }}</span>
            </div>
            <div class="notice-card__title">{{ item.title }}</div>
            <div class="notice-card__summary">{{ item.summary }}</div>
            <div class="notice-card__footer">
              <div class="notice-card__footer-meta">
                <StatusTag :status="item.workflowStatus" />
                <span>业务号 {{ item.bizId }}</span>
              </div>
              <el-button
                text
                type="primary"
                :disabled="item.isRead"
                @click="handleMarkRead(item)"
              >
                {{ item.isRead ? '已读' : '标记已读' }}
              </el-button>
            </div>
          </div>
        </div>
      </PageSection>

      <div class="section-stack">
        <PageSection title="来源分布" description="快速确认最近事件主要来自哪个服务，便于定位问题。">
          <div class="summary-list">
            <div v-for="item in sourceSummary" :key="item.label" class="summary-item">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </PageSection>

        <PageSection title="状态概览" description="关注审批中、已拒绝和已排队通知等关键状态。">
          <div class="summary-list">
            <div v-for="item in statusSummary" :key="item.label" class="summary-item">
              <span>{{ item.label }}</span>
              <div class="summary-item__status">
                <StatusTag :status="item.label" />
                <strong>{{ item.value }}</strong>
              </div>
            </div>
          </div>
        </PageSection>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  fetchWorkflowInbox,
  markWorkflowInboxRead,
  type WorkflowInboxItem,
} from '../api/erp'
import PageHeader from '../components/common/PageHeader.vue'
import PageSection from '../components/common/PageSection.vue'
import StatusTag from '../components/common/StatusTag.vue'
import { showRequestError } from '../lib/feedback'
import { useAppStore } from '../stores/app'

const store = useAppStore()
const activeTab = ref<'all' | 'unread'>('all')
const records = ref<WorkflowInboxItem[]>([])

const unreadCount = computed(() => records.value.filter((item) => !item.isRead).length)

const filteredRecords = computed(() => {
  if (activeTab.value === 'unread') {
    return records.value.filter((item) => !item.isRead)
  }
  return records.value
})

const buildSummary = (selector: (item: WorkflowInboxItem) => string) => {
  const bucket = new Map<string, number>()
  records.value.forEach((item) => {
    const key = selector(item) || 'UNKNOWN'
    bucket.set(key, (bucket.get(key) ?? 0) + 1)
  })

  return Array.from(bucket.entries()).map(([label, value]) => ({ label, value }))
}

const sourceSummary = computed(() => buildSummary((item) => item.sourceService))
const statusSummary = computed(() => buildSummary((item) => item.workflowStatus))

const loadData = async () => {
  try {
    records.value = await fetchWorkflowInbox()
  } catch (error) {
    showRequestError(error, '消息收件箱加载失败。')
  }
}

const handleMarkRead = async (item: WorkflowInboxItem) => {
  if (item.isRead) {
    return
  }

  try {
    const updated = await markWorkflowInboxRead(item.id)
    records.value = records.value.map((record) => (record.id === updated.id ? updated : record))
    ElMessage.success('已标记为已读')
  } catch (error) {
    showRequestError(error, '标记已读失败。')
  }
}

watch(
  unreadCount,
  (value) => {
    store.unreadCount = value
  },
  { immediate: true },
)

onMounted(() => {
  void loadData()
})
</script>

<style scoped>
.notification-inbox-page {
  gap: 20px;
}

:deep(.notice-tabs .el-tabs__header) {
  margin: 0;
}

.notice-list,
.summary-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notice-card,
.summary-item {
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.09), rgba(255, 255, 255, 0.04));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.notice-card--unread {
  border-color: rgba(173, 222, 252, 0.28);
  background: linear-gradient(145deg, rgba(160, 214, 248, 0.12), rgba(255, 255, 255, 0.04));
}

.notice-card__head,
.notice-card__footer,
.summary-item,
.notice-card__meta,
.notice-card__footer-meta,
.summary-item__status {
  display: flex;
  align-items: center;
}

.notice-card__head,
.notice-card__footer,
.summary-item {
  justify-content: space-between;
  gap: 12px;
}

.notice-card__meta,
.notice-card__footer-meta,
.summary-item__status {
  gap: 8px;
  flex-wrap: wrap;
}

.notice-card__category,
.notice-card__priority,
.notice-card__source {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
}

.notice-card__category,
.notice-card__source {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: var(--erp-color-text-secondary);
}

.notice-card__priority {
  background: rgba(226, 140, 124, 0.2);
  color: #ffd7ce;
}

.notice-card__time,
.notice-card__summary,
.notice-card__footer span,
.summary-item span {
  color: var(--erp-color-text-tertiary);
  font-size: 12px;
}

.notice-card__title,
.summary-item strong {
  font-size: 15px;
  font-weight: 600;
}

.notice-card__summary {
  margin-top: 10px;
  line-height: 1.7;
}

.notice-card__footer {
  margin-top: 14px;
}
</style>
