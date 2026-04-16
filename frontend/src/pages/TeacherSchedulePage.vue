<template>
  <div class="page teacher-schedule-page">
    <PageHeader
      title="我的课表"
      description="围绕教师一周授课节奏组织信息，重点保证班级、节次、地点与备课时间一眼可见。"
      :breadcrumbs="['教师服务', '我的课表']"
    >
      <template #actions>
        <span class="glass-chip">本周授课 {{ summary.weeklyLessons }}</span>
        <span class="glass-chip">辅导员工作 {{ summary.homeroomLessons }}</span>
      </template>
    </PageHeader>

    <PageSection
      title="周课表"
      description="保持和学生端一致的表格结构，但信息主体替换为授课班级与教学安排。"
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
      <PageSection title="今日授课" description="以顺序列表展示当天课程，方便教师在课间快速查看下一节安排。">
        <div class="today-list">
          <div v-for="course in todayCourses" :key="course.id" class="today-item">
            <div class="today-item__time">{{ course.time }}</div>
            <div class="today-item__main">
              <div class="today-item__title">{{ course.className }} · {{ course.courseName }}</div>
              <div class="today-item__meta">{{ course.location }}</div>
            </div>
            <StatusTag :status="course.status" />
          </div>
        </div>
      </PageSection>

      <PageSection title="教学提醒" description="把和课堂执行直接相关的提醒聚合在侧边区块。">
        <ul class="tips-list">
          <li v-for="tip in tips" :key="tip">{{ tip }}</li>
        </ul>
      </PageSection>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchTeacherSchedule, type TeacherCourse } from '../api/erp'
import PageHeader from '../components/common/PageHeader.vue'
import PageSection from '../components/common/PageSection.vue'
import StatusTag from '../components/common/StatusTag.vue'
import { showRequestError } from '../lib/feedback'
import type { StudentCourseSlot } from '../types'

const summary = ref({
  weeklyLessons: '--',
  homeroomLessons: '--',
})
const weeklySchedule = ref<StudentCourseSlot[]>([])
const todayCourses = ref<TeacherCourse[]>([])
const tips = ref<string[]>([])

const loadData = async () => {
  try {
    const data = await fetchTeacherSchedule()
    summary.value = data.summary
    weeklySchedule.value = data.weeklySchedule
    todayCourses.value = data.todayCourses
    tips.value = data.tips
  } catch (error) {
    showRequestError(error, '教师课表加载失败。')
  }
}

onMounted(loadData)
</script>

<style scoped>
.teacher-schedule-page {
  gap: 20px;
}

.today-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.today-item {
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
  color: var(--erp-color-text-secondary);
  font-size: 13px;
}

.today-item__main {
  flex: 1;
  min-width: 0;
}

.today-item__title {
  font-size: 15px;
  font-weight: 600;
}

.today-item__meta,
.tips-list li {
  margin-top: 6px;
  color: var(--erp-color-text-tertiary);
  font-size: 13px;
}

.tips-list {
  margin: 0;
  padding-left: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  line-height: 1.8;
}
</style>
