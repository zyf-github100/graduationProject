export const statusMeta: Record<
  string,
  { label: string; type: 'primary' | 'success' | 'info' | 'warning' | 'danger' }
> = {
  ACTIVE: { label: '在读', type: 'success' },
  LEAVE: { label: '请假中', type: 'warning' },
  SUSPENDED: { label: '休学', type: 'danger' },
  APPROVING: { label: '审批中', type: 'warning' },
  TODO: { label: '待处理', type: 'warning' },
  DRAFT: { label: '草稿', type: 'info' },
  PUBLISHED: { label: '已发布', type: 'success' },
  PAID: { label: '已缴清', type: 'success' },
  PARTIAL_PAID: { label: '部分已缴', type: 'warning' },
  OVERDUE: { label: '已逾期', type: 'danger' },
  PENDING: { label: '待缴费', type: 'info' },
  WARNING: { label: '待跟进', type: 'warning' },
  DANGER: { label: '高风险', type: 'danger' },
  INFO: { label: '提醒', type: 'info' },
  NORMAL: { label: '正常', type: 'success' },
  LATE: { label: '迟到', type: 'warning' },
}
