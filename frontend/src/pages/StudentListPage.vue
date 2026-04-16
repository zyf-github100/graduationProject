<template>
  <div class="page">
    <PageHeader
      title="学生档案"
      description="以学号、班级、状态、入学时间等维度检索学生主档案，支撑教务、审批和收费等模块的主数据引用。"
      :breadcrumbs="['基础数据', '学生档案']"
    >
      <template #actions>
        <el-button>导出档案</el-button>
        <el-button>批量导入</el-button>
        <el-button type="primary" @click="router.push('/students/new')">新建学生</el-button>
      </template>
    </PageHeader>

    <div class="summary-pills">
      <div class="summary-pill">
        <div class="label">在读学生</div>
        <div class="value">{{ summary.currentStudents }}</div>
      </div>
      <div class="summary-pill">
        <div class="label">请假中</div>
        <div class="value">{{ summary.onLeaveStudents }}</div>
      </div>
      <div class="summary-pill">
        <div class="label">待补充联系人信息</div>
        <div class="value">{{ summary.incompleteGuardianProfiles }}</div>
      </div>
      <div class="summary-pill">
        <div class="label">本周新建档案</div>
        <div class="value">{{ summary.weeklyNewRecords }}</div>
      </div>
    </div>

    <FilterPanel description="默认保留高频筛选项，适合后台系统的长期检索和快速回查。">
      <el-form :model="filters" label-position="top">
        <el-row :gutter="16">
          <el-col :lg="6" :md="8" :sm="12">
            <el-form-item label="关键字">
              <el-input v-model="filters.keyword" placeholder="学号 / 学生姓名 / 紧急联系人" />
            </el-form-item>
          </el-col>
          <el-col :lg="4" :md="8" :sm="12">
            <el-form-item label="年级">
              <el-select v-model="filters.grade" style="width: 100%">
                <el-option label="全部" value="" />
                <el-option v-for="grade in options.grades" :key="grade" :label="grade" :value="grade" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :lg="4" :md="8" :sm="12">
            <el-form-item label="学生状态">
              <el-select v-model="filters.status" style="width: 100%">
                <el-option label="全部" value="" />
                <el-option
                  v-for="status in options.statuses"
                  :key="status.value"
                  :label="status.label"
                  :value="status.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :lg="6" :md="12" :sm="24">
            <el-form-item label="入学日期">
              <el-date-picker
                v-model="filters.dateRange"
                type="daterange"
                unlink-panels
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :lg="4" :md="12" :sm="24">
            <el-form-item label="操作">
              <div class="student-list__filter-actions">
                <el-button type="primary" @click="handleSearch">查询</el-button>
                <el-button @click="resetFilters">重置</el-button>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </FilterPanel>

    <PageSection
      title="档案列表"
      description="表格优先承载高频检索、批量勾选和行级跳转，操作保持稳定位置。"
    >
      <template #actions>
        <div class="toolbar-inline">
          <el-button>批量导出</el-button>
          <el-button>批量调整行政班</el-button>
          <el-button text @click="loadStudents">刷新数据</el-button>
        </div>
      </template>

      <el-table :data="records" border stripe>
        <el-table-column type="selection" width="48" />
        <el-table-column label="学生信息" min-width="220">
          <template #default="{ row }">
            <div class="table-cell-meta">
              <span class="primary">{{ row.studentName }}</span>
              <span class="secondary">学号 {{ row.studentNo }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="gender" label="性别" width="80" />
        <el-table-column prop="gradeName" label="年级" width="90" />
        <el-table-column prop="className" label="班级" min-width="140" />
        <el-table-column prop="guardianName" label="紧急联系人" min-width="120" />
        <el-table-column prop="guardianPhone" label="联系电话" min-width="120" />
        <el-table-column prop="admissionDate" label="入学日期" width="120" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="160">
          <template #default="{ row }">
            <el-button text type="primary" @click="router.push(`/students/${row.id}`)">查看</el-button>
            <el-button text @click="router.push(`/students/${row.id}/edit`)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="student-list__pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          layout="total, sizes, prev, pager, next"
          :total="total"
          :page-sizes="[10, 20, 50]"
        />
      </div>
    </PageSection>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  fetchStudentOptions,
  fetchStudents,
  fetchStudentSummary,
  type StudentOptionsData,
  type StudentSummaryData,
} from '../api/erp'
import FilterPanel from '../components/common/FilterPanel.vue'
import PageHeader from '../components/common/PageHeader.vue'
import PageSection from '../components/common/PageSection.vue'
import StatusTag from '../components/common/StatusTag.vue'
import { showRequestError } from '../lib/feedback'
import type { StudentRecord } from '../types'

const router = useRouter()
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const records = ref<StudentRecord[]>([])

const summary = ref<StudentSummaryData>({
  currentStudents: 0,
  onLeaveStudents: 0,
  incompleteGuardianProfiles: 0,
  weeklyNewRecords: 0,
})

const options = ref<StudentOptionsData>({
  campuses: [],
  grades: [],
  genders: [],
  statuses: [],
})

const filters = reactive({
  keyword: '',
  grade: '',
  status: '',
  dateRange: [] as string[],
})

const loadStudents = async () => {
  try {
    const page = await fetchStudents({
      pageNo: currentPage.value,
      pageSize: pageSize.value,
      keyword: filters.keyword,
      grade: filters.grade,
      status: filters.status,
    })
    records.value = page.records
    total.value = page.total
  } catch (error) {
    showRequestError(error, '学生列表加载失败。')
  }
}

const handleSearch = async () => {
  if (currentPage.value !== 1) {
    currentPage.value = 1
    return
  }

  await loadStudents()
}

const resetFilters = async () => {
  filters.keyword = ''
  filters.grade = ''
  filters.status = ''
  filters.dateRange = []
  await handleSearch()
}

watch([currentPage, pageSize], () => {
  void loadStudents()
})

onMounted(async () => {
  try {
    const [summaryData, optionData] = await Promise.all([
      fetchStudentSummary(),
      fetchStudentOptions(),
    ])
    summary.value = summaryData
    options.value = optionData
  } catch (error) {
    showRequestError(error, '学生档案初始化失败。')
  }

  await loadStudents()
})
</script>

<style scoped>
.student-list__filter-actions {
  display: flex;
  gap: var(--erp-space-3);
  flex-wrap: wrap;
}

.student-list__pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}
</style>
