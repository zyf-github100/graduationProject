import { ElMessage } from 'element-plus'

export const resolveErrorMessage = (error: unknown, fallback = '请求失败，请稍后重试。') => {
  if (error instanceof Error && error.message) {
    return error.message
  }

  return fallback
}

export const showRequestError = (error: unknown, fallback?: string) => {
  ElMessage.error(resolveErrorMessage(error, fallback))
}
