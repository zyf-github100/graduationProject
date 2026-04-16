<template>
  <div class="page teacher-grades-page">
    <PageHeader
      title="成绩录入"
      description="成绩页以任务状态、录入名单和发布流程为核心，优先保证教师批量处理的效率。"
      :breadcrumbs="['教师服务', '成绩录入']"
    >
      <template #actions>
        <el-button>导出成绩模板</el-button>
        <el-button type="primary">提交成绩发布</el-button>
      </template>
    </PageHeader>

    <div class="summary-pills">
      <div v-for="item in summary" :key="item.label" class="summary-pill">
        <div class="label">{{ item.label }}</div>
        <div class="value">{{ item.value }}</div>
      </div>
    </div>

    <PageSection title="成绩任务" description="先展示当前需要处理的录入任务，再进入学生分数表。">
      <el-table :data="tasks" border stripe>
        <el-table-column prop="taskName" label="任务名称" min-width="220" />
        <el-table-column prop="className" label="班级" width="140" />
        <el-table-column prop="deadline" label="截止时间" width="160" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary">{{ row.status === 'TODO' ? '进入录入' : '查看任务' }}</el-button>
            <el-button text>导出</el-button>
          </template>
        </el-table-column>
      </el-table>
    </PageSection>

    <div class="page-grid two-columns">
      <PageSection
        title="学生成绩录入"
        description="列表字段控制在姓名、学号、分数、排名和状态，避免录入时视觉负担过重。"
      >
        <el-table :data="records" border stripe>
          <el-table-column prop="studentName" label="学生姓名" min-width="120" />
          <el-table-column prop="studentNo" label="学号" width="130" />
          <el-table-column prop="className" label="班级" width="140" />
          <el-table-column label="分数" width="110">
            <template #default="{ row }">
              <el-input :model-value="String(row.score)" />
            </template>
          </el-table-column>
          <el-table-column prop="rank" label="班级排名" width="110" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <StatusTag :status="row.status" />
            </template>
          </el-table-column>
        </el-table>
      </PageSection>

      <PageSection title="录入提示" description="成绩页不做炫技可视化，更多聚焦流程提醒和发布规则。">
        <ul class="tips-list">
          <li v-for="tip in tips" :key="tip">{{ tip }}</li>
        </ul>
      </PageSection>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchTeacherGrades, type TeacherGradeRecord, type TeacherGradeTask } from '../api/erp'
import PageHeader from '../components/common/PageHeader.vue'
import PageSection from '../components/common/PageSection.vue'
import StatusTag from '../components/common/StatusTag.vue'
import { showRequestError } from '../lib/feedback'
import type { LabeledValue } from '../types'

const summary = ref<LabeledValue[]>([])
const tasks = ref<TeacherGradeTask[]>([])
const records = ref<TeacherGradeRecord[]>([])
const tips = ref<string[]>([])

const loadData = async () => {
  try {
    const data = await fetchTeacherGrades()
    summary.value = data.summary
    tasks.value = data.tasks
    records.value = data.records
    tips.value = data.tips
  } catch (error) {
    showRequestError(error, '教师成绩任务加载失败。')
  }
}

onMounted(loadData)
</script>

<style scoped>
.teacher-grades-page {
  gap: 20px;
}

.tips-list {
  margin: 0;
  padding-left: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  line-height: 1.8;
  color: var(--erp-color-text-tertiary);
}
</style>
