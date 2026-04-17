<template>
  <el-dialog v-model="visibleModel" title="账单详情" width="840px">
    <template v-if="!billId">
      <el-empty description="未选择账单" />
    </template>

    <template v-else-if="loading">
      <el-skeleton animated>
        <template #template>
          <el-skeleton-item variant="p" style="width: 100%; height: 140px" />
          <el-skeleton-item variant="p" style="width: 100%; height: 120px; margin-top: 16px" />
          <el-skeleton-item variant="p" style="width: 100%; height: 120px; margin-top: 16px" />
        </template>
      </el-skeleton>
    </template>

    <template v-else-if="detail">
      <div class="detail-stack">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="账单号">{{ detail.billNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <StatusTag :status="detail.status" />
          </el-descriptions-item>
          <el-descriptions-item label="学生姓名">{{ detail.studentInfo.studentName }}</el-descriptions-item>
          <el-descriptions-item label="学号">{{ detail.studentInfo.studentNo }}</el-descriptions-item>
          <el-descriptions-item label="班级">{{ detail.studentInfo.className }}</el-descriptions-item>
          <el-descriptions-item label="费用项目">{{ detail.feeItemInfo.feeItemName }}</el-descriptions-item>
          <el-descriptions-item label="应收金额">{{ formatCurrency(detail.receivableAmount) }}</el-descriptions-item>
          <el-descriptions-item label="已收金额">{{ formatCurrency(detail.receivedAmount) }}</el-descriptions-item>
          <el-descriptions-item label="待缴金额">{{ formatCurrency(detail.outstandingAmount) }}</el-descriptions-item>
          <el-descriptions-item label="到期日期">{{ detail.dueDate }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detail.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="最近更新">{{ detail.updatedAt }}</el-descriptions-item>
        </el-descriptions>

        <section class="detail-block">
          <div class="detail-block__title">账单明细</div>
          <el-table :data="detail.billDetails" border stripe>
            <el-table-column prop="itemName" label="项目" min-width="180" />
            <el-table-column label="金额" width="140">
              <template #default="{ row }">
                {{ formatCurrency(row.amount) }}
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="说明" min-width="220" />
          </el-table>
        </section>

        <section class="detail-block">
          <div class="detail-block__title">收款记录</div>
          <el-table :data="detail.receipts" border stripe empty-text="暂无收款记录">
            <el-table-column prop="receiptNo" label="回单号" min-width="160" />
            <el-table-column label="金额" width="120">
              <template #default="{ row }">
                {{ formatCurrency(row.receiptAmount) }}
              </template>
            </el-table-column>
            <el-table-column prop="paymentChannel" label="渠道" width="140" />
            <el-table-column prop="sourceType" label="来源" width="140" />
            <el-table-column prop="paymentTime" label="支付时间" min-width="160" />
          </el-table>
        </section>

        <section class="detail-block">
          <div class="detail-block__title">状态流转</div>
          <el-timeline>
            <el-timeline-item
              v-for="item in detail.statusTimeline"
              :key="item.time + item.content"
              :timestamp="item.time"
              :type="item.type"
            >
              {{ item.content }}
            </el-timeline-item>
          </el-timeline>
        </section>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { fetchBillDetail, type BillDetailData } from '../../api/erp'
import { showRequestError } from '../../lib/feedback'
import StatusTag from '../common/StatusTag.vue'

interface Props {
  billId: number | null
  modelValue: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const loading = ref(false)
const detail = ref<BillDetailData | null>(null)

const visibleModel = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const loadDetail = async () => {
  if (!props.billId || !props.modelValue) {
    return
  }

  loading.value = true
  try {
    detail.value = await fetchBillDetail(props.billId)
  } catch (error) {
    showRequestError(error, '账单详情加载失败。')
    visibleModel.value = false
  } finally {
    loading.value = false
  }
}

const formatCurrency = (value: number) =>
  new Intl.NumberFormat('zh-CN', {
    currency: 'CNY',
    maximumFractionDigits: 0,
    style: 'currency',
  }).format(value)

watch(
  () => [props.billId, props.modelValue],
  () => {
    if (!props.modelValue) {
      detail.value = null
      return
    }
    void loadDetail()
  },
  { immediate: true },
)
</script>

<style scoped>
.detail-stack {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.detail-block__title {
  margin-bottom: 12px;
  font-size: 15px;
  font-weight: 600;
}
</style>
