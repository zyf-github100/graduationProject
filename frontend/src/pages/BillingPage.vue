<template>
  <div class="page billing-page">
    <PageHeader
      title="账单管理"
      description="统一处理账单生成、详情查看、收款登记和批量催缴，让财务高频动作在一个页面内闭环。"
      :breadcrumbs="['收费管理', '账单管理']"
    >
      <template #actions>
        <el-button @click="handleExport">导出对账单</el-button>
        <el-button
          :disabled="selectedRemindableBills.length === 0"
          :loading="remindSending"
          @click="handleBatchRemind"
        >
          批量催缴
        </el-button>
        <el-button type="primary" @click="openGenerateDialog">生成账单</el-button>
      </template>
    </PageHeader>

    <div class="page-grid metrics-four bill-metrics">
      <MetricCard
        v-for="metric in billSummary"
        :key="metric.title"
        v-bind="metric"
      />
    </div>

    <FilterPanel description="先按学生、费用项目、账单状态和到期日期过滤，再查看详情或登记收款。">
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
                <el-option label="已结清" value="PAID" />
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
                value-format="YYYY-MM-DD"
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

    <PageSection
      title="账单列表"
      description="支持勾选未结清账单批量发送缴费提醒，也可直接查看详情或登记收款。"
    >
      <template #actions>
        <span class="glass-chip">已选 {{ selectedRemindableBills.length }} 笔待催缴</span>
      </template>

      <el-table
        ref="billTableRef"
        :data="filteredBills"
        border
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column
          type="selection"
          width="48"
          :selectable="isBillSelectable"
        />
        <el-table-column prop="billNo" label="账单编号" min-width="150" />
        <el-table-column label="学生信息" min-width="220">
          <template #default="{ row }">
            <div class="table-cell-meta">
              <span class="primary">{{ row.studentName }}</span>
              <span class="secondary">学号 {{ row.studentNo }} · {{ row.className }}</span>
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
        <el-table-column label="待缴金额" width="120">
          <template #default="{ row }">
            {{ formatCurrency(outstandingAmount(row)) }}
          </template>
        </el-table-column>
        <el-table-column prop="dueDate" label="到期日期" width="120" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="170">
          <template #default="{ row }">
            <el-button text type="primary" @click="openDetailDialog(row.id)">查看</el-button>
            <el-button
              text
              @click="row.status === 'PAID' ? openDetailDialog(row.id) : openReceiptDialog(row)"
            >
              {{ row.status === 'PAID' ? '查看回单' : '登记收款' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </PageSection>

    <BillDetailDialog v-model="detailVisible" :bill-id="detailBillId" />

    <el-dialog v-model="generateVisible" title="生成账单" width="560px">
      <el-form :model="generateForm" label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="学生姓名">
              <el-input v-model="generateForm.studentName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学号">
              <el-input v-model="generateForm.studentNo" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="班级">
              <el-input v-model="generateForm.className" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="费用项目">
              <el-input v-model="generateForm.feeItemName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="应收金额">
              <el-input-number
                v-model="generateForm.receivableAmount"
                :min="1"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="到期日期">
              <el-date-picker
                v-model="generateForm.dueDate"
                type="date"
                style="width: 100%"
                value-format="YYYY-MM-DD"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <div class="toolbar-inline">
          <el-button @click="generateVisible = false">取消</el-button>
          <el-button type="primary" :loading="generateSaving" @click="submitGenerate">
            提交生成
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="receiptVisible" title="登记收款" width="520px">
      <el-form :model="receiptForm" label-position="top">
        <el-alert
          v-if="activeBill"
          type="info"
          :closable="false"
          class="receipt-dialog__alert"
          :title="`${activeBill.studentName} · ${activeBill.feeItemName}`"
          :description="`待缴金额 ${formatCurrency(outstandingAmount(activeBill))}`"
        />

        <el-form-item label="收款金额">
          <el-input-number
            v-model="receiptForm.receiptAmount"
            :min="1"
            :max="activeBill ? outstandingAmount(activeBill) : 1"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="支付渠道">
          <el-select v-model="receiptForm.paymentChannel" style="width: 100%">
            <el-option label="财务台账" value="FINANCE_DESK" />
            <el-option label="银行转账" value="BANK_TRANSFER" />
            <el-option label="支付宝" value="ALIPAY" />
            <el-option label="微信支付" value="WECHAT" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="toolbar-inline">
          <el-button @click="receiptVisible = false">取消</el-button>
          <el-button type="primary" :loading="receiptSaving" @click="submitReceipt">
            确认登记
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, type TableInstance } from 'element-plus'
import {
  createReceipt,
  fetchBillingSummary,
  fetchBills,
  generateBill,
  sendNotifyMessage,
  type GenerateBillResult,
} from '../api/erp'
import BillDetailDialog from '../components/billing/BillDetailDialog.vue'
import FilterPanel from '../components/common/FilterPanel.vue'
import MetricCard from '../components/common/MetricCard.vue'
import PageHeader from '../components/common/PageHeader.vue'
import PageSection from '../components/common/PageSection.vue'
import StatusTag from '../components/common/StatusTag.vue'
import { statusMeta } from '../constants/status'
import { downloadCsv, todayFileToken } from '../lib/export'
import { showRequestError } from '../lib/feedback'
import { useAppStore } from '../stores/app'
import type { BillRecord, DashboardMetric } from '../types'

const appStore = useAppStore()
const billTableRef = ref<TableInstance>()
const billSummary = ref<DashboardMetric[]>([])
const allBills = ref<BillRecord[]>([])
const selectedBills = ref<BillRecord[]>([])

const filters = reactive({
  keyword: '',
  feeItem: '',
  status: '',
  dateRange: [] as string[],
})

const detailVisible = ref(false)
const detailBillId = ref<number | null>(null)

const generateVisible = ref(false)
const generateSaving = ref(false)
const generateForm = reactive({
  studentName: '林嘉禾',
  studentNo: '202501001',
  className: '2025级软件工程1班',
  feeItemName: '实训材料费',
  receivableAmount: 600,
  dueDate: nextWeekDate(),
})

const receiptVisible = ref(false)
const receiptSaving = ref(false)
const remindSending = ref(false)
const activeBill = ref<BillRecord | null>(null)
const receiptForm = reactive({
  paymentChannel: 'FINANCE_DESK',
  receiptAmount: 1,
  sourceType: 'FINANCE_DESK',
})

const filteredBills = computed(() =>
  allBills.value.filter((bill) => {
    const matchesFeeItem = !filters.feeItem || bill.feeItemName === filters.feeItem
    const matchesDateRange =
      filters.dateRange.length !== 2 ||
      isDateWithinRange(bill.dueDate, filters.dateRange[0], filters.dateRange[1])

    return matchesFeeItem && matchesDateRange
  }),
)

const feeItemOptions = computed(() =>
  Array.from(new Set(allBills.value.map((bill) => bill.feeItemName))).sort(),
)

const selectedRemindableBills = computed(() =>
  selectedBills.value.filter((bill) => isBillSelectable(bill)),
)

const loadBills = async () => {
  try {
    const page = await fetchBills({
      keyword: filters.keyword,
      pageNo: 1,
      pageSize: 100,
      status: filters.status,
    })
    allBills.value = page.records
    selectedBills.value = []
    await nextTick()
    billTableRef.value?.clearSelection()
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

const refreshPage = async () => {
  await Promise.all([loadSummary(), loadBills()])
}

const resetFilters = async () => {
  filters.keyword = ''
  filters.feeItem = ''
  filters.status = ''
  filters.dateRange = []
  await refreshPage()
}

const openDetailDialog = (billId: number) => {
  detailBillId.value = billId
  detailVisible.value = true
}

const openGenerateDialog = () => {
  generateForm.dueDate = nextWeekDate()
  generateVisible.value = true
}

const openReceiptDialog = (bill: BillRecord) => {
  activeBill.value = bill
  receiptForm.paymentChannel = 'FINANCE_DESK'
  receiptForm.sourceType = 'FINANCE_DESK'
  receiptForm.receiptAmount = outstandingAmount(bill)
  receiptVisible.value = true
}

const submitGenerate = async () => {
  if (
    !generateForm.studentName ||
    !generateForm.studentNo ||
    !generateForm.className ||
    !generateForm.feeItemName ||
    !generateForm.dueDate
  ) {
    ElMessage.warning('请完整填写账单信息。')
    return
  }

  generateSaving.value = true
  try {
    const result: GenerateBillResult = await generateBill({
      className: generateForm.className,
      dueDate: generateForm.dueDate,
      feeItemName: generateForm.feeItemName,
      receivableAmount: generateForm.receivableAmount,
      studentName: generateForm.studentName,
      studentNo: generateForm.studentNo,
    })
    ElMessage.success(`账单已生成：${result.billNo}`)
    generateVisible.value = false
    await refreshPage()
  } catch (error) {
    showRequestError(error, '生成账单失败。')
  } finally {
    generateSaving.value = false
  }
}

const submitReceipt = async () => {
  if (!activeBill.value) {
    return
  }

  receiptSaving.value = true
  try {
    await createReceipt({
      billId: activeBill.value.id,
      paymentChannel: receiptForm.paymentChannel,
      receiptAmount: receiptForm.receiptAmount,
      sourceType: receiptForm.sourceType,
    })
    ElMessage.success('收款登记成功')
    receiptVisible.value = false
    await refreshPage()
    openDetailDialog(activeBill.value.id)
  } catch (error) {
    showRequestError(error, '收款登记失败。')
  } finally {
    receiptSaving.value = false
  }
}

const handleExport = () => {
  const rows = filteredBills.value

  if (!rows.length) {
    ElMessage.warning('当前筛选条件下没有可导出的账单。')
    return
  }

  downloadCsv(`账单对账单-${todayFileToken()}.csv`, rows, [
    { header: '序号', value: (_row, index) => index + 1 },
    { header: '账单编号', value: (bill) => bill.billNo },
    { header: '学生姓名', value: (bill) => bill.studentName },
    { header: '学号', value: (bill) => bill.studentNo },
    { header: '班级', value: (bill) => bill.className },
    { header: '费用项目', value: (bill) => bill.feeItemName },
    { header: '应收金额', value: (bill) => bill.receivableAmount },
    { header: '已收金额', value: (bill) => bill.receivedAmount },
    { header: '待缴金额', value: (bill) => outstandingAmount(bill) },
    { header: '到期日期', value: (bill) => bill.dueDate },
    { header: '账单状态', value: (bill) => statusLabel(bill.status) },
  ])
  ElMessage.success(`已导出 ${rows.length} 笔账单。`)
}

const handleSelectionChange = (rows: BillRecord[]) => {
  selectedBills.value = rows
}

const handleBatchRemind = async () => {
  const recipients = selectedRemindableBills.value.map((bill) => ({
    billId: bill.id,
    billNo: bill.billNo,
    className: bill.className,
    dueDate: bill.dueDate,
    feeItemName: bill.feeItemName,
    outstandingAmount: outstandingAmount(bill),
    studentName: bill.studentName,
    studentNo: bill.studentNo,
  }))

  if (!recipients.length) {
    ElMessage.warning('请先选择未结清账单。')
    return
  }

  remindSending.value = true
  try {
    const result = await sendNotifyMessage({
      bizType: '账单催缴',
      channel: 'INTERNAL_MESSAGE',
      recipients,
      summary:
        recipients.length === 1
          ? `已向 ${recipients[0].studentName} 发送 ${recipients[0].feeItemName} 缴费提醒。`
          : `已为 ${recipients.length} 笔未结清账单发送缴费提醒，请及时跟进。`,
      templateCode: 'BILL_REMINDER',
      title: '缴费催缴提醒',
    })
    ElMessage.success(`催缴任务已创建：${result.taskNo}，覆盖 ${result.recipientCount} 笔账单`)
    selectedBills.value = []
    billTableRef.value?.clearSelection()
    await appStore.refreshPortalIndicators('/dashboard')
  } catch (error) {
    showRequestError(error, '批量催缴发送失败。')
  } finally {
    remindSending.value = false
  }
}

const isBillSelectable = (bill: BillRecord) => outstandingAmount(bill) > 0

const outstandingAmount = (bill: BillRecord) => bill.receivableAmount - bill.receivedAmount

const statusLabel = (status: string) => statusMeta[status]?.label ?? status

const formatCurrency = (value: number) =>
  new Intl.NumberFormat('zh-CN', {
    currency: 'CNY',
    maximumFractionDigits: 0,
    style: 'currency',
  }).format(value)

const isDateWithinRange = (value: string, start: string, end: string) =>
  value >= start && value <= end

function nextWeekDate() {
  const date = new Date()
  date.setDate(date.getDate() + 7)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

onMounted(() => {
  void refreshPage()
})
</script>

<style scoped>
.billing-page {
  gap: 20px;
}

.bill-metrics {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.table-cell-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.table-cell-meta .primary {
  font-weight: 600;
}

.table-cell-meta .secondary {
  color: var(--erp-color-text-tertiary);
  font-size: 12px;
}

.receipt-dialog__alert {
  margin-bottom: 16px;
}

@media (max-width: 1100px) {
  .bill-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
