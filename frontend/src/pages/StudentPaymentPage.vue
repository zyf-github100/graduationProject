<template>
  <div class="page student-payment-page">
    <PageHeader
      title="缴费中心"
      description="学生端缴费页优先展示待缴账单、截止日期与支付状态，让支付动作直接、明确、可追踪。"
      :breadcrumbs="['学生服务', '缴费中心']"
    >
      <template #actions>
        <el-button>下载缴费记录</el-button>
        <el-button type="primary">立即缴费</el-button>
      </template>
    </PageHeader>

    <div class="payment-metrics">
      <MetricCard v-for="metric in summary" :key="metric.title" v-bind="metric" />
    </div>

    <div class="page-grid two-columns">
      <PageSection
        title="我的账单"
        description="统一展示费用项目、截止时间和支付状态，不拆成多个二级页面。"
      >
        <el-table :data="bills" border stripe>
          <el-table-column prop="billNo" label="账单编号" min-width="140" />
          <el-table-column prop="feeItemName" label="费用项目" min-width="140" />
          <el-table-column label="应缴金额" width="120">
            <template #default="{ row }">
              {{ formatCurrency(row.receivableAmount) }}
            </template>
          </el-table-column>
          <el-table-column label="已缴金额" width="120">
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
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary">{{ row.status === 'PAID' ? '查看回单' : '去缴费' }}</el-button>
            </template>
          </el-table-column>
        </el-table>
      </PageSection>

      <div class="section-stack">
        <PageSection
          title="待缴提醒"
          description="把最紧急的一笔待缴账单单独提出，避免学生错过截止时间。"
        >
          <div class="payment-focus surface-muted">
            <div class="payment-focus__label">{{ pendingBill?.feeItemName ?? '暂无待缴账单' }}</div>
            <div class="payment-focus__value">
              {{ pendingBill ? formatCurrency(pendingBill.receivableAmount - pendingBill.receivedAmount) : '¥0' }}
            </div>
            <div class="payment-focus__meta">
              {{ pendingBill ? `请于 ${pendingBill.dueDate} 前完成支付` : '当前没有需要处理的账单' }}
            </div>
          </div>
        </PageSection>

        <PageSection
          title="缴费说明"
          description="说明区保持业务型表达，帮助学生知道支付后会发生什么。"
        >
          <ul class="payment-tips">
            <li>线上支付成功后，账单状态将在 1 分钟内自动更新。</li>
            <li>若已线下缴费，请保留回执，财务老师将在对账后同步登记。</li>
            <li>需要申请缓缴时，请先联系辅导员或财务中心完成审批。</li>
          </ul>
        </PageSection>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchStudentBillingOverview } from '../api/erp'
import MetricCard from '../components/common/MetricCard.vue'
import PageHeader from '../components/common/PageHeader.vue'
import PageSection from '../components/common/PageSection.vue'
import StatusTag from '../components/common/StatusTag.vue'
import { showRequestError } from '../lib/feedback'
import type { BillRecord, DashboardMetric } from '../types'

const summary = ref<DashboardMetric[]>([])
const bills = ref<BillRecord[]>([])
const pendingBill = ref<BillRecord | null>(null)

const formatCurrency = (value: number) => {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    maximumFractionDigits: 0,
  }).format(value)
}

const loadData = async () => {
  try {
    const data = await fetchStudentBillingOverview()
    summary.value = data.summary
    bills.value = data.bills
    pendingBill.value = data.pendingBill
  } catch (error) {
    showRequestError(error, '学生账单加载失败。')
  }
}

onMounted(loadData)
</script>

<style scoped>
.student-payment-page {
  gap: 20px;
}

.payment-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--erp-space-6);
}

.payment-focus {
  padding: 20px;
}

.payment-focus__label,
.payment-focus__meta,
.payment-tips li {
  color: var(--erp-color-text-tertiary);
}

.payment-focus__value {
  margin-top: 12px;
  font-size: 34px;
  line-height: 1;
  font-weight: 600;
}

.payment-focus__meta {
  margin-top: 10px;
  font-size: 13px;
}

.payment-tips {
  margin: 0;
  padding-left: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  line-height: 1.8;
}

@media (max-width: 1100px) {
  .payment-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
