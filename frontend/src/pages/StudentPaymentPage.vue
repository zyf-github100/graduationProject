<template>
  <div class="page student-payment-page">
    <PageHeader
      title="缴费中心"
      description="学生端优先聚焦待缴账单，让缴费、查看回单和确认到账状态都在一个页面完成。"
      :breadcrumbs="['学生服务', '缴费中心']"
    >
      <template #actions>
        <el-button @click="handleDownload">下载缴费记录</el-button>
        <el-button type="primary" @click="handleImmediatePay">立即缴费</el-button>
      </template>
    </PageHeader>

    <div class="payment-metrics">
      <MetricCard v-for="metric in summary" :key="metric.title" v-bind="metric" />
    </div>

    <div class="page-grid two-columns">
      <PageSection
        title="我的账单"
        description="直接查看每一笔账单的金额、状态和截止时间，待缴账单支持一键支付。"
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
              <el-button
                text
                type="primary"
                @click="row.status === 'PAID' ? openDetailDialog(row.id) : openPaymentDialog(row)"
              >
                {{ row.status === 'PAID' ? '查看回单' : '去缴费' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </PageSection>

      <div class="section-stack">
        <PageSection
          title="待缴提醒"
          description="把最紧急的一笔待缴账单单独突出，避免错过截止日期。"
        >
          <div class="payment-focus surface-muted">
            <div class="payment-focus__label">{{ pendingBill?.feeItemName ?? '暂无待缴账单' }}</div>
            <div class="payment-focus__value">
              {{ pendingBill ? formatCurrency(outstandingAmount(pendingBill)) : '¥0' }}
            </div>
            <div class="payment-focus__meta">
              {{ pendingBill ? `请于 ${pendingBill.dueDate} 前完成支付` : '当前没有需要处理的账单' }}
            </div>
          </div>
        </PageSection>

        <PageSection
          title="缴费说明"
          description="说明区保持业务型表达，帮助学生了解支付成功后的状态变化。"
        >
          <ul class="payment-tips">
            <li>线上支付成功后，账单状态会立即更新为已缴清或部分已缴。</li>
            <li>如需分次支付，系统会保留历史回单，直到整笔账单结清。</li>
            <li>若需缓缴，请先联系辅导员或财务中心完成审批。</li>
          </ul>
        </PageSection>
      </div>
    </div>

    <BillDetailDialog v-model="detailVisible" :bill-id="detailBillId" />

    <el-dialog v-model="paymentVisible" title="支付账单" width="520px">
      <el-form :model="paymentForm" label-position="top">
        <el-alert
          v-if="activeBill"
          type="warning"
          :closable="false"
          class="payment-dialog__alert"
          :title="`${activeBill.feeItemName}`"
          :description="`待缴金额 ${formatCurrency(outstandingAmount(activeBill))}`"
        />

        <el-form-item label="支付金额">
          <el-input-number v-model="paymentForm.receiptAmount" :min="1" :max="activeBill ? outstandingAmount(activeBill) : 1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="支付渠道">
          <el-select v-model="paymentForm.paymentChannel" style="width: 100%">
            <el-option label="支付宝" value="ALIPAY" />
            <el-option label="微信支付" value="WECHAT" />
            <el-option label="银行卡" value="BANK_TRANSFER" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="toolbar-inline">
          <el-button @click="paymentVisible = false">取消</el-button>
          <el-button type="primary" :loading="paymentSaving" @click="submitPayment">
            确认支付
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createReceipt,
  fetchStudentBillingOverview,
} from '../api/erp'
import BillDetailDialog from '../components/billing/BillDetailDialog.vue'
import MetricCard from '../components/common/MetricCard.vue'
import PageHeader from '../components/common/PageHeader.vue'
import PageSection from '../components/common/PageSection.vue'
import StatusTag from '../components/common/StatusTag.vue'
import { statusMeta } from '../constants/status'
import { downloadCsv, todayFileToken } from '../lib/export'
import { showRequestError } from '../lib/feedback'
import type { BillRecord, DashboardMetric } from '../types'

const summary = ref<DashboardMetric[]>([])
const bills = ref<BillRecord[]>([])
const pendingBill = ref<BillRecord | null>(null)

const detailVisible = ref(false)
const detailBillId = ref<number | null>(null)

const paymentVisible = ref(false)
const paymentSaving = ref(false)
const activeBill = ref<BillRecord | null>(null)
const paymentForm = ref({
  paymentChannel: 'ALIPAY',
  receiptAmount: 1,
  sourceType: 'STUDENT_PORTAL',
})

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

const handleDownload = () => {
  if (!bills.value.length) {
    ElMessage.warning('当前没有可下载的缴费记录。')
    return
  }

  downloadCsv(`缴费记录-${todayFileToken()}.csv`, bills.value, [
    { header: '序号', value: (_row, index) => index + 1 },
    { header: '账单编号', value: (bill) => bill.billNo },
    { header: '费用项目', value: (bill) => bill.feeItemName },
    { header: '应缴金额', value: (bill) => bill.receivableAmount },
    { header: '已缴金额', value: (bill) => bill.receivedAmount },
    { header: '待缴金额', value: (bill) => outstandingAmount(bill) },
    { header: '截止日期', value: (bill) => bill.dueDate },
    { header: '状态', value: (bill) => statusLabel(bill.status) },
  ])
  ElMessage.success(`已下载 ${bills.value.length} 笔缴费记录。`)
}

const handleImmediatePay = () => {
  if (!pendingBill.value) {
    ElMessage.info('当前没有待缴账单。')
    return
  }
  openPaymentDialog(pendingBill.value)
}

const openDetailDialog = (billId: number) => {
  detailBillId.value = billId
  detailVisible.value = true
}

const openPaymentDialog = (bill: BillRecord) => {
  activeBill.value = bill
  paymentForm.value = {
    paymentChannel: 'ALIPAY',
    receiptAmount: outstandingAmount(bill),
    sourceType: 'STUDENT_PORTAL',
  }
  paymentVisible.value = true
}

const submitPayment = async () => {
  if (!activeBill.value) {
    return
  }

  paymentSaving.value = true
  try {
    await createReceipt({
      billId: activeBill.value.id,
      paymentChannel: paymentForm.value.paymentChannel,
      receiptAmount: paymentForm.value.receiptAmount,
      sourceType: paymentForm.value.sourceType,
    })
    ElMessage.success('支付成功，账单状态已更新')
    paymentVisible.value = false
    await loadData()
    openDetailDialog(activeBill.value.id)
  } catch (error) {
    showRequestError(error, '支付失败。')
  } finally {
    paymentSaving.value = false
  }
}

const outstandingAmount = (bill: BillRecord) => bill.receivableAmount - bill.receivedAmount

const statusLabel = (status: string) => statusMeta[status]?.label ?? status

const formatCurrency = (value: number) =>
  new Intl.NumberFormat('zh-CN', {
    currency: 'CNY',
    maximumFractionDigits: 0,
    style: 'currency',
  }).format(value)

onMounted(() => {
  void loadData()
})
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

.payment-dialog__alert {
  margin-bottom: 16px;
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
