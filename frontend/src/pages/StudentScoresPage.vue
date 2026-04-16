<template>
  <div class="page student-scores-page">
    <PageHeader
      title="我的成绩"
      description="成绩页以课程结果、专业表现与教师评语为核心，优先满足学生查看和自我复盘的高频需求。"
      :breadcrumbs="['学生服务', '我的成绩']"
    />

    <div class="score-metrics">
      <MetricCard v-for="metric in metrics" :key="metric.title" v-bind="metric" />
    </div>

    <div class="page-grid two-columns">
      <PageSection
        title="学科成绩"
        description="采用稳定表格结构展示平时、期中、期末与总评成绩，便于横向比较。"
      >
        <el-table :data="records" border stripe>
          <el-table-column prop="subject" label="科目" min-width="100" />
          <el-table-column prop="continuousScore" label="平时" width="90" />
          <el-table-column prop="midtermScore" label="期中" width="90" />
          <el-table-column prop="finalScore" label="期末" width="90" />
          <el-table-column prop="totalScore" label="总评" width="90" />
          <el-table-column prop="rank" label="课程内排名" width="110" />
          <el-table-column prop="teacherName" label="任课教师" width="110" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <StatusTag :status="row.status" />
            </template>
          </el-table-column>
        </el-table>
      </PageSection>

      <div class="section-stack">
        <PageSection title="成绩解读" description="用短句总结优势学科和待提升学科，让页面既能看结果，也能指导下一步。">
          <div class="analysis-list">
            <div v-for="item in analysis" :key="item.label" class="analysis-item">
              <div class="analysis-item__label">{{ item.label }}</div>
              <div class="analysis-item__value">{{ item.value }}</div>
            </div>
          </div>
        </PageSection>

        <PageSection title="教师评语" description="保留核心反馈，不把成绩页做成装饰化图表页面。">
          <div class="comment-list">
            <div v-for="score in publishedScores" :key="score.subject" class="comment-item">
              <div class="comment-item__head">
                <strong>{{ score.subject }}</strong>
                <span>{{ score.teacherName }}</span>
              </div>
              <div class="comment-item__body">{{ score.comment }}</div>
            </div>
          </div>
        </PageSection>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchStudentScores } from '../api/erp'
import MetricCard from '../components/common/MetricCard.vue'
import PageHeader from '../components/common/PageHeader.vue'
import PageSection from '../components/common/PageSection.vue'
import StatusTag from '../components/common/StatusTag.vue'
import { showRequestError } from '../lib/feedback'
import type { DashboardMetric, LabeledValue, StudentScoreRecord } from '../types'

const metrics = ref<DashboardMetric[]>([])
const records = ref<StudentScoreRecord[]>([])
const analysis = ref<LabeledValue[]>([])

const publishedScores = computed(() => records.value.filter((item) => item.status === 'PUBLISHED'))

const loadData = async () => {
  try {
    const data = await fetchStudentScores()
    metrics.value = data.metrics
    records.value = data.records
    analysis.value = data.analysis
  } catch (error) {
    showRequestError(error, '学生成绩加载失败。')
  }
}

onMounted(loadData)
</script>

<style scoped>
.student-scores-page {
  gap: 20px;
}

.score-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--erp-space-6);
}

.analysis-list,
.comment-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.analysis-item,
.comment-item {
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.09), rgba(255, 255, 255, 0.04));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.analysis-item__label,
.comment-item__head span {
  color: var(--erp-color-text-tertiary);
  font-size: 12px;
}

.analysis-item__value,
.comment-item__head strong {
  margin-top: 6px;
  display: block;
  font-size: 15px;
  font-weight: 600;
}

.comment-item__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.comment-item__body {
  margin-top: 10px;
  color: var(--erp-color-text-secondary);
  line-height: 1.7;
  font-size: 13px;
}

@media (max-width: 1100px) {
  .score-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
