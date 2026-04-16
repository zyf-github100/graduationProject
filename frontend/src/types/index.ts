export interface MenuItem {
  children?: MenuItem[]
  component: string
  icon?: string | null
  menuId: number
  menuName: string
  path: string
}

export interface CurrentUser {
  displayName: string
  orgUnit: string
  orgUnitId: number
  roles: string[]
  userId: number
  userType: string
  username: string
}

export interface LoginResponse {
  accessToken: string
  displayName: string
  expiresIn: number
  refreshToken: string
  userId: number
  userType: string
}

export interface PermissionResponse {
  dataScopes: string[]
  exportPermissions: string[]
  permissions: string[]
}

export interface PageResult<T> {
  pageNo: number
  pageSize: number
  records: T[]
  total: number
  totalPages: number
}

export interface LabeledValue {
  label: string
  value: string
}

export interface SelectOption {
  label: string
  value: string
}

export interface DashboardMetric {
  caption: string
  direction?: 'up' | 'down' | 'flat'
  series?: number[]
  title: string
  tone: 'primary' | 'success' | 'warning' | 'danger'
  trend: string
  value: string
}

export interface StudentRecord {
  admissionDate: string
  className: string
  gender: string
  gradeName: string
  guardianName: string
  guardianPhone: string
  id: number
  status: string
  studentName: string
  studentNo: string
}

export interface ApprovalTask {
  applicantName: string
  applicantRole: string
  bizType: string
  className: string
  currentNode: string
  duration: string
  id: number
  processNo: string
  reason: string
  status: string
  submittedAt: string
}

export interface TimelineItem {
  actor: string
  description: string
  time: string
  title: string
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info'
}

export interface BillRecord {
  billNo: string
  className: string
  dueDate: string
  feeItemName: string
  id: number
  receivableAmount: number
  receivedAmount: number
  status: string
  studentName: string
}

export interface StudentCourseSlot {
  friday: string
  monday: string
  period: string
  thursday: string
  time: string
  tuesday: string
  wednesday: string
}

export interface StudentTodayCourse {
  courseName: string
  id: number
  location: string
  status: string
  teacherName: string
  time: string
}

export interface StudentScoreRecord {
  comment: string
  continuousScore: number
  finalScore: number
  midtermScore: number
  rank: string
  status: string
  subject: string
  teacherName: string
  totalScore: number
}

export interface StudentNotice {
  category: string
  id: number
  isRead: boolean
  priority: 'high' | 'normal'
  publishTime: string
  publisher: string
  summary: string
  title: string
}
