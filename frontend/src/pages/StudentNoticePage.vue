<template>
  <div class="page student-notice-page">
    <PageHeader
      title="通知公告"
      description="把考试通知、缴费提醒和校园公告统一收口在一个信息页面，避免学生在多个入口之间查找消息。"
      :breadcrumbs="['学生服务', '通知公告']"
    >
      <template #actions>
        <span class="glass-chip">未读 {{ unreadCount }} 条</span>
      </template>
    </PageHeader>

    <div class="page-grid two-columns">
      <PageSection title="消息列表" description="默认按发布时间倒序展示，并支持只看未读消息。">
        <template #actions>
          <el-tabs v-model="activeTab" class="notice-tabs">
            <el-tab-pane label="全部" name="all" />
            <el-tab-pane label="未读" name="unread" />
          </el-tabs>
        </template>

        <div class="notice-list">
          <div
            v-for="notice in filteredNotices"
            :key="notice.id"
            class="notice-card"
            :class="{ 'notice-card--unread': !notice.isRead }"
          >
            <div class="notice-card__head">
              <div class="notice-card__meta">
                <span class="notice-card__category">{{ notice.category }}</span>
                <span v-if="notice.priority === 'high'" class="notice-card__priority">重要</span>
              </div>
              <span class="notice-card__time">{{ notice.publishTime }}</span>
            </div>
            <div class="notice-card__title">{{ notice.title }}</div>
            <div class="notice-card__summary">{{ notice.summary }}</div>
            <div class="notice-card__footer">
              <span>{{ notice.publisher }}</span>
              <div class="notice-card__actions">
                <el-button text type="primary" @click="openDetail(notice)">查看详情</el-button>
                <el-button
                  v-if="!notice.isRead"
                  text
                  :loading="markingNoticeId === notice.id"
                  @click="markAsRead(notice)"
                >
                  标记已读
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </PageSection>

      <div class="section-stack">
        <PageSection title="今日动态" description="保留近期系统推送与个人操作轨迹，帮助学生确认事项是否处理完成。">
          <div class="activity-list">
            <div
              v-for="item in timeline"
              :key="`${item.time}-${item.content}`"
              class="activity-item"
            >
              <div class="activity-item__time">{{ item.time }}</div>
              <div class="activity-item__body">
                <div class="activity-item__title">{{ item.content }}</div>
                <div class="activity-item__actor">{{ item.actor }}</div>
              </div>
            </div>
          </div>
        </PageSection>

        <PageSection title="常看分类" description="用简洁统计帮助学生理解消息来源，不额外引入复杂图表。">
          <div class="category-list">
            <div v-for="item in categorySummary" :key="item.category" class="category-item">
              <span>{{ item.category }}</span>
              <strong>{{ item.label }}</strong>
            </div>
          </div>
        </PageSection>
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="通知详情" width="640px">
      <template v-if="activeNotice">
        <div class="notice-detail">
          <div class="notice-detail__head">
            <div>
              <div class="notice-detail__meta">
                <span class="notice-card__category">{{ activeNotice.category }}</span>
                <span v-if="activeNotice.priority === 'high'" class="notice-card__priority">重要</span>
              </div>
              <h2>{{ activeNotice.title }}</h2>
            </div>
            <el-tag :type="activeNotice.isRead ? 'success' : 'warning'" round>
              {{ activeNotice.isRead ? '已读' : '未读' }}
            </el-tag>
          </div>

          <el-descriptions :column="2" border>
            <el-descriptions-item label="发布单位">{{ activeNotice.publisher }}</el-descriptions-item>
            <el-descriptions-item label="发布时间">{{ activeNotice.publishTime }}</el-descriptions-item>
          </el-descriptions>

          <div class="notice-detail__body">{{ activeNotice.summary }}</div>
        </div>
      </template>

      <template #footer>
        <div class="toolbar-inline">
          <el-button @click="detailVisible = false">关闭</el-button>
          <el-button
            v-if="activeNotice && !activeNotice.isRead"
            type="primary"
            :loading="markingNoticeId === activeNotice.id"
            @click="markAsRead(activeNotice)"
          >
            标记已读
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  fetchStudentNotices,
  markStudentNoticeRead,
  type ActivityItem,
  type NoticeCategorySummary,
} from '../api/erp'
import PageHeader from '../components/common/PageHeader.vue'
import PageSection from '../components/common/PageSection.vue'
import { showRequestError } from '../lib/feedback'
import { useAppStore } from '../stores/app'
import type { StudentNotice } from '../types'

