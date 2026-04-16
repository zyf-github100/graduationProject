<template>
  <div class="page">
    <PageHeader
      title="教务管理"
      description="统一承载课表、成绩和学生信息管理模板，验证高校教务场景下高信息密度页面的稳定性与复用性。"
      :breadcrumbs="['教务管理']"
    >
      <template #actions>
        <el-button>生成课表</el-button>
        <el-button>创建成绩任务</el-button>
        <el-button type="primary">导出教务报表</el-button>
      </template>
    </PageHeader>

    <div class="summary-pills">
      <div
        v-for="item in summaryItems"
        :key="item.label"
        class="summary-pill"
      >
        <div class="label">{{ item.label }}</div>
        <div class="value">{{ item.value }}</div>
      </div>
    </div>

    <FilterPanel description="教务查询优先围绕学期、年级、班级、教师等高频维度组织，避免课表和成绩页面频繁切换筛选心智。">
      <el-form :model="filters" label-position="top">
        <el-row :gutter="16">
          <el-col :lg="6" :md="12">
            <el-form-item label="学期">
              <el-select v-model="filters.term" style="width: 100%">
                <el-option label="2025-2026 学年第二学期" value="2025-2026 学年第二学期" />
                <el-option label="2025-2026 学年第一学期" value="2025-2026 学年第一学期" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :lg="5" :md="12">
            <el-form-item label="年级">
              <el-select v-model="filters.grade" style="width: 100%">
                <el-option label="2025级" value="2025级" />
                <el-option label="2024级" value="2024级" />
                <el-option label="2023级" value="2023级" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :lg="5" :md="12">
            <el-form-item label="班级">
              <el-input v-model="filters.className" placeholder="2025级软件工程1班" />
            </el-form-item>
          </el-col>
          <el-col :lg="4" :md="12">
            <el-form-item label="教师">
              <el-input v-model="filters.teacherName" placeholder="任课教师" />
            </el-form-item>
          </el-col>
          <el-col :lg="4" :md="12">
            <el-form-item label="操作">
              <div class="toolbar-inline">
                <el-button type="primary" @click="loadData">查询</el-button>
                <el-button @click="resetFilters">重置</el-button>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </FilterPanel>

    <PageSection title="教务工作区" description="通过统一 Tabs 模板在一个工作区内切换课表、成绩和学生信息，避免页面语言发散。">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="教学班课表" name="timetable">
          <el-table :data="overview.timetablePreview" border stripe>
            <el-table-column prop="period" label="节次" width="90" />
            <el-table-column prop="monday" label="周一" min-width="180" />
            <el-table-column prop="tuesday" label="周二" min-width="180" />
            <el-table-column prop="wednesday" label="周三" min-width="180" />
            <el-table-column prop="thursday" label="周四" min-width="180" />
            <el-table-column prop="friday" label="周五" min-width="180" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="成绩任务" name="grades">
          <el-table :data="filteredGradeTasks" border stripe>
            <el-table-column prop="taskName" label="任务名称" min-width="220" />
            <el-table-column prop="className" label="适用班级" min-width="180" />
            <el-table-column label="责任教师" width="120">
              <template #default="{ row }">
                {{ row.teacherName ?? '教务管理中心' }}
              </template>
            </el-table-column>
            <el-table-column prop="deadline" label="截止时间" width="160" />
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <StatusTag :status="row.status" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140">
              <template #default>
                <el-button text type="primary">查看</el-button>
                <el-button text>发布</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="学生信息管理" name="roster">
          <el-table :data="filteredRosterPreview" border stripe>
            <el-table-column prop="studentName" label="学生姓名" min-width="140" />
            <el-table-column prop="studentNo" label="学号" width="130" />
            <el-table-column prop="className" label="班级" min-width="140" />
            <el-table-column label="今日考勤" width="110">
              <template #default="{ row }">
                <StatusTag :status="row.attendance" />
              </template>
            </el-table-column>
            <el-table-column prop="score" label="最近成绩" width="100" />
            <el-table-column label="档案状态" width="110">
              <template #default="{ row }">
                <StatusTag :status="row.status" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button text type="primary">{{ row.studentName }} 详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </PageSection>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { fetchAcademicOverview, type AcademicOverviewData } from '../api/erp'
import FilterPanel from '../components/common/FilterPanel.vue'
import PageHeader from '../components/common/PageHeader.vue'
import PageSection from '../components/common/PageSection.vue'
import StatusTag from '../components/common/StatusTag.vue'
import { showRequestError } from '../lib/feedback'

const activeTab = ref('timetable')
const overview = ref<AcademicOverviewData>({
  summary: [],
  gradeTasks: [],
  rosterPreview: [],
  timetablePreview: [],
})

const filters = reactive({
  term: '2025-2026 学年第二学期',
  grade: '2025级',
  className: '2025级软件工程1班',
  teacherName: '',
})

const summaryItems = computed(() => overview.value.summary)

const filteredGradeTasks = computed(() => {
  return overview.value.gradeTasks.filter((task) => {
    const matchesGrade = !filters.grade || task.className.includes(filters.grade)
    const matchesClass = !filters.className || task.className.includes(filters.className)
    const matchesTeacher = !filters.teacherName || (task.teacherName ?? '').includes(filters.teacherName)
    return matchesGrade && matchesClass && matchesTeacher
  })
})

const filteredRosterPreview = computed(() => {
  return overview.value.rosterPreview.filter((student) => {
    const matchesGrade = !filters.grade || student.className.includes(filters.grade)
    const matchesClass = !filters.className || student.className.includes(filters.className)
    return matchesGrade && matchesClass
  })
})

const loadData = async () => {
  try {
    overview.value = await fetchAcademicOverview()
  } catch (error) {
    showRequestError(error, '教务总览加载失败。')
  }
}

const resetFilters = async () => {
  filters.term = '2025-2026 学年第二学期'
  filters.grade = '2025级'
  filters.className = '2025级软件工程1班'
  filters.teacherName = ''
  await loadData()
}

onMounted(loadData)
</script>
