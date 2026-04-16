<template>
  <div class="login-page">
    <div class="login-page__glow login-page__glow--emerald" />
    <div class="login-page__glow login-page__glow--cyan" />
    <div class="login-page__orbital login-page__orbital--one" />
    <div class="login-page__orbital login-page__orbital--two" />
    <div class="login-page__orbital login-page__orbital--three" />

    <div
      v-for="bubble in bubbles"
      :key="bubble.className"
      :class="['login-page__bubble', bubble.className]"
    />

    <main class="login-card">
      <div class="login-card__sheen" />

      <header class="login-card__header">
        <div class="login-card__brand">
          <span class="login-card__brand-mark">
            <span />
          </span>
          <span>广州软件学院</span>
        </div>
        <h1>欢迎登录</h1>
      </header>

      <div class="login-role-switch">
        <button
          type="button"
          class="login-role-switch__item"
          :class="{ 'login-role-switch__item--active': form.role === 'student' }"
          @click="switchRole('student')"
        >
          学生
        </button>
        <button
          type="button"
          class="login-role-switch__item"
          :class="{ 'login-role-switch__item--active': form.role === 'teacher' }"
          @click="switchRole('teacher')"
        >
          教师
        </button>
      </div>

      <el-form class="login-form" @submit.prevent="handleSubmit">
        <el-form-item>
          <el-input
            v-model="form.username"
            autocomplete="username"
            clearable
            size="large"
            placeholder="请输入账号"
          />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.password"
            autocomplete="current-password"
            show-password
            size="large"
            type="password"
            placeholder="请输入密码"
          />
        </el-form-item>

        <div class="login-form__meta">
          <label class="login-form__remember">
            <input v-model="form.remember" type="checkbox" />
            <span>记住账号</span>
          </label>
          <span class="login-form__status">安全登录</span>
        </div>

        <div class="login-form__hint">
          <span>学生演示：`202501001 / 123456`</span>
          <span>教师演示：`teacher.chen / 123456`</span>
        </div>

        <el-button
          class="login-form__submit"
          native-type="submit"
          size="large"
          :loading="submitting"
        >
          登录系统
        </el-button>
      </el-form>

      <footer class="login-card__footer">
        <button type="button" @click="handleAssist('forgot')">忘记密码？</button>
        <button type="button" @click="handleAssist('support')">联系支持</button>
      </footer>
    </main>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { showRequestError } from '../lib/feedback'
import { useAppStore } from '../stores/app'

const router = useRouter()
const route = useRoute()
const store = useAppStore()
const submitting = ref(false)

const bubbles = [
  { className: 'login-page__bubble--top-right' },
  { className: 'login-page__bubble--mid-right' },
  { className: 'login-page__bubble--left' },
  { className: 'login-page__bubble--bottom-left' },
  { className: 'login-page__bubble--bottom-right' },
]

const defaultCredentials: Record<'student' | 'teacher', { username: string; password: string }> = {
  student: {
    username: '202501001',
    password: '123456',
  },
  teacher: {
    username: 'teacher.chen',
    password: '123456',
  },
}

const form = reactive({
  username: defaultCredentials.student.username,
  password: defaultCredentials.student.password,
  remember: true,
  role: 'student' as 'student' | 'teacher',
})

const switchRole = (role: 'student' | 'teacher') => {
  form.role = role
  form.username = defaultCredentials[role].username
  form.password = defaultCredentials[role].password
}

const resolveClientType = (role: 'student' | 'teacher') => {
  return role === 'teacher' ? 'TEACHER_WEB' : 'STUDENT_APP'
}

const resolveRedirect = (userType?: string) => {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
  if (!redirect) {
    return store.homePath
  }

  if (userType === 'STUDENT' && redirect.startsWith('/student')) {
    return redirect
  }

  if (userType === 'TEACHER' && redirect.startsWith('/teacher')) {
    return redirect
  }

  if (userType === 'SCHOOL_ADMIN' && !redirect.startsWith('/student') && !redirect.startsWith('/teacher')) {
    return redirect
  }

  return store.homePath
}

const handleSubmit = async () => {
  submitting.value = true

  try {
    const user = await store.login({
      username: form.username.trim(),
      password: form.password,
      clientType: resolveClientType(form.role),
      remember: form.remember,
    })

    await store.refreshPortalIndicators(store.homePath)
    ElMessage.success('登录成功')
    router.push(resolveRedirect(user?.userType))
  } catch (error) {
    showRequestError(error, '登录失败，请检查账号和密码。')
  } finally {
    submitting.value = false
  }
}

