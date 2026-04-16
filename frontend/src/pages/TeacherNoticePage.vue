<template>
  <div class="page teacher-notice-page">
    <PageHeader
      title="通知中心"
      description="集中承载教务、教研与行政消息，帮助教师快速定位和教学执行相关的关键信息。"
      :breadcrumbs="['教师服务', '通知中心']"
    >
      <template #actions>
        <span class="glass-chip">未读 {{ unreadCount }} 条</span>
      </template>
    </PageHeader>

    <PageSection
      title="通知列表"
      description="默认按时间倒序展示，重要通知单独标识但不过度打扰主阅读。"
    >
      <div class="notice-list">
        <div
          v-for="notice in records"
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
            <el-button text type="primary">{{ notice.isRead ? '查看详情' : '标记已读' }}</el-button>
          </div>
        </div>
      </div>
    </PageSection>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchTeacherNotices } from '../api/erp'
import PageHeader from '../components/common/PageHeader.vue'
import PageSection from '../components/common/PageSection.vue'
import { showRequestError } from '../lib/feedback'
import type { StudentNotice } from '../types'

const records = ref<StudentNotice[]>([])
const unreadCount = ref(0)

const loadData = async () => {
  try {
    const data = await fetchTeacherNotices()
    records.value = data.records
    unreadCount.value = data.unreadCount
  } catch (error) {
    showRequestError(error, '教师通知加载失败。')
  }
}

onMounted(loadData)
</script>

<style scoped>
.teacher-notice-page {
  gap: 20px;
}

.notice-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notice-card {
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
.notice-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
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
.notice-card__footer span {
  color: var(--erp-color-text-tertiary);
  font-size: 12px;
}

.notice-card__title {
  margin-top: 10px;
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
