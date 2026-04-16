<template>
  <div class="page student-home-page">
    <PageHeader
      title="学生首页"
      description="聚合今日课程、通知提醒、待办事项和近期校园安排，让学生登录后先看到最需要处理的信息。"
      :breadcrumbs="['学生服务', '学生首页']"
    >
      <template #actions>
        <span class="glass-chip">当前学期：{{ store.term }}</span>
      </template>
    </PageHeader>

    <div class="page-grid metrics-four">
      <MetricCard v-for="metric in metrics" :key="metric.title" v-bind="metric" />
    </div>

    <div class="student-home-grid">
      <PageSection
        title="今日课程安排"
        description="课程时间、授课教师与教室信息集中展示，帮助学生快速确认当天行程。"
      >
        <div class="course-list">
          <div v-for="course in todayCourses" :key="course.id" class="course-item">
            <div class="course-item__time">
              <strong>{{ course.time }}</strong>
              <span>{{ course.location }}</span>
            </div>
            <div class="course-item__main">
              <div class="course-item__title">{{ course.courseName }}</div>
              <div class="course-item__meta">{{ course.teacherName }}</div>
            </div>
            <StatusTag :status="course.status" />
          </div>
        </div>
      </PageSection>

      <div class="section-stack">
        <PageSection title="最新通知" description="优先显示与课程考核、缴费和院系事务相关的未读消息。">
          <div class="notice-list">
            <div v-for="notice in latestNotices" :key="notice.id" class="notice-item">
              <div class="notice-item__head">
                <span class="notice-item__category">{{ notice.category }}</span>
                <span class="notice-item__time">{{ notice.publishTime }}</span>
              </div>
              <div class="notice-item__title">{{ notice.title }}</div>
              <div class="notice-item__summary">{{ notice.summary }}</div>
            </div>
          </div>
        </PageSection>

        <PageSection title="近期提醒" description="把测验、讲座和缴费截止时间放在同一侧边区域，减少遗漏。">
          <div class="upcoming-list">
            <div v-for="item in upcomingItems" :key="item.title" class="upcoming-item">
              <div class="upcoming-item__title">{{ item.title }}</div>
              <div class="upcoming-item__meta">{{ item.meta }}</div>
            </div>
          </div>
        </PageSection>
      </div>
    </div>

    <PageSection title="今日动态" description="记录当天完成的签到、实验报告和院系事务，让学生快速确认处理进度。">
      <div class="timeline-list">
        <div
          v-for="item in timeline"
          :key="`${item.time}-${item.content}`"
          class="timeline-item"
        >
          <div class="timeline-item__time">{{ item.time }}</div>
          <div class="timeline-item__dot" />
          <div class="timeline-item__content">
            <div class="timeline-item__title">{{ item.content }}</div>
            <div class="timeline-item__actor">{{ item.actor }}</div>
          </div>
        </div>
      </div>
    </PageSection>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchStudentHome, type ActivityItem, type ReminderItem } from '../api/erp'
import MetricCard from '../components/common/MetricCard.vue'
import PageHeader from '../components/common/PageHeader.vue'
import PageSection from '../components/common/PageSection.vue'
import StatusTag from '../components/common/StatusTag.vue'
import { showRequestError } from '../lib/feedback'
import { useAppStore } from '../stores/app'
import type { DashboardMetric, StudentNotice, StudentTodayCourse } from '../types'

const store = useAppStore()

const metrics = ref<DashboardMetric[]>([])
const todayCourses = ref<StudentTodayCourse[]>([])
const latestNotices = ref<StudentNotice[]>([])
const upcomingItems = ref<ReminderItem[]>([])
const timeline = ref<ActivityItem[]>([])

const loadData = async () => {
  try {
    const data = await fetchStudentHome()
    metrics.value = data.metrics
    todayCourses.value = data.todayCourses
    latestNotices.value = data.latestNotices
    upcomingItems.value = data.upcomingItems
    timeline.value = data.timeline
  } catch (error) {
    showRequestError(error, '学生首页加载失败。')
  }
}

onMounted(loadData)
</script>

<style scoped>
.student-home-page {
  gap: 20px;
}

.student-home-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(320px, 1fr);
  gap: 22px;
}

.course-list,
.notice-list,
.upcoming-list,
.timeline-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.course-item,
.notice-item,
.upcoming-item {
  display: flex;
  gap: 16px;
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.09), rgba(255, 255, 255, 0.04));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.course-item {
  align-items: center;
  justify-content: space-between;
}

.course-item__time {
  min-width: 132px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.course-item__time strong,
.notice-item__title,
.upcoming-item__title,
.timeline-item__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--erp-color-text-primary);
}

.course-item__time span,
.course-item__meta,
.notice-item__summary,
.upcoming-item__meta,
.timeline-item__actor {
  color: var(--erp-color-text-tertiary);
  font-size: 12px;
}

.course-item__main {
  min-width: 0;
  flex: 1;
}

.course-item__title {
  font-size: 17px;
  font-weight: 600;
}

.course-item__meta {
  margin-top: 6px;
}

.notice-item,
.upcoming-item {
  flex-direction: column;
}

.notice-item__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--erp-color-text-tertiary);
  font-size: 12px;
}

.notice-item__category {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.notice-item__summary {
  line-height: 1.7;
}

.timeline-item {
  display: grid;
  grid-template-columns: 90px 18px minmax(0, 1fr);
  gap: 12px;
  align-items: start;
}

.timeline-item__time {
  color: var(--erp-color-text-tertiary);
  font-size: 12px;
  padding-top: 2px;
}

.timeline-item__dot {
  width: 10px;
  height: 10px;
  margin-top: 6px;
  border-radius: 50%;
  background: rgba(160, 216, 250, 0.92);
  box-shadow: 0 0 0 4px rgba(160, 216, 250, 0.14);
}

.timeline-item__content {
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.timeline-item:last-child .timeline-item__content {
  padding-bottom: 0;
  border-bottom: none;
}

@media (max-width: 1280px) {
  .student-home-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .course-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .timeline-item {
    grid-template-columns: 1fr;
  }
}
</style>
