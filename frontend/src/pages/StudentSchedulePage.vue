<template>
  <div class="page student-schedule-page">
    <PageHeader
      title="我的课表"
      description="围绕学生一天与一周的上课安排组织信息，优先保证节次、教室、教师三类信息清晰可查。"
      :breadcrumbs="['学生服务', '我的课表']"
    >
      <template #actions>
        <span v-if="summary[0]" class="glass-chip">{{ summary[0].value }}</span>
        <span v-if="summary[1]" class="glass-chip">{{ summary[1].value }}</span>
      </template>
    </PageHeader>

    <div class="summary-pills">
      <div v-for="item in summary" :key="item.label" class="summary-pill">
        <div class="label">{{ item.label }}</div>
        <div class="value">{{ item.value }}</div>
      </div>
    </div>

    <PageSection
      title="周课表"
      description="以整周矩阵表为主工作区，适合学生快速查看不同工作日的课程节奏。"
    >
      <el-table :data="weeklySchedule" border stripe>
        <el-table-column prop="period" label="节次" width="90" />
        <el-table-column prop="time" label="时间" width="130" />
        <el-table-column prop="monday" label="周一" min-width="180" />
        <el-table-column prop="tuesday" label="周二" min-width="180" />
        <el-table-column prop="wednesday" label="周三" min-width="180" />
        <el-table-column prop="thursday" label="周四" min-width="180" />
        <el-table-column prop="friday" label="周五" min-width="180" />
      </el-table>
    </PageSection>

    <div class="page-grid two-columns">
      <PageSection title="今日安排" description="按时间顺序列出当天课程，适合课间快速确认下一节课信息。">
        <div class="today-list">
          <div v-for="course in todayCourses" :key="course.id" class="today-item">
            <div class="today-item__time">{{ course.time }}</div>
            <div class="today-item__main">
              <div class="today-item__title">{{ course.courseName }}</div>
              <div class="today-item__meta">{{ course.teacherName }} · {{ course.location }}</div>
            </div>
            <StatusTag :status="course.status" />
          </div>
        </div>
      </PageSection>

      <PageSection title="课程提醒" description="把测验、讲座和课程要求集中展示，避免学生在多个页面之间来回切换。">
        <div class="reminder-list">
          <div v-for="item in upcomingItems" :key="item.title" class="reminder-item">
            <div class="reminder-item__title">{{ item.title }}</div>
            <div class="reminder-item__meta">{{ item.meta }}</div>
          </div>
        </div>
      </PageSection>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchStudentSchedule, type ReminderItem } from '../api/erp'
import PageHeader from '../components/common/PageHeader.vue'
import PageSection from '../components/common/PageSection.vue'
import StatusTag from '../components/common/StatusTag.vue'
import { showRequestError } from '../lib/feedback'
import type { LabeledValue, StudentCourseSlot, StudentTodayCourse } from '../types'

const summary = ref<LabeledValue[]>([])
const weeklySchedule = ref<StudentCourseSlot[]>([])
const todayCourses = ref<StudentTodayCourse[]>([])
const upcomingItems = ref<ReminderItem[]>([])

const loadData = async () => {
  try {
    const data = await fetchStudentSchedule()
    summary.value = data.summary
    weeklySchedule.value = data.weeklySchedule
    todayCourses.value = data.todayCourses
    upcomingItems.value = data.upcomingItems
  } catch (error) {
    showRequestError(error, '学生课表加载失败。')
  }
}

onMounted(loadData)
</script>

<style scoped>
.student-schedule-page {
  gap: 20px;
}

.today-list,
.reminder-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.today-item,
.reminder-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.09), rgba(255, 255, 255, 0.04));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.today-item__time {
  min-width: 104px;
  font-size: 13px;
  color: var(--erp-color-text-secondary);
}

.today-item__main {
  flex: 1;
  min-width: 0;
}

.today-item__title,
.reminder-item__title {
  font-size: 15px;
  font-weight: 600;
}

.today-item__meta,
.reminder-item__meta {
  color: var(--erp-color-text-tertiary);
  font-size: 13px;
}

.today-item__meta,
.reminder-item__meta {
  margin-top: 6px;
}

.reminder-item {
  align-items: flex-start;
  flex-direction: column;
}

@media (max-width: 768px) {
  .today-item {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
