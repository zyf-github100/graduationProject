<template>
  <div class="page">
    <PageHeader
      title="审批中心"
      description="审批页采用待办列表 + 申请详情 + 流转时间线 + 审批动作的组合模板，适用于请假、调课、报修等固定流程。"
      :breadcrumbs="['审批中心', '待办中心']"
    >
      <template #actions>
        <el-button>已办中心</el-button>
        <el-button type="primary">查看全部流程</el-button>
      </template>
    </PageHeader>

    <FilterPanel description="固定流程场景优先使用业务类型、状态、申请时间和申请人等高频筛选维度。">
      <el-form :model="filters" label-position="top">
        <el-row :gutter="16">
          <el-col :lg="6" :md="12">
            <el-form-item label="流程类型">
              <el-select v-model="filters.bizType" style="width: 100%">
                <el-option label="全部" value="" />
                <el-option label="学生请假" value="学生请假" />
                <el-option label="教师调课" value="教师调课" />
                <el-option label="教室报修" value="教室报修" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :lg="6" :md="12">
            <el-form-item label="当前节点">
              <el-input v-model="filters.node" placeholder="辅导员审批 / 教务管理中心审批" />
            </el-form-item>
          </el-col>
          <el-col :lg="6" :md="12">
            <el-form-item label="申请人">
              <el-input v-model="filters.applicant" placeholder="姓名或班级" />
            </el-form-item>
          </el-col>
          <el-col :lg="6" :md="12">
            <el-form-item label="操作">
              <div class="toolbar-inline">
                <el-button type="primary" @click="loadTasks">查询</el-button>
                <el-button @click="resetFilters">重置</el-button>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </FilterPanel>

    <div class="page-grid two-columns">
      <PageSection title="待办列表" description="左侧保留列表上下文，便于审批人员连续处理多个任务。">
        <el-table
          :data="tasks"
          border
          highlight-current-row
          @current-change="handleRowChange"
        >
          <el-table-column prop="processNo" label="流程编号" min-width="140" />
          <el-table-column prop="bizType" label="类型" width="100" />
          <el-table-column prop="applicantName" label="申请人" width="100" />
          <el-table-column prop="currentNode" label="当前节点" min-width="130" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <StatusTag :status="row.status" />
            </template>
          </el-table-column>
        </el-table>
      </PageSection>

      <div class="section-stack">
        <div class="surface approval-summary">
          <div class="approval-summary__header">
            <div>
              <div class="label-muted">当前选中流程</div>
              <h2>{{ selectedTask.processNo }}</h2>
            </div>
            <StatusTag :status="selectedTask.status" />
          </div>
          <div class="approval-summary__meta">
            <span>类型：{{ selectedTask.bizType }}</span>
            <span>申请人：{{ selectedTask.applicantName }}（{{ selectedTask.applicantRole }}）</span>
            <span>归属：{{ selectedTask.className }}</span>
          </div>
        </div>

        <DetailSection title="申请信息" description="详情区突出申请内容、业务时间和关联对象，避免审批人来回跳转。">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="申请事由">{{ selectedTask.reason }}</el-descriptions-item>
            <el-descriptions-item label="业务时间">{{ selectedTask.duration }}</el-descriptions-item>
            <el-descriptions-item label="提交时间">{{ selectedTask.submittedAt }}</el-descriptions-item>
            <el-descriptions-item label="当前节点">{{ selectedTask.currentNode }}</el-descriptions-item>
          </el-descriptions>
        </DetailSection>

        <DetailSection title="审批动作" description="动作区固定在详情上下文内，支持审批意见、驳回原因和转交操作。">
          <el-form label-position="top">
            <el-form-item label="审批意见">
              <el-input
                v-model="approvalOpinion"
                type="textarea"
                :rows="4"
                placeholder="请输入审批意见或补充说明。"
              />
            </el-form-item>
            <div class="toolbar-inline">
              <el-button @click="submitTransfer">转交处理</el-button>
              <div class="toolbar-inline">
                <el-button type="danger" @click="submitAction('REJECT')">驳回</el-button>
                <el-button type="primary" @click="submitAction('APPROVE')">审批通过</el-button>
              </div>
            </div>
          </el-form>
        </DetailSection>

        <DetailSection title="流转记录" description="统一时间线结构便于展示流程节点、动作结果和处理人。">
          <el-timeline>
            <el-timeline-item
              v-for="item in timeline"
              :key="item.time + item.title"
              :timestamp="item.time"
            >
              <div class="timeline-entry__title">{{ item.title }}</div>
              <div class="timeline-entry__meta">{{ item.actor }}</div>
              <div class="timeline-entry__desc">{{ item.description }}</div>
            </el-timeline-item>
          </el-timeline>
        </DetailSection>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  approveWorkflowTask,
  fetchWorkflowTaskDetail,
  fetchWorkflowTasks,
  fetchWorkflowTimeline,
} from '../api/erp'
import DetailSection from '../components/common/DetailSection.vue'
import FilterPanel from '../components/common/FilterPanel.vue'
import PageHeader from '../components/common/PageHeader.vue'
import PageSection from '../components/common/PageSection.vue'
import StatusTag from '../components/common/StatusTag.vue'
import { showRequestError } from '../lib/feedback'
import type { ApprovalTask } from '../types'
import type { TimelineItem } from '../types'

