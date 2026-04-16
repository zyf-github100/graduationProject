<template>
  <div class="page teacher-attendance-page">
    <PageHeader
      title="考勤登记"
      description="考勤页优先保证班级筛选、到课状态和备注录入的效率，适合教师高频操作。"
      :breadcrumbs="['教师服务', '考勤登记']"
    >
      <template #actions>
        <el-button>导出考勤</el-button>
        <el-button type="primary">提交本节考勤</el-button>
      </template>
    </PageHeader>

    <PageSection title="当前考勤任务" description="先确定授课班级和日期，再进入点名登记。">
      <el-form :model="filters" label-position="top">
        <el-row :gutter="16">
          <el-col :lg="8" :md="12">
            <el-form-item label="授课班级">
              <el-select v-model="filters.className" style="width: 100%">
                <el-option v-for="item in classOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :lg="8" :md="12">
            <el-form-item label="考勤日期">
              <el-date-picker
                v-model="filters.date"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%"
                placeholder="选择日期"
              />
            </el-form-item>
          </el-col>
          <el-col :lg="8" :md="12">
            <el-form-item label="课程节次">
              <el-select v-model="filters.period" style="width: 100%">
                <el-option v-for="item in periodOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </PageSection>

    <PageSection
      title="学生考勤名单"
      description="保持表格为主要工作区，状态、时间和备注统一在一行内完成查看。"
    >
      <el-table :data="records" border stripe>
        <el-table-column prop="studentName" label="学生姓名" min-width="120" />
        <el-table-column prop="studentNo" label="学号" width="130" />
        <el-table-column prop="className" label="班级" width="140" />
        <el-table-column label="到课状态" width="110">
          <template #default="{ row }">
            <StatusTag :status="row.attendance" />
          </template>
        </el-table-column>
        <el-table-column prop="checkInTime" label="签到时间" width="110" />
        <el-table-column prop="remark" label="备注" min-width="180" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default>
            <el-button text type="primary">修改状态</el-button>
            <el-button text>补充说明</el-button>
          </template>
        </el-table-column>
      </el-table>
    </PageSection>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { fetchTeacherAttendance, type TeacherAttendanceRecord } from '../api/erp'
import PageHeader from '../components/common/PageHeader.vue'
import PageSection from '../components/common/PageSection.vue'
import StatusTag from '../components/common/StatusTag.vue'
import { showRequestError } from '../lib/feedback'
import type { SelectOption } from '../types'

const filters = reactive({
  className: '',
  date: '',
  period: '',
})
const classOptions = ref<SelectOption[]>([])
const periodOptions = ref<SelectOption[]>([])
const records = ref<TeacherAttendanceRecord[]>([])

const loadData = async () => {
  try {
    const data = await fetchTeacherAttendance()
    filters.className = data.filters.className
    filters.date = data.filters.date
    filters.period = data.filters.period
    classOptions.value = data.classOptions
    periodOptions.value = data.periodOptions
    records.value = data.records
  } catch (error) {
    showRequestError(error, '教师考勤数据加载失败。')
  }
}

onMounted(loadData)
</script>

<style scoped>
.teacher-attendance-page {
  gap: 20px;
}
</style>