const handleAssist = (type: 'forgot' | 'support') => {
  const message =
    type === 'forgot'
      ? '请联系广州软件学院信息服务中心处理账号或密码问题。'
      : '请联系系统管理员或广州软件学院信息服务中心获取支持。'

  ElMessage.info(message)
}
</script>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  display: grid;
  place-items: center;
  padding: 32px 24px;
  font-family: 'Inter', 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  background:
    radial-gradient(circle at 18% 78%, rgba(22, 145, 117, 0.26), transparent 24%),
    radial-gradient(circle at 80% 10%, rgba(41, 154, 205, 0.2), transparent 20%),
    linear-gradient(125deg, #020615 0%, #051324 30%, #072231 58%, #03101d 100%);
}

.login-page::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(120deg, transparent 12%, rgba(97, 187, 255, 0.08) 44%, transparent 58%),
    linear-gradient(145deg, transparent 36%, rgba(78, 194, 134, 0.09) 68%, transparent 72%);
  opacity: 0.75;
  pointer-events: none;
}

.login-page::after {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at center, rgba(255, 255, 255, 0.04), transparent 52%),
    linear-gradient(180deg, rgba(3, 10, 22, 0.08), rgba(3, 10, 22, 0.3));
  pointer-events: none;
}

.login-page__glow {
  position: absolute;
  border-radius: 999px;
  filter: blur(100px);
  opacity: 0.34;
  pointer-events: none;
}

.login-page__glow--emerald {
  left: 6%;
  bottom: 4%;
  width: 360px;
  height: 240px;
  background: rgba(40, 190, 123, 0.42);
}

.login-page__glow--cyan {
  right: 7%;
  top: 6%;
  width: 320px;
  height: 220px;
  background: rgba(61, 175, 227, 0.35);
}

.login-page__orbital {
  position: absolute;
  border: 1px solid rgba(155, 212, 255, 0.16);
  border-radius: 999px;
  pointer-events: none;
}

.login-page__orbital--one {
  width: 1040px;
  height: 760px;
  transform: rotate(18deg);
}

.login-page__orbital--two {
  width: 860px;
  height: 1120px;
  transform: rotate(-28deg);
  border-color: rgba(122, 214, 179, 0.18);
}

.login-page__orbital--three {
  width: 1280px;
  height: 540px;
  transform: rotate(-11deg);
  border-color: rgba(90, 160, 224, 0.14);
}

.login-page__bubble {
  position: absolute;
  border-radius: 50%;
  background:
    radial-gradient(circle at 32% 30%, rgba(255, 255, 255, 0.72), rgba(255, 255, 255, 0.14) 38%, transparent 58%),
    radial-gradient(circle at 72% 70%, rgba(93, 200, 255, 0.5), transparent 48%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.26), rgba(255, 255, 255, 0.06));
  border: 1px solid rgba(255, 255, 255, 0.18);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.36),
    0 18px 36px rgba(5, 13, 28, 0.26);
  backdrop-filter: blur(10px);
  pointer-events: none;
}

.login-page__bubble--top-right {
  top: 19%;
  left: calc(50% + 180px);
  width: 88px;
  height: 88px;
}

.login-page__bubble--mid-right {
  top: 48%;
  right: 12%;
  width: 62px;
  height: 62px;
}

.login-page__bubble--left {
  top: 29%;
  left: 12%;
  width: 58px;
  height: 58px;
}

.login-page__bubble--bottom-left {
  left: calc(50% - 156px);
  bottom: 20%;
  width: 92px;
  height: 92px;
}

.login-page__bubble--bottom-right {
  right: 16%;
  bottom: 17%;
  width: 54px;
  height: 54px;
}

.login-card {
  position: relative;
  z-index: 1;
  width: min(100%, 442px);
  padding: 30px 30px 26px;
  border-radius: 24px;
  background:
    linear-gradient(180deg, rgba(204, 220, 230, 0.18), rgba(125, 149, 167, 0.08)),
    rgba(18, 31, 48, 0.26);
  border: 1px solid rgba(255, 255, 255, 0.28);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.2),
    0 28px 54px rgba(2, 7, 19, 0.44);
  backdrop-filter: blur(22px);
}

.login-card__sheen {
  position: absolute;
  inset: 12px;
  border-radius: 20px;
  background:
    radial-gradient(circle at 16% 82%, rgba(188, 255, 230, 0.28), transparent 24%),
    radial-gradient(circle at 80% 18%, rgba(202, 239, 255, 0.26), transparent 18%),
    radial-gradient(circle at center, rgba(255, 255, 255, 0.08), transparent 48%);
  filter: blur(8px);
  opacity: 0.95;
  pointer-events: none;
}

.login-card__header {
  position: relative;
}

.login-card__brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: rgba(242, 247, 250, 0.94);
  font-size: 17px;
  font-weight: 500;
  letter-spacing: 0.01em;
}

.login-card__brand-mark {
  position: relative;
  width: 22px;
  height: 22px;
  border-radius: 7px;
  border: 1px solid rgba(255, 255, 255, 0.72);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.18);
}

