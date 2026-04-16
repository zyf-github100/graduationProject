import { createRouter, createWebHistory } from 'vue-router'
import { pinia } from '../stores/pinia'
import { useAppStore } from '../stores/app'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../pages/LoginPage.vue'),
      meta: { title: '登录' },
    },
    {
      path: '/',
      component: () => import('../layouts/AppShell.vue'),
      children: [
        {
          path: '',
          redirect: '/student/home',
        },
        {
          path: 'student/home',
          name: 'student-home',
          component: () => import('../pages/StudentHomePage.vue'),
          meta: { title: '学生首页' },
        },
        {
          path: 'student/schedule',
          name: 'student-schedule',
          component: () => import('../pages/StudentSchedulePage.vue'),
          meta: { title: '我的课表' },
        },
        {
          path: 'student/scores',
          name: 'student-scores',
          component: () => import('../pages/StudentScoresPage.vue'),
          meta: { title: '我的成绩' },
        },
        {
          path: 'student/payments',
          name: 'student-payments',
          component: () => import('../pages/StudentPaymentPage.vue'),
          meta: { title: '缴费中心' },
        },
        {
          path: 'student/notices',
          name: 'student-notices',
          component: () => import('../pages/StudentNoticePage.vue'),
          meta: { title: '通知公告' },
        },
        {
          path: 'student/profile',
          name: 'student-profile',
          component: () => import('../pages/StudentProfilePage.vue'),
          meta: { title: '个人信息' },
        },
        {
          path: 'teacher/home',
          name: 'teacher-home',
          component: () => import('../pages/TeacherHomePage.vue'),
          meta: { title: '教师首页' },
        },
        {
          path: 'teacher/schedule',
          name: 'teacher-schedule',
          component: () => import('../pages/TeacherSchedulePage.vue'),
          meta: { title: '我的课表' },
        },
        {
          path: 'teacher/classes',
          name: 'teacher-classes',
          component: () => import('../pages/TeacherClassesPage.vue'),
          meta: { title: '授课班级' },
        },
        {
          path: 'teacher/attendance',
          name: 'teacher-attendance',
          component: () => import('../pages/TeacherAttendancePage.vue'),
          meta: { title: '考勤登记' },
        },
        {
          path: 'teacher/grades',
          name: 'teacher-grades',
          component: () => import('../pages/TeacherGradesPage.vue'),
          meta: { title: '成绩录入' },
        },
        {
          path: 'teacher/notices',
          name: 'teacher-notices',
          component: () => import('../pages/TeacherNoticePage.vue'),
          meta: { title: '通知中心' },
        },
        {
          path: 'teacher/profile',
          name: 'teacher-profile',
          component: () => import('../pages/TeacherProfilePage.vue'),
          meta: { title: '个人信息' },
        },
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('../pages/DashboardPage.vue'),
          meta: { title: '系统首页' },
        },
        {
          path: 'students',
          name: 'students',
          component: () => import('../pages/StudentListPage.vue'),
          meta: { title: '学生档案' },
        },
        {
          path: 'students/new',
          name: 'student-create',
          component: () => import('../pages/StudentFormPage.vue'),
          meta: { title: '新建学生档案' },
        },
        {
          path: 'students/:id',
          name: 'student-detail',
          component: () => import('../pages/StudentDetailPage.vue'),
          meta: { title: '学生详情' },
        },
        {
          path: 'students/:id/edit',
          name: 'student-edit',
          component: () => import('../pages/StudentFormPage.vue'),
          meta: { title: '编辑学生档案' },
        },
        {
          path: 'approvals',
          name: 'approvals',
          component: () => import('../pages/ApprovalPage.vue'),
          meta: { title: '审批中心' },
        },
        {
          path: 'academic',
          name: 'academic',
          component: () => import('../pages/AcademicManagementPage.vue'),
          meta: { title: '教务管理' },
        },
        {
          path: 'billing/bills',
          name: 'billing-bills',
          component: () => import('../pages/BillingPage.vue'),
          meta: { title: '账单管理' },
        },
        {
          path: 'settings/profile',
          name: 'profile',
          component: () => import('../pages/ProfilePage.vue'),
          meta: { title: '个人中心' },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('../pages/NotFoundPage.vue'),
      meta: { title: '页面未找到' },
    },
  ],
})

router.beforeEach(async (to) => {
  const store = useAppStore(pinia)
  const requiresAuth = to.name !== 'login'

  if (to.name === 'login') {
    if (!store.hasSession) {
      return true
    }

    try {
      await store.initializeSession()
      return store.homePath
    } catch {
      store.clearSession()
      return true
    }
  }

  if (!requiresAuth) {
    return true
  }

  if (!store.hasSession) {
    return {
      name: 'login',
      query: { redirect: to.fullPath },
    }
  }

  try {
    await store.initializeSession()
  } catch {
    store.clearSession()
    return {
      name: 'login',
      query: { redirect: to.fullPath },
    }
  }

  const userType = store.currentUser?.userType
  if (userType === 'STUDENT' && !to.path.startsWith('/student')) {
    return store.homePath
  }

  if (userType === 'TEACHER' && !to.path.startsWith('/teacher')) {
    return store.homePath
  }

  if (userType === 'SCHOOL_ADMIN' && (to.path.startsWith('/student') || to.path.startsWith('/teacher'))) {
    return store.homePath
  }

  return true
})

router.afterEach(async (to) => {
  const pageTitle = typeof to.meta.title === 'string' ? to.meta.title : '广州软件学院智慧校园平台'
  document.title = `${pageTitle} - 广州软件学院智慧校园平台`

  if (to.name !== 'login') {
    const store = useAppStore(pinia)
    if (store.hasSession) {
      try {
        await store.refreshPortalIndicators(to.path)
      } catch {
        // Ignore header badge refresh errors and let page-level requests show details.
      }
    }
  }
})

export default router
