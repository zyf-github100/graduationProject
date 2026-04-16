<template>
  <div class="page">
    <PageHeader
      title="账单管理"
      description="收费页延续统一筛选、摘要和表格模板，突出账单状态、金额对账和催缴动作。"
      :breadcrumbs="['收费管理', '账单管理']"
    >
      <template #actions>
        <el-button>导出对账单</el-button>
        <el-button>批量催缴</el-button>
        <el-button type="primary">生成账单</el-button>
      </template>
    </PageHeader>

    <div class="page-grid metrics-four bill-metrics">
      <MetricCard
        v-for="metric in billSummary"
        :key="metric.title"
        v-bind="metric"
      />
    </div>

    <FilterPanel description="收费检索以学期、费用项目、账单状态和到期时间为主，支持财务人员快速回查。">
      <el-form :model="filters" label-position="top">
        <el-row :gutter="16">
          <el-col :lg="5" :md="12">
            <el-form-item label="学生 / 学号">
              <el-input v-model="filters.keyword" placeholder="输入学生姓名或学号" />
            </el-form-item>
          </el-col>
          <el-col :lg="5" :md="12">
            <el-form-item label="费用项目">
              <el-select v-model="filters.feeItem" style="width: 100%">
                <el-option label="全部" value="" />
                <el-option
                  v-for="item in feeItemOptions"
                  :key="item"
                  :label="item"
                  :value="item"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :lg="4" :md="12">
            <el-form-item label="账单状态">
              <el-select v-model="filters.status" style="width: 100%">
                <el-option label="全部" value="" />
                <el-option label="已缴清" value="PAID" />
                <el-option label="部分已缴" value="PARTIAL_PAID" />
                <el-option label="待缴费" value="PENDING" />
                <el-option label="已逾期" value="OVERDUE" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :lg="6" :md="12">
            <el-form-item label="到期日期">
              <el-date-picker
                v-model="filters.dateRange"
                type="daterange"
                style="width: 100%"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
              />
            </el-form-item>
          </el-col>
          <el-col :lg="4" :md="12">
            <el-form-item label="操作">
              <div class="toolbar-inline">
                <el-button type="primary" @click="loadBills">查询</el-button>
                <el-button @click="resetFilters">重置</el-button>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </FilterPanel>

    <PageSection title="账单列表" description="通过状态色、金额字段和到期日期帮助财务人员快速识别需跟进账单。">
      <el-table :data="filteredBills" border stripe>
        <el-table-column prop="billNo" label="账单编号" min-width="150" />
        <el-table-column label="学生信息" min-width="180">
          <template #default="{ row }">
            <div class="table-cell-meta">
              <span class="primary">{{ row.studentName }}</span>
              <span class="secondary">{{ row.className }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="feeItemName" label="费用项目" min-width="150" />
        <el-table-column label="应收金额" width="120">
          <template #default="{ row }">
            {{ formatCurrency(row.receivableAmount) }}
          </template>
        </el-table-column>
        <el-table-column label="已收金额" width="120">
          <template #default="{ row }">
            {{ formatCurrency(row.receivedAmount) }}
          </template>
        </el-table-column>
        <el-table-column prop="dueDate" label="截止日期" width="120" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="150">
          <template #default="{ row }">
            <el-button text type="primary">查看</el-button>
            <el-button text>{{ row.status === 'PAID' ? '回单' : '催缴' }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </PageSection>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { fetchBillingSummary, fetchBills } from '../api/erp'
import FilterPanel from '../components/common/FilterPanel.vue'
import MetricCard from '../components/common/MetricCard.vue'
import PageHeader from '../components/common/PageHeader.vue'
import PageSection from '../components/common/PageSection.vue'
import StatusTag from '../components/common/StatusTag.vue'
import { showRequestError } from '../lib/feedback'
import type { BillRecord, DashboardMetric } from '../types'

const billSummary = ref<DashboardMetric[]>([])
const allBills = ref<BillRecord[]>([])

const filters = reactive({
  keyword: '',
  feeItem: '',
  status: '',
  dateRange: [] as Date[],
})

const filteredBills = computed(() => {
  return allBills.value.filter((bill) => {
    const matchesKeyword = !filters.keyword || `${bill.studentName} ${bill.billNo}`.includes(filters.keyword)
    const matchesFeeItem = !filters.feeItem || bill.feeItemName === filters.feeItem
    const matchesStatus = !filters.status || bill.status === filters.status
    const matchesDateRange =
      filters.dateRange.length !== 2 || isDateWithinRange(bill.dueDate, filters.dateRange[0], filters.dateRange[1])

    return matchesKeyword && matchesFeeItem && matchesStatus && matchesDateRange
  })
})

const feeItemOptions = computed(() => {
  return Array.from(new Set(allBills.value.map((bill) => bill.feeItemName))).sort()
})

const loadBills = async () => {
  try {
    const page = await fetchBills({
      pageNo: 1,
      pageSize: 100,
      keyword: filters.keyword,
      status: filters.status,
    })
    allBills.value = page.records
  } catch (error) {
    showRequestError(error, '账单列表加载失败。')
  }
}

const loadSummary = async () => {
  try {
    billSummary.value = await fetchBillingSummary()
  } catch (error) {
    showRequestError(error, '账单摘要加载失败。')
  }
}

const resetFilters = async () => {
  filters.keyword = ''
  filters.feeItem = ''
  filters.status = ''
  filters.dateRange = []
  await Promise.all([loadSummary(), loadBills()])
}

const formatCurrency = (value: number) => {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    maximumFractionDigits: 0,
  }).format(value)
}

const isDateWithinRange = (value: string, start: Date, end: Date) => {
  const current = new Date(`${value}T00:00:00`)
  const rangeStart = new Date(start)
  const rangeEnd = new Date(end)
  rangeStart.setHours(0, 0, 0, 0)
  rangeEnd.setHours(23, 59, 59, 999)
  return current >= rangeStart && current <= rangeEnd
}

onMounted(async () => {
  await Promise.all([loadSummary(), loadBills()])
})
</script>

<style scoped>
.bill-metrics {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

@media (max-width: 1100px) {
  .bill-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
