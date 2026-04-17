import { request } from '../lib/http'
import type {
  ApprovalTask,
  BillRecord,
  CurrentUser,
  DashboardMetric,
  LabeledValue,
  LoginResponse,
  MenuItem,
  PageResult,
  PermissionResponse,
  SelectOption,
  StudentCourseSlot,
  StudentNotice,
  StudentRecord,
  StudentScoreRecord,
  StudentTodayCourse,
  TimelineItem,
} from '../types'

export interface ReminderItem {
  meta: string
  title: string
}

export interface ActivityItem {
  actor: string
  content: string
  time: string
}

export interface StudentHomeData {
  latestNotices: StudentNotice[]
  metrics: DashboardMetric[]
  timeline: ActivityItem[]
  todayCourses: StudentTodayCourse[]
  upcomingItems: ReminderItem[]
}

export interface StudentScheduleData {
  summary: LabeledValue[]
  todayCourses: StudentTodayCourse[]
  upcomingItems: ReminderItem[]
  weeklySchedule: StudentCourseSlot[]
}

export interface StudentScoresData {
  analysis: LabeledValue[]
  metrics: DashboardMetric[]
  records: StudentScoreRecord[]
}

export interface StudentBillingOverview {
  bills: BillRecord[]
  pendingBill: BillRecord | null
  summary: DashboardMetric[]
}

export interface BillDetailData extends BillRecord {
  billDetails: Array<{
    amount: number
    itemName: string
    remark: string
  }>
  createdAt: string
  feeItemInfo: {
    dueDate: string
    feeItemName: string
    receivableAmount: number
  }
  outstandingAmount: number
  receipts: Array<{
    billId: number
    paymentChannel: string
    paymentTime: string
    receiptAmount: number
    receiptId: number
    receiptNo: string
    sourceType: string
  }>
  statusTimeline: Array<{
    content: string
    time: string
    type: 'primary' | 'success' | 'warning' | 'danger' | 'info'
  }>
  studentInfo: {
    className: string
    studentName: string
    studentNo: string
  }
  studentNo: string
  updatedAt: string
}

export interface GenerateBillPayload {
  className: string
  dueDate: string
  feeItemName: string
  receivableAmount: number
  studentName: string
  studentNo: string
}

export interface GenerateBillResult {
  billId: number
  billNo: string
  taskId: number
  taskStatus: string
}

export interface ReceiptCreatePayload {
  billId: number
  paymentChannel: string
  paymentTime?: string
  receiptAmount: number
  sourceType: string
}

export interface ReceiptCreateResult {
  billId: number
  billStatus: string
  outstandingAmount: number
  receiptId: number
  receiptNo: string
  receivedAmount: number
}

export interface NoticeCategorySummary {
  category: string
  label: string
}

export interface StudentNoticeCenterData {
  categorySummary: NoticeCategorySummary[]
  records: StudentNotice[]
  timeline: ActivityItem[]
  unreadCount: number
}

export interface TeacherNoticeCenterData {
  categorySummary: NoticeCategorySummary[]
  records: StudentNotice[]
  unreadCount: number
}

export interface WorkflowInboxItem {
  bizId: string
  bizType: string
  eventType: string
  id: string
  isRead: boolean
  occurredAt: string
  priority: 'high' | 'normal'
  routingKey: string
  sourceService: string
  summary: string
  title: string
  workflowStatus: string
}

export interface NotifyMessageRecipientPayload {
  billId: number
  billNo: string
  className: string
  dueDate: string
  feeItemName: string
  outstandingAmount: number
  studentName: string
  studentNo: string
}

export interface SendNotifyMessagePayload {
  bizId?: string
  bizType?: string
  channel: string
  recipients: NotifyMessageRecipientPayload[]
  summary: string
  templateCode: string
  title: string
}

export interface NotifyMessageTask {
  bizId: string
  bizType: string
  channel: string
  createdAt: string
  failedCount: number
  recipientCount: number
  status: string
  successCount: number
  summary: string
  taskId: string
  taskNo: string
  templateCode: string
  title: string
  updatedAt: string
}

export interface NotifyMessageRecipient {
  billId: number
  billNo: string
  channel: string
  className: string
  dueDate: string
  feeItemName: string
  outstandingAmount: number
  recipientId: string
  recipientName: string
  sentAt: string
  status: string
  studentNo: string
  taskId: string
}

export interface StudentProfileData {
  baseInfo: LabeledValue[]
  contacts: LabeledValue[]
  preferences: {
    billing: boolean
    campus: boolean
    scores: boolean
  }
  schoolInfo: LabeledValue[]
  security: LabeledValue[]
}

export interface TeacherCourse {
  className: string
  courseName: string
  id: number
  location: string
  status: string
  time: string
}

export interface TeacherTaskItem {
  deadline: string
  status: string
  title: string
}

