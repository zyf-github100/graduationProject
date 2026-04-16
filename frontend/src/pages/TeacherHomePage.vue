<template>
  <div class="page teacher-home-page">
    <PageHeader
      title="教师首页"
      description="聚合授课、考勤、成绩与校务提醒，帮助教师在进入系统后先处理当天最关键的教学事务。"
      :breadcrumbs="['教师服务', '教师首页']"
    >
      <template #actions>
        <span class="glass-chip">当前周次：第 9 周</span>
      </template>
    </PageHeader>

    <div class="page-grid metrics-four">
      <MetricCard v-for="metric in metrics" :key="metric.title" v-bind="metric" />
    </div>

    <div class="teacher-home-grid">
      <PageSection
        title="今日授课安排"
        description="优先展示当天授课班级、地点和时间，帮助教师快速进入上课状态。"
      >
        <div class="course-list">
          <div v-for="course in todayCourses" :key="course.id" class="course-item">
            <div class="course-item__time">
              <strong>{{ course.time }}</strong>
              <span>{{ course.location }}</span>
            </div>
            <div class="course-item__main">
              <div class="course-item__title">{{ course.className }} · {{ course.courseName }}</div>
              <div class="course-item__meta">授课安排已同步到课表与考勤模块</div>
            </div>
            <StatusTag :status="course.status" />
          </div>
        </div>
      </PageSection>

      <div class="section-stack">
        <PageSection title="待办事项" description="把成绩录入、考勤补录和备课提交集中收口。">
          <div class="task-list">
            <div v-for="task in tasks" :key="task.title" class="task-item">
              <div>
                <div class="task-item__title">{{ task.title }}</div>
                <div class="task-item__meta">{{ task.deadline }}</div>
              </div>
              <StatusTag :status="task.status" />
            </div>
          </div>
        </PageSection>

        <PageSection title="最新通知" description="保留教务、教研与行政通知，不让教师被不相关信息打断。">
          <div class="notice-list">
            <div v-for="notice in latestNotices" :key="notice.id" class="notice-item">
              <div class="notice-item__head">
                <span>{{ notice.category }}</span>
                <span>{{ notice.publishTime }}</span>
              </div>
              <div class="notice-item__title">{{ notice.title }}</div>
              <div class="notice-item__summary">{{ notice.summary }}</div>
            </div>
          </div>
        </PageSection>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchTeacherHome, type TeacherCourse, type TeacherTaskItem } from '../api/erp'
import MetricCard from '../components/common/MetricCard.vue'
import PageHeader from '../components/common/PageHeader.vue'
import PageSection from '../components/common/PageSection.vue'
import StatusTag from '../components/common/StatusTag.vue'
import { showRequestError } from '../lib/feedback'
import type { DashboardMetric, StudentNotice } from '../types'

const metrics = ref<DashboardMetric[]>([])
const todayCourses = ref<TeacherCourse[]>([])
const tasks = ref<TeacherTaskItem[]>([])
const latestNotices = ref<StudentNotice[]>([])

const loadData = async () => {
  try {
    const data = await fetchTeacherHome()
    metrics.value = data.metrics
    todayCourses.value = data.todayCourses
    tasks.value = data.tasks
    latestNotices.value = data.latestNotices
  } catch (error) {
    showRequestError(error, '教师首页加载失败。')
  }
}

onMounted(loadData)
</script>

<style scoped>
.teacher-home-page {
  gap: 20px;
}

.teacher-home-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(320px, 1fr);
  gap: 22px;
}

.course-list,
.task-list,
.notice-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.course-item,
.task-item,
.notice-item {
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.09), rgba(255, 255, 255, 0.04));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.course-item,
.task-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.course-item__time {
  min-width: 138px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.course-item__time span,
.course-item__meta,
.task-item__meta,
.notice-item__head,
.notice-item__summary {
  color: var(--erp-color-text-tertiary);
  font-size: 12px;
}

.course-item__title,
.task-item__title,
.notice-item__title {
  font-size: 15px;
  font-weight: 600;
}

.course-item__meta,
.notice-item__summary {
  margin-top: 6px;
}

.notice-item__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

@media (max-width: 1280px) {
  .teacher-home-grid {
    grid-template-columns: 1fr;
  }
}
</style>