const filters = reactive({
  bizType: '',
  node: '',
  applicant: '',
})

const emptyTask: ApprovalTask = {
  id: 0,
  processNo: '--',
  bizType: '--',
  applicantName: '--',
  applicantRole: '--',
  className: '--',
  status: 'INFO',
  currentNode: '--',
  submittedAt: '--',
  reason: '--',
  duration: '--',
}

const tasks = ref<ApprovalTask[]>([])
const selectedTask = ref<ApprovalTask>({ ...emptyTask })
const timeline = ref<TimelineItem[]>([])
const approvalOpinion = ref('')

const loadTaskDetail = async (taskId: number) => {
  try {
    const [detail, timelineData] = await Promise.all([
      fetchWorkflowTaskDetail(taskId),
      fetchWorkflowTimeline(taskId),
    ])
    selectedTask.value = detail
    timeline.value = timelineData
  } catch (error) {
    showRequestError(error, '审批详情加载失败。')
  }
}

const loadTasks = async (preferredTaskId?: number) => {
  try {
    const page = await fetchWorkflowTasks({
      pageNo: 1,
      pageSize: 50,
      bizType: filters.bizType,
      node: filters.node,
      applicant: filters.applicant,
    })

    tasks.value = page.records
    const matchedTaskId =
      preferredTaskId && page.records.some((task) => task.id === preferredTaskId)
        ? preferredTaskId
        : page.records[0]?.id

    if (matchedTaskId) {
      await loadTaskDetail(matchedTaskId)
      return
    }

    selectedTask.value = { ...emptyTask }
    timeline.value = []
  } catch (error) {
    showRequestError(error, '审批列表加载失败。')
  }
}

const handleRowChange = async (row?: ApprovalTask) => {
  if (row) {
    await loadTaskDetail(row.id)
  }
}

const resetFilters = async () => {
  filters.bizType = ''
  filters.node = ''
  filters.applicant = ''
  await loadTasks()
}

const submitAction = async (action: 'APPROVE' | 'REJECT') => {
  if (!selectedTask.value.id) {
    return
  }

  try {
    await approveWorkflowTask(selectedTask.value.id, {
      action,
      opinion: approvalOpinion.value,
    })
    ElMessage.success(action === 'APPROVE' ? '审批已通过。' : '当前流程已驳回。')
    approvalOpinion.value = ''
    await loadTasks(selectedTask.value.id)
  } catch (error) {
    showRequestError(error, action === 'APPROVE' ? '审批通过失败。' : '驳回流程失败。')
  }
}

const submitTransfer = async () => {
  if (!selectedTask.value.id) {
    return
  }

  try {
    const { value } = await ElMessageBox.prompt('请输入转交处理人。', '转交处理', {
      inputPlaceholder: '例如：软件工程学院辅导员',
    })

    await approveWorkflowTask(selectedTask.value.id, {
      action: 'TRANSFER',
      assignee: value,
      opinion: approvalOpinion.value,
    })
    ElMessage.success('流程已转交处理。')
    approvalOpinion.value = ''
    await loadTasks(selectedTask.value.id)
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }

    showRequestError(error, '转交流程失败。')
  }
}

onMounted(() => {
  void loadTasks()
})
</script>

<style scoped>
.approval-summary {
  padding: 20px;
}

.approval-summary__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--erp-space-4);
}

.approval-summary__header h2 {
  margin: 8px 0 0;
  font-size: 22px;
}

.approval-summary__meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 16px;
  color: var(--erp-color-text-secondary);
}

.timeline-entry__title {
  font-weight: 600;
}

.timeline-entry__meta,
.timeline-entry__desc {
  margin-top: 4px;
  color: var(--erp-color-text-secondary);
}
</style>