export interface TeacherHomeData {
  latestNotices: StudentNotice[]
  metrics: DashboardMetric[]
  tasks: TeacherTaskItem[]
  todayCourses: TeacherCourse[]
}

export interface TeacherScheduleData {
  summary: {
    homeroomLessons: string
    weeklyLessons: string
  }
  tips: string[]
  todayCourses: TeacherCourse[]
  weeklySchedule: StudentCourseSlot[]
}

export interface TeacherClassSummary {
  attendanceRate: string
  averageScore: string
  className: string
  pendingItems: string
  role: string
  studentCount: number
}

export interface TeacherRosterRecord {
  attendance: string
  className: string
  latestHomework: string
  score: string
  studentName: string
  studentNo: string
}

export interface TeacherClassesData {
  classes: TeacherClassSummary[]
  roster: TeacherRosterRecord[]
}

export interface TeacherAttendanceRecord {
  attendance: string
  checkInTime: string
  className: string
  remark: string
  studentName: string
  studentNo: string
}

export interface TeacherAttendanceData {
  classOptions: SelectOption[]
  filters: {
    className: string
    date: string
    period: string
  }
  periodOptions: SelectOption[]
  records: TeacherAttendanceRecord[]
}

export interface TeacherGradeTask {
  className: string
  deadline: string
  status: string
  taskName: string
  teacherName?: string
}

export interface TeacherGradeRecord {
  className: string
  rank: string
  score: number
  status: string
  studentName: string
  studentNo: string
}

export interface TeacherGradesData {
  records: TeacherGradeRecord[]
  summary: LabeledValue[]
  tasks: TeacherGradeTask[]
  tips: string[]
}

export interface TeacherProfileData {
  baseInfo: LabeledValue[]
  security: LabeledValue[]
  teachingInfo: LabeledValue[]
}

export interface StudentSummaryData {
  currentStudents: number
  incompleteGuardianProfiles: number
  onLeaveStudents: number
  weeklyNewRecords: number
}

export interface StudentOptionsData {
  campuses: string[]
  genders: string[]
  grades: string[]
  statuses: SelectOption[]
}

export interface StudentContact {
  label: string
  name: string
  phone: string
  relation: string
}

export interface StudentLog {
  actor: string
  content: string
  time: string
}

export interface StudentDetailData extends StudentRecord {
  address: string
  campus: string
  classTeacher: string
  contacts: StudentContact[]
  dormitory: string
  idCardMasked: string
  logs: StudentLog[]
  remark: string
}

export interface StudentSavePayload {
  address: string
  admissionDate: string
  campus: string
  className: string
  classTeacher: string
  contacts?: StudentContact[]
  dormitory: string
  gender: string
  gradeName: string
  guardianName: string
  guardianPhone: string
  idCardMasked: string
  remark: string
  status: string
  studentName: string
  studentNo: string
}

export interface AcademicOverviewData {
  gradeTasks: TeacherGradeTask[]
  rosterPreview: Array<{
    attendance: string
    className: string
    score: string
    status: string
    studentName: string
    studentNo: string
  }>
  summary: LabeledValue[]
  timetablePreview: Array<{
    friday: string
    monday: string
    period: string
    thursday: string
    tuesday: string
    wednesday: string
  }>
}

export interface WorkflowTaskQuery {
  applicant?: string
  bizType?: string
  node?: string
  pageNo?: number
  pageSize?: number
}

export interface WorkflowTodoItem {
  bizType: string
  createdAt: string
  currentNode: string
  dueAt: string
  processId: number
  status: string
  taskId: number
  title: string
}

export interface AdminProfileSecurityItem extends LabeledValue {}

export const login = (payload: {
  clientType: string
  password: string
  username: string
}) =>
  request<LoginResponse>('/api/v1/auth/login', {
    body: JSON.stringify(payload),
    method: 'POST',
    withAuth: false,
  })

export const logout = () =>
  request<{ success: boolean }>('/api/v1/auth/logout', {
    method: 'POST',
  })

export const me = () => request<CurrentUser>('/api/v1/auth/me')

export const myMenus = () => request<MenuItem[]>('/api/v1/auth/me/menus')

export const fetchPermissionProfile = () =>
  request<PermissionResponse>('/api/v1/auth/me/permissions')

export const fetchStudentHome = () => request<StudentHomeData>('/api/v1/academic/student/home')

export const fetchStudentSchedule = () =>
  request<StudentScheduleData>('/api/v1/academic/student/schedule')

export const fetchStudentScores = () =>
  request<StudentScoresData>('/api/v1/academic/student/scores')

export const fetchStudentBillingOverview = () =>
  request<StudentBillingOverview>('/api/v1/billing/student/overview')

export const fetchStudentNotices = () =>
  request<StudentNoticeCenterData>('/api/v1/notify/student/notices')

export const fetchStudentProfile = (studentNo: string) =>
  request<StudentProfileData>('/api/v1/master/student/profile', {
    query: { studentNo },
  })

