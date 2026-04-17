import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  fetchPermissionProfile,
  fetchStudentNotices,
  fetchTeacherNotices,
  fetchWorkflowInbox,
  login as loginRequest,
  logout as logoutRequest,
  me,
  myMenus,
} from '../api/erp'
import { clearStoredSession, getStoredAccessToken, storeAuthSession } from '../lib/http'
import type { CurrentUser, MenuItem, PermissionResponse } from '../types'

const DEFAULT_TERM = '2025-2026 学年第二学期'
const DEFAULT_CAMPUS = '主校区'
const TERM_STORAGE_KEY = 'school-erp.term'
const CAMPUS_STORAGE_KEY = 'school-erp.campus'

const loadStoredValue = (key: string, fallback: string) => {
  if (typeof window === 'undefined') {
    return fallback
  }

  return window.localStorage.getItem(key) ?? fallback
}

const saveStoredValue = (key: string, value: string) => {
  if (typeof window === 'undefined') {
    return
  }

  window.localStorage.setItem(key, value)
}

export const useAppStore = defineStore('app', () => {
  const collapsed = ref(false)
  const term = ref(loadStoredValue(TERM_STORAGE_KEY, DEFAULT_TERM))
  const campus = ref(loadStoredValue(CAMPUS_STORAGE_KEY, DEFAULT_CAMPUS))
  const searchKeyword = ref('')
  const unreadCount = ref(0)
  const currentUser = ref<CurrentUser | null>(null)
  const menus = ref<MenuItem[]>([])
  const permissions = ref<PermissionResponse | null>(null)
  const hasSession = ref(Boolean(getStoredAccessToken()))

  let initializePromise: Promise<void> | null = null

  const homePath = computed(() => resolveHomePath(currentUser.value?.userType))

  const syncSessionState = () => {
    hasSession.value = Boolean(getStoredAccessToken())
  }

  const toggleSidebar = () => {
    collapsed.value = !collapsed.value
  }

  const setTerm = (value: string) => {
    term.value = value
    saveStoredValue(TERM_STORAGE_KEY, value)
  }

  const setCampus = (value: string) => {
    campus.value = value
    saveStoredValue(CAMPUS_STORAGE_KEY, value)
  }

  const resetSessionState = () => {
    currentUser.value = null
    menus.value = []
    permissions.value = null
    unreadCount.value = 0
  }

  const loadSessionData = async () => {
    const [user, userMenus, permissionProfile] = await Promise.all([
      me(),
      myMenus(),
      fetchPermissionProfile(),
    ])

    currentUser.value = user
    menus.value = userMenus
    permissions.value = permissionProfile
  }

  const initializeSession = async (force = false) => {
    syncSessionState()

    if (!hasSession.value) {
      resetSessionState()
      return
    }

    if (!force && currentUser.value && menus.value.length > 0) {
      return
    }

    if (initializePromise) {
      return initializePromise
    }

    initializePromise = loadSessionData().finally(() => {
      initializePromise = null
    })

    return initializePromise
  }

  const refreshPortalIndicators = async (_path: string) => {
    if (!hasSession.value) {
      unreadCount.value = 0
      return
    }

    if (currentUser.value?.userType === 'STUDENT') {
      unreadCount.value = (await fetchStudentNotices()).unreadCount
      return
    }

    if (currentUser.value?.userType === 'TEACHER') {
      unreadCount.value = (await fetchTeacherNotices()).unreadCount
      return
    }

    unreadCount.value = (await fetchWorkflowInbox()).filter((item) => !item.isRead).length
  }

  const login = async (payload: {
    username: string
    password: string
    clientType: string
    remember: boolean
  }) => {
    const session = await loginRequest({
      username: payload.username,
      password: payload.password,
      clientType: payload.clientType,
    })

    storeAuthSession(session, payload.remember)
    syncSessionState()
    await initializeSession(true)
    return currentUser.value
  }

  const logout = async () => {
    try {
      if (hasSession.value) {
        await logoutRequest()
      }
    } finally {
      clearStoredSession()
      syncSessionState()
      resetSessionState()
    }
  }

  const clearSession = () => {
    clearStoredSession()
    syncSessionState()
    resetSessionState()
  }

  return {
    campus,
    clearSession,
    collapsed,
    currentUser,
    hasSession,
    homePath,
    initializeSession,
    login,
    logout,
    menus,
    permissions,
    refreshPortalIndicators,
    searchKeyword,
    setCampus,
    setTerm,
    term,
    toggleSidebar,
    unreadCount,
  }
})

function resolveHomePath(userType?: string) {
  switch (userType) {
    case 'STUDENT':
      return '/student/home'
    case 'TEACHER':
      return '/teacher/home'
    default:
      return '/dashboard'
  }
}
