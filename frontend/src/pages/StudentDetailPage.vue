<template>
  <div class="page">
    <PageHeader
      title="学生详情"
      description="详情页使用统一摘要、分组信息和留痕结构，适合主数据、审批单、账单等 ERP 详情场景复用。"
      :breadcrumbs="['基础数据', '学生档案', student?.studentName ?? '学生详情']"
    >
      <template #actions>
        <el-button @click="router.push('/students')">返回列表</el-button>
        <el-button
          type="primary"
          :disabled="!student"
          @click="router.push(`/students/${route.params.id}/edit`)"
        >
          编辑档案
        </el-button>
      </template>
    </PageHeader>

    <div class="student-overview surface">
      <div class="student-overview__main">
        <div class="student-overview__title">
          <h2>{{ student?.studentName ?? '--' }}</h2>
          <StatusTag :status="student?.status ?? 'INFO'" />
        </div>
        <div class="student-overview__meta">
          <span>学号：{{ student?.studentNo ?? '--' }}</span>
          <span>班级：{{ student?.className ?? '--' }}</span>
          <span>辅导员：{{ student?.classTeacher ?? '--' }}</span>
          <span>校区：{{ student?.campus ?? '--' }}</span>
        </div>
      </div>
      <div class="summary-pills">
        <div class="summary-pill">
          <div class="label">当前状态</div>
          <div class="value">{{ statusLabel }}</div>
        </div>
        <div class="summary-pill">
          <div class="label">最近异动</div>
          <div class="value">{{ latestLogDate }}</div>
        </div>
        <div class="summary-pill">
          <div class="label">风险提醒</div>
          <div class="value">{{ riskTip }}</div>
        </div>
      </div>
    </div>

    <div class="page-grid two-columns">
      <div class="section-stack">
        <DetailSection title="基础信息" description="展示学生档案主数据，敏感字段按实际权限控制脱敏。">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="学生姓名">{{ student?.studentName ?? '--' }}</el-descriptions-item>
            <el-descriptions-item label="学号">{{ student?.studentNo ?? '--' }}</el-descriptions-item>
            <el-descriptions-item label="性别">{{ student?.gender ?? '--' }}</el-descriptions-item>
            <el-descriptions-item label="身份证号">{{ student?.idCardMasked ?? '--' }}</el-descriptions-item>
            <el-descriptions-item label="年级">{{ student?.gradeName ?? '--' }}</el-descriptions-item>
            <el-descriptions-item label="班级">{{ student?.className ?? '--' }}</el-descriptions-item>
            <el-descriptions-item label="入学日期">{{ student?.admissionDate ?? '--' }}</el-descriptions-item>
            <el-descriptions-item label="宿舍">{{ student?.dormitory ?? '--' }}</el-descriptions-item>
          </el-descriptions>
        </DetailSection>

        <DetailSection title="联系信息" description="使用表格结构展示联系人、关系和联系方式，适合 ERP 长列表阅读。">
          <el-table :data="student?.contacts ?? []" border>
            <el-table-column prop="label" label="类型" width="120" />
            <el-table-column prop="name" label="姓名" width="120" />
            <el-table-column prop="relation" label="关系" width="120" />
            <el-table-column prop="phone" label="联系电话" min-width="160" />
          </el-table>
        </DetailSection>

        <DetailSection title="备注信息" description="用于记录需长期跟踪的档案说明。">
          <el-alert type="info" :closable="false" show-icon :title="student?.remark ?? '暂无备注'" />
        </DetailSection>
      </div>

      <div class="section-stack">
        <DetailSection title="档案状态" description="统一承载业务状态、归属组织和最新地址等摘要信息。">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="所属校区">{{ student?.campus ?? '--' }}</el-descriptions-item>
            <el-descriptions-item label="辅导员">{{ student?.classTeacher ?? '--' }}</el-descriptions-item>
            <el-descriptions-item label="当前状态">{{ statusLabel }}</el-descriptions-item>
            <el-descriptions-item label="联系地址">{{ student?.address ?? '--' }}</el-descriptions-item>
          </el-descriptions>
        </DetailSection>

        <DetailSection title="操作留痕" description="关键主数据变更保留时间、动作和操作者，方便审计与回查。">
          <el-timeline>
            <el-timeline-item
              v-for="log in student?.logs ?? []"
              :key="`${log.time}-${log.content}`"
              :timestamp="log.time"
            >
              <div class="timeline-entry__title">{{ log.content }}</div>
              <div class="timeline-entry__meta">{{ log.actor }}</div>
            </el-timeline-item>
          </el-timeline>
        </DetailSection>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchStudentDetail, type StudentDetailData } from '../api/erp'
import DetailSection from '../components/common/DetailSection.vue'
import PageHeader from '../components/common/PageHeader.vue'
import StatusTag from '../components/common/StatusTag.vue'
import { showRequestError } from '../lib/feedback'

const route = useRoute()
const router = useRouter()
const student = ref<StudentDetailData | null>(null)

const statusLabel = computed(() => {
  switch (student.value?.status) {
    case 'ACTIVE':
      return '在读'
    case 'LEAVE':
      return '请假中'
    case 'SUSPENDED':
      return '休学'
    default:
      return '--'
  }
})

const latestLogDate = computed(() => {
  return student.value?.logs?.[0]?.time?.split(' ')[0] ?? '--'
})

const riskTip = computed(() => {
  if (student.value?.status === 'LEAVE') {
    return '请假跟进'
  }

  return student.value?.remark ? '查看备注' : '正常'
})

const loadData = async () => {
  try {
    student.value = await fetchStudentDetail(Number(route.params.id))
  } catch (error) {
    showRequestError(error, '学生详情加载失败。')
  }
}

onMounted(loadData)
</script>

<style scoped>
.student-overview {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--erp-space-4);
  padding: 22px 24px;
}

.student-overview__title {
  display: flex;
  align-items: center;
  gap: var(--erp-space-3);
}

.student-overview__title h2 {
  margin: 0;
  font-size: 26px;
}

.student-overview__meta {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px 18px;
  color: var(--erp-color-text-secondary);
}

.timeline-entry__title {
  font-weight: 600;
}

.timeline-entry__meta {
  margin-top: 4px;
  color: var(--erp-color-text-tertiary);
  font-size: var(--erp-font-size-caption);
}

@media (max-width: 960px) {
  .student-overview {
    flex-direction: column;
  }
}
</style>