export const fetchTeacherHome = () => request<TeacherHomeData>('/api/v1/academic/teacher/home')

export const fetchTeacherSchedule = () =>
  request<TeacherScheduleData>('/api/v1/academic/teacher/schedule')

export const fetchTeacherClasses = () =>
  request<TeacherClassesData>('/api/v1/academic/teacher/classes')

export const fetchTeacherAttendance = () =>
  request<TeacherAttendanceData>('/api/v1/academic/teacher/attendance')

export const fetchTeacherGrades = () =>
  request<TeacherGradesData>('/api/v1/academic/teacher/grades')

export const fetchTeacherNotices = () =>
  request<TeacherNoticeCenterData>('/api/v1/notify/teacher/notices')

export const fetchWorkflowInbox = () =>
  request<WorkflowInboxItem[]>('/api/v1/notify/tasks/inbox')

export const markWorkflowInboxRead = (messageId: string) =>
  request<WorkflowInboxItem>(`/api/v1/notify/tasks/inbox/${messageId}/read`, {
    method: 'POST',
  })

export const sendNotifyMessage = (payload: SendNotifyMessagePayload) =>
  request<NotifyMessageTask>('/api/v1/notify/messages/send', {
    body: JSON.stringify(payload),
    method: 'POST',
  })

export const fetchNotifyMessageTask = (taskId: string) =>
  request<NotifyMessageTask>(`/api/v1/notify/messages/${taskId}`)

export const fetchNotifyMessageRecipients = (taskId: string) =>
  request<NotifyMessageRecipient[]>(`/api/v1/notify/messages/${taskId}/recipients`)

export const fetchTeacherProfile = (teacherNo?: string) =>
  request<TeacherProfileData>('/api/v1/master/teacher/profile', {
    query: { teacherNo },
  })

export const fetchStudentSummary = () =>
  request<StudentSummaryData>('/api/v1/master/students/summary')

export const fetchStudentOptions = () =>
  request<StudentOptionsData>('/api/v1/master/students/options')

export const fetchStudents = (query: {
  grade?: string
  keyword?: string
  pageNo?: number
  pageSize?: number
  status?: string
}) =>
  request<PageResult<StudentRecord>>('/api/v1/master/students', {
    query,
  })

export const fetchStudentDetail = (studentId: number) =>
  request<StudentDetailData>(`/api/v1/master/students/${studentId}`)

export const createStudent = (payload: StudentSavePayload) =>
  request<StudentDetailData>('/api/v1/master/students', {
    body: JSON.stringify(payload),
    method: 'POST',
  })

export const updateStudent = (studentId: number, payload: StudentSavePayload) =>
  request<StudentDetailData>(`/api/v1/master/students/${studentId}`, {
    body: JSON.stringify(payload),
    method: 'PUT',
  })

export const fetchAcademicOverview = () =>
  request<AcademicOverviewData>('/api/v1/academic/overview')

export const fetchBillingSummary = () =>
  request<DashboardMetric[]>('/api/v1/billing/bills/summary')

export const fetchBillDetail = (billId: number) =>
  request<BillDetailData>(`/api/v1/billing/bills/${billId}`)

export const fetchBills = (query: {
  keyword?: string
  pageNo?: number
  pageSize?: number
  status?: string
}) =>
  request<PageResult<BillRecord>>('/api/v1/billing/bills', {
    query,
  })

export const generateBill = (payload: GenerateBillPayload) =>
  request<GenerateBillResult>('/api/v1/billing/bills/generate', {
    body: JSON.stringify(payload),
    method: 'POST',
  })

export const createReceipt = (payload: ReceiptCreatePayload) =>
  request<ReceiptCreateResult>('/api/v1/billing/receipts', {
    body: JSON.stringify(payload),
    method: 'POST',
  })

export const fetchWorkflowTasks = (query: WorkflowTaskQuery) =>
  request<PageResult<ApprovalTask>>('/api/v1/workflow/tasks', {
    query: query as Record<string, unknown>,
  })

export const fetchWorkflowTodo = (query: { bizType?: string; pageNo?: number; pageSize?: number }) =>
  request<PageResult<WorkflowTodoItem>>('/api/v1/workflow/tasks/todo', {
    query,
  })

export const fetchWorkflowTaskDetail = (taskId: number) =>
  request<ApprovalTask>(`/api/v1/workflow/tasks/${taskId}`)

export const fetchWorkflowTimeline = (taskId: number) =>
  request<TimelineItem[]>(`/api/v1/workflow/tasks/${taskId}/timeline`)

export const approveWorkflowTask = (taskId: number, payload: {
  action: 'APPROVE' | 'REJECT' | 'TRANSFER'
  assignee?: string
  opinion?: string
}) =>
  request<ApprovalTask>(`/api/v1/workflow/tasks/${taskId}/approve`, {
    body: JSON.stringify(payload),
    method: 'POST',
  })