const store = useAppStore()
const activeTab = ref('all')
const records = ref<StudentNotice[]>([])
const timeline = ref<ActivityItem[]>([])
const categorySummary = ref<NoticeCategorySummary[]>([])
const unreadCount = ref(0)
const activeNotice = ref<StudentNotice | null>(null)
const detailVisible = ref(false)
const markingNoticeId = ref<number | null>(null)

const filteredNotices = computed(() => {
  if (activeTab.value === 'unread') {
    return records.value.filter((item) => !item.isRead)
  }

  return records.value
})

const loadData = async () => {
  try {
    const data = await fetchStudentNotices()
    records.value = data.records
    timeline.value = data.timeline
    categorySummary.value = data.categorySummary
    unreadCount.value = data.unreadCount
  } catch (error) {
    showRequestError(error, '学生通知加载失败。')
  }
}

const openDetail = (notice: StudentNotice) => {
  activeNotice.value = notice
  detailVisible.value = true
}

const markAsRead = async (notice: StudentNotice) => {
  if (notice.isRead) {
    return
  }

  markingNoticeId.value = notice.id
  try {
    const updated = await markStudentNoticeRead(notice.id)
    records.value = records.value.map((item) => (item.id === updated.id ? updated : item))
    if (activeNotice.value?.id === updated.id) {
      activeNotice.value = updated
    }
    refreshNoticeSummary()
    await store.refreshPortalIndicators('/student/notices')
    ElMessage.success('通知已标记为已读。')
  } catch (error) {
    showRequestError(error, '标记已读失败。')
  } finally {
    markingNoticeId.value = null
  }
}

const refreshNoticeSummary = () => {
  unreadCount.value = records.value.filter((item) => !item.isRead).length
  const summary = new Map<string, { total: number; unread: number }>()

  records.value.forEach((notice) => {
    const item = summary.get(notice.category) ?? { total: 0, unread: 0 }
    item.total += 1
    if (!notice.isRead) {
      item.unread += 1
    }
    summary.set(notice.category, item)
  })

  categorySummary.value = Array.from(summary.entries()).map(([category, item]) => ({
    category,
    label: `${item.unread} 条未读 / 共 ${item.total} 条`,
  }))
}

onMounted(loadData)
</script>

<style scoped>
.student-notice-page {
  gap: 20px;
}

:deep(.notice-tabs .el-tabs__header) {
  margin: 0;
}

.notice-list,
.activity-list,
.category-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notice-card,
.category-item {
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
.notice-card__actions,
.category-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.notice-card__actions {
  justify-content: flex-end;
}

.notice-card__meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.notice-card__category,
.notice-card__priority {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
}

.notice-card__category {
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
.activity-item__time,
.activity-item__actor,
.category-item span {
  color: var(--erp-color-text-tertiary);
  font-size: 12px;
}

.notice-card__title,
.activity-item__title,
.category-item strong {
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

.activity-item {
  display: grid;
  grid-template-columns: 84px minmax(0, 1fr);
  gap: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.activity-item:last-child {
  padding-bottom: 0;
  border-bottom: none;
}

.activity-item__actor {
  margin-top: 6px;
}

.notice-detail {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.notice-detail__head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
}

.notice-detail__head h2 {
  margin: 12px 0 0;
  color: var(--erp-color-text-primary);
  font-size: 22px;
  line-height: 1.4;
}

.notice-detail__meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.notice-detail__body {
  padding: 18px;
  border-radius: 14px;
  color: var(--erp-color-text-secondary);
  line-height: 1.8;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
}
</style>