.login-card__brand-mark span {
  position: absolute;
  left: 5px;
  top: 4px;
  width: 10px;
  height: 12px;
  border-left: 2px solid rgba(255, 255, 255, 0.92);
  border-bottom: 2px solid rgba(255, 255, 255, 0.92);
  transform: skewY(14deg) rotate(-10deg);
}

.login-card__header h1 {
  margin: 32px 0 0;
  color: rgba(249, 251, 252, 0.98);
  font-size: clamp(38px, 4vw, 44px);
  font-weight: 500;
  letter-spacing: -0.03em;
  text-align: center;
}

.login-form {
  position: relative;
  margin-top: 24px;
}

.login-role-switch {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  padding: 8px;
  margin-top: 24px;
  border-radius: 18px;
  background: rgba(194, 210, 223, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.login-role-switch__item {
  min-height: 42px;
  border: 0;
  border-radius: 14px;
  background: transparent;
  color: rgba(232, 239, 246, 0.74);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition:
    background 180ms ease,
    color 180ms ease,
    box-shadow 180ms ease;
}

.login-role-switch__item--active {
  color: rgba(255, 255, 255, 0.96);
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.18), rgba(255, 255, 255, 0.08));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.24);
}

.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.login-form :deep(.el-input__wrapper) {
  min-height: 56px;
  padding: 0 18px;
  border-radius: 15px;
  background: rgba(194, 210, 223, 0.08);
  box-shadow:
    inset 0 0 0 1px rgba(228, 239, 245, 0.2),
    0 10px 24px rgba(4, 10, 22, 0.12);
}

.login-form :deep(.el-input__inner) {
  color: rgba(248, 251, 252, 0.94);
  font-size: 16px;
}

.login-form :deep(.el-input__inner::placeholder) {
  color: rgba(229, 238, 246, 0.56);
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow:
    inset 0 0 0 1px rgba(232, 243, 250, 0.42),
    0 0 0 4px rgba(141, 216, 255, 0.08),
    0 12px 24px rgba(4, 10, 22, 0.16);
}

.login-form :deep(.el-input__password) {
  color: rgba(239, 245, 249, 0.72);
}

.login-form__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin: 4px 0 12px;
  color: rgba(232, 239, 246, 0.72);
  font-size: 13px;
}

.login-form__hint {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 20px;
  color: rgba(229, 238, 246, 0.66);
  font-size: 12px;
}

.login-form__remember {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.login-form__remember input {
  width: 15px;
  height: 15px;
  margin: 0;
  accent-color: rgba(214, 235, 247, 0.98);
}

.login-form__status {
  color: rgba(202, 234, 217, 0.9);
}

.login-form__submit {
  width: 100%;
  min-height: 56px;
  border: 1px solid rgba(255, 255, 255, 0.26);
  border-radius: 999px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.2), rgba(193, 207, 219, 0.1)),
    rgba(214, 224, 231, 0.08);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.48),
    0 18px 34px rgba(4, 10, 22, 0.18);
  color: rgba(255, 255, 255, 0.96);
  font-size: 17px;
  font-weight: 600;
  transition:
    transform 180ms ease,
    box-shadow 180ms ease,
    background 180ms ease;
}

.login-form__submit:hover {
  transform: translateY(-1px);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.56),
    0 20px 34px rgba(4, 10, 22, 0.26);
}

.login-form__submit:active {
  transform: translateY(0);
}

.login-card__footer {
  position: relative;
  margin-top: 22px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.login-card__footer button {
  padding: 0;
  border: 0;
  background: transparent;
  color: rgba(239, 246, 250, 0.84);
  font-size: 14px;
  cursor: pointer;
  transition: color 180ms ease, opacity 180ms ease;
}

.login-card__footer button:hover {
  color: rgba(255, 255, 255, 1);
}

@media (max-width: 900px) {
  .login-page__orbital--one {
    width: 860px;
    height: 620px;
  }

  .login-page__orbital--two {
    width: 720px;
    height: 960px;
  }

  .login-page__orbital--three {
    width: 940px;
  }

  .login-page__bubble--left,
  .login-page__bubble--mid-right,
  .login-page__bubble--bottom-right {
    display: none;
  }
}

@media (max-width: 640px) {
  .login-page {
    padding: 18px;
  }

  .login-card {
    padding: 24px 20px 22px;
    border-radius: 22px;
  }

  .login-card__header h1 {
    margin-top: 26px;
    font-size: 34px;
  }

  .login-form__meta {
    flex-direction: column;
    align-items: flex-start;
  }

  .login-card__footer {
    flex-direction: column;
    align-items: flex-start;
  }

  .login-page__orbital--three,
  .login-page__bubble--top-right {
    display: none;
  }
}
</style>
