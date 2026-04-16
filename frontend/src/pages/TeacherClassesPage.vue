<template>
  <div class="page teacher-classes-page">
    <PageHeader
      title="授课班级"
      description="统一展示授课班级、班级概况和学生名单预览，帮助教师在一个工作面内掌握班级状态。"
      :breadcrumbs="['教师服务', '授课班级']"
    />

    <div class="class-grid">
      <div v-for="item in classes" :key="item.className + item.role" class="surface class-card">
        <div class="class-card__top">
          <div>
            <h2>{{ item.className }}</h2>
            <p>{{ item.role }}</p>
          </div>
          <StatusTag status="INFO" />
        </div>
        <div class="class-card__stats">
          <div class="class-card__stat">
            <span>学生人数</span>
            <strong>{{ item.studentCount }}</strong>
          </div>
          <div class="class-card__stat">
            <span>本周出勤</span>
            <strong>{{ item.attendanceRate }}</strong>
          </div>
          <div class="class-card__stat">
            <span>平均成绩</span>
            <strong>{{ item.averageScore }}</strong>
          </div>
          <div class="class-card__stat">
            <span>待处理事项</span>
            <strong>{{ item.pendingItems }}</strong>
          </div>
        </div>
      </div>
    </div>

    <PageSection
      title="学生名单预览"
      description="班级页面默认提供关键名单与课堂状态，避免频繁跳到其他模块确认学生信息。"
    >
      <el-table :data="roster" border stripe>
        <el-table-column prop="studentName" label="学生姓名" min-width="120" />
        <el-table-column prop="studentNo" label="学号" width="130" />
        <el-table-column prop="className" label="班级" width="140" />
        <el-table-column label="今日考勤" width="110">
          <template #default="{ row }">
            <StatusTag :status="row.attendance" />
          </template>
        </el-table-column>
        <el-table-column prop="latestHomework" label="最近作业" width="120" />
        <el-table-column prop="score" label="最近成绩" width="100" />
      </el-table>
    </PageSection>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchTeacherClasses, type TeacherClassSummary, type TeacherRosterRecord } from '../api/erp'
import PageHeader from '../components/common/PageHeader.vue'
import PageSection from '../components/common/PageSection.vue'
import StatusTag from '../components/common/StatusTag.vue'
import { showRequestError } from '../lib/feedback'

const classes = ref<TeacherClassSummary[]>([])
const roster = ref<TeacherRosterRecord[]>([])

const loadData = async () => {
  try {
    const data = await fetchTeacherClasses()
    classes.value = data.classes
    roster.value = data.roster
  } catch (error) {
    showRequestError(error, '教师授课班级数据加载失败。')
  }
}

onMounted(loadData)
</script>

<style scoped>
.teacher-classes-page {
  gap: 20px;
}

.class-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.class-card {
  padding: 20px;
}

.class-card__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.class-card__top h2 {
  margin: 0;
  font-size: 20px;
}

.class-card__top p {
  margin: 6px 0 0;
  color: var(--erp-color-text-secondary);
  font-size: 13px;
}

.class-card__stats {
  margin-top: 20px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.class-card__stat {
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.class-card__stat span {
  color: var(--erp-color-text-tertiary);
  font-size: 12px;
}

.class-card__stat strong {
  display: block;
  margin-top: 8px;
  font-size: 18px;
}

@media (max-width: 1200px) {
  .class-grid {
    grid-template-columns: 1fr;
  }
}
</style>
