<template>
  <div class="page dashboard-page">
    <PageHeader
      title="Dashboard"
      description="聚合学生、教学、收费和事务提醒，让管理员优先处理当天最重要的运营动作。"
      :breadcrumbs="['系统首页']"
    >
      <template #actions>
        <span class="glass-chip">当前学期：{{ currentTerm }}</span>
      </template>
    </PageHeader>

    <div class="page-grid metrics-four">
      <MetricCard
        v-for="metric in dashboardMetrics"
        :key="metric.title"
        v-bind="metric"
      />
    </div>

    <div class="dashboard-main">
      <PageSection
        title="Priority Tasks & Alerts"
        description="将审批、排课、财务和门户支持等高频任务集中在一个主工作区中。"
      >
        <div class="task-list">
          <div
            v-for="task in dashboardTasks"
            :key="task.title"
            class="task-item"
          >
            <div class="task-item__main">
              <div class="task-item__title">{{ task.title }}</div>
              <div class="task-item__meta">{{ task.owner }}</div>
            </div>
            <div class="task-item__side">
              <span class="task-item__priority">
                {{ task.level === '紧急' ? 'High Priority' : task.level === '重要' ? 'Medium Priority' : 'Low Priority' }}
              </span>
              <StatusTag :status="task.level === '紧急' ? 'DANGER' : task.level === '重要' ? 'WARNING' : 'INFO'" />
            </div>
          </div>
        </div>
      </PageSection>

      <div class="dashboard-side">
        <PageSection
          title="Recent Activities & Notifications"
          description="来自建档、成绩和运维模块的最近更新。"
        >
          <div class="activity-list">
            <div
              v-for="activity in recentActivities"
              :key="activity.title"
              class="activity-item"
            >
              <div class="activity-item__avatar">{{ activity.actor.slice(0, 1) }}</div>
              <div class="activity-item__body">
                <div class="activity-item__title">{{ activity.title }}</div>
                <div class="activity-item__time">{{ activity.time }}</div>
              </div>
            </div>
          </div>
        </PageSection>

        <PageSection
          title="Upcoming Events"
          description="保留近期运维与校务节点，便于提前协调。"
        >
          <div class="event-list">
            <div
              v-for="event in upcomingEvents"
              :key="event.title"
              class="event-item"
            >
              <div class="event-item__date">
                <strong>{{ event.day }}</strong>
                <span>{{ event.month }}</span>
              </div>
              <div class="event-item__body">
                <div class="event-item__title">{{ event.title }}</div>
                <div class="event-item__meta">{{ event.meta }}</div>
              </div>
            </div>
          </div>
        </PageSection>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  fetchAcademicOverview,
  fetchBillingSummary,
  fetchBills,
  fetchStudentSummary,
  fetchWorkflowTodo,
  type AcademicOverviewData,
  type StudentSummaryData,
  type TeacherGradeTask,
  type WorkflowTodoItem,
} from '../api/erp'
import MetricCard from '../components/common/MetricCard.vue'
import PageHeader from '../components/common/PageHeader.vue'
import PageSection from '../components/common/PageSection.vue'
import StatusTag from '../components/common/StatusTag.vue'
import { showRequestError } from '../lib/feedback'
import type { BillRecord, DashboardMetric } from '../types'

interface DashboardTaskItem {
  level: '紧急' | '重要' | '常规'
  owner: string
  title: string
}

interface RecentActivityItem {
  actor: string
  time: string
  title: string
}

interface UpcomingEventItem {
  day: string
  meta: string
  month: string
  title: string
}

const summary = ref<StudentSummaryData>({
  currentStudents: 0,
  incompleteGuardianProfiles: 0,
  onLeaveStudents: 0,
  weeklyNewRecords: 0,
})
const overview = ref<AcademicOverviewData>({
  summary: [],
  gradeTasks: [],
  rosterPreview: [],
  timetablePreview: [],
})
const dashboardMetrics = ref<DashboardMetric[]>([])
const dashboardTasks = ref<DashboardTaskItem[]>([])
const recentActivities = ref<RecentActivityItem[]>([])
const upcomingEvents = ref<UpcomingEventItem[]>([])

const currentTerm = computed(() => {
  return overview.value.summary.find((item) => item.label === '当前学期')?.value ?? '2025-2026 学年第二学期'
})

const buildMetrics = (billingMetrics: DashboardMetric[]) => {
  dashboardMetrics.value = [
    {
      title: '在读学生',
      value: `${summary.value.currentStudents}`,
      trend: `本周新建 ${summary.value.weeklyNewRecords} 份档案`,
      caption: `请假中 ${summary.value.onLeaveStudents} 人`,
      tone: 'primary',
      direction: 'up',
      series: [72, 75, 78, 82, 83, 86, 88, 90],
    },
    {
      title: '待补监护信息',
      value: `${summary.value.incompleteGuardianProfiles}`,
      trend: '需要辅导员与学生补录',
      caption: '主数据完整性持续跟进',
      tone: 'warning',
      direction: 'flat',
      series: [30, 29, 28, 27, 26, 25, 24, 24],
    },
    ...billingMetrics.slice(0, 2),
  ]
}

const buildTasks = (records: WorkflowTodoItem[]) => {
  dashboardTasks.value = records.length
    ? records.map((task) => ({
      title: task.title,
      owner: `${task.currentNode} · 提交于 ${task.createdAt}`,
      level: task.status === 'TODO' ? '紧急' : task.status === 'APPROVING' ? '重要' : '常规',
    }))
    : [
      {
        title: '当前没有待办流程',
        owner: '审批中心',
        level: '常规',
      },
    ]
}

const buildActivities = (gradeTasks: TeacherGradeTask[], bills: BillRecord[]) => {
  const gradeActivities = gradeTasks.slice(0, 2).map((task) => ({
    actor: task.teacherName ?? '教务管理中心',
    title: task.taskName,
    time: `截止 ${task.deadline}`,
  }))
  const billActivities = bills.slice(0, 2).map((bill) => ({
    actor: '财务中心',
    title: `${bill.studentName} ${bill.feeItemName}`,
    time: `到期 ${bill.dueDate}`,
  }))

  recentActivities.value = [...gradeActivities, ...billActivities]
}

const buildEvents = (todos: WorkflowTodoItem[], gradeTasks: TeacherGradeTask[], bills: BillRecord[]) => {
  const workflowEvents = todos.slice(0, 2).map((task) => ({
    title: task.title,
    meta: `${task.currentNode} · ${task.status}`,
    date: task.dueAt,
  }))
  const academicEvents = gradeTasks.slice(0, 2).map((task) => ({
    title: task.taskName,
    meta: task.className,
    date: task.deadline,
  }))
  const billingEvents = bills.slice(0, 2).map((bill) => ({
    title: bill.feeItemName,
    meta: `${bill.studentName} · ${bill.status}`,
    date: bill.dueDate,
  }))

  upcomingEvents.value = [...workflowEvents, ...academicEvents, ...billingEvents]
    .sort((left, right) => toTimestamp(left.date) - toTimestamp(right.date))
    .slice(0, 4)
    .map((item) => ({
      title: item.title,
      meta: item.meta,
      day: formatDay(item.date),
      month: formatMonth(item.date),
    }))
}

const loadData = async () => {
  try {
    const [summaryData, billingMetrics, workflowTodo, academicOverview, billPage] = await Promise.all([
      fetchStudentSummary(),
      fetchBillingSummary(),
      fetchWorkflowTodo({ pageNo: 1, pageSize: 5 }),
      fetchAcademicOverview(),
      fetchBills({ pageNo: 1, pageSize: 4 }),
    ])

    summary.value = summaryData
    overview.value = academicOverview
    buildMetrics(billingMetrics)
    buildTasks(workflowTodo.records)
    buildActivities(academicOverview.gradeTasks, billPage.records)
    buildEvents(workflowTodo.records, academicOverview.gradeTasks, billPage.records)
  } catch (error) {
    showRequestError(error, '首页数据加载失败。')
  }
}

const toTimestamp = (value: string) => {
  const normalized = value.replace(' ', 'T')
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? Number.MAX_SAFE_INTEGER : date.getTime()
}

const normalizeDate = (value: string) => value.split(' ')[0]

const formatDay = (value: string) => {
  return normalizeDate(value).split('-')[2] ?? '--'
}

const formatMonth = (value: string) => {
  const month = normalizeDate(value).split('-')[1]
  return month ? `${month}月` : '--'
}

onMounted(loadData)
</script>

<style scoped>
.dashboard-page {
  gap: 20px;
}

.dashboard-main {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(320px, 0.95fr);
  gap: 22px;
}

.dashboard-side {
  display: grid;
  gap: 18px;
  align-content: start;
}

.task-list,
.activity-list,
.event-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.09), rgba(255, 255, 255, 0.04));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.task-item__main {
  min-width: 0;
}

.task-item__title {
  font-size: 16px;
  font-weight: 500;
}

.task-item__meta {
  margin-top: 6px;
  color: var(--erp-color-text-tertiary);
  font-size: 12px;
}

.task-item__side {
  display: flex;
  align-items: center;
  gap: 12px;
}

.task-item__priority {
  color: var(--erp-color-text-secondary);
  font-size: 13px;
  white-space: nowrap;
}

.activity-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.activity-item__avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.24), rgba(255, 255, 255, 0.08));
  border: 1px solid rgba(255, 255, 255, 0.14);
  color: var(--erp-color-text-primary);
  flex: none;
}

.activity-item__title,
.event-item__title {
  font-size: 15px;
  color: var(--erp-color-text-primary);
}

.activity-item__time,
.event-item__meta {
  margin-top: 4px;
  font-size: 12px;
  color: var(--erp-color-text-tertiary);
}

.event-item {
  display: grid;
  grid-template-columns: 62px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
}

.event-item__date {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60px;
  border-right: 1px solid rgba(255, 255, 255, 0.12);
}

.event-item__date strong {
  font-size: 30px;
  line-height: 1;
  font-weight: 500;
}

.event-item__date span {
  margin-top: 6px;
  font-size: 12px;
  color: var(--erp-color-text-tertiary);
}

@media (max-width: 1280px) {
  .dashboard-main {
    grid-template-columns: 1fr;
  }
}
</style>
