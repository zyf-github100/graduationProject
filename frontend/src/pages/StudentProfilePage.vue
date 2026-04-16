<template>
  <div class="page student-profile-page">
    <PageHeader
      title="个人信息"
      description="个人信息页以学籍、联系方式、校园身份与账号安全为核心，保持正式、稳定的学生门户风格。"
      :breadcrumbs="['学生服务', '个人信息']"
    >
      <template #actions>
        <el-button>更新联系方式</el-button>
        <el-button type="primary">提交修改申请</el-button>
      </template>
    </PageHeader>

    <div class="page-grid two-columns">
      <div class="section-stack">
        <div class="surface profile-card">
          <div class="profile-card__top">
            <el-avatar :size="76">{{ userInitial }}</el-avatar>
            <div>
              <h2>{{ currentUser.displayName }}</h2>
              <p>{{ currentUser.username }} · {{ currentUser.roles[0] }}</p>
            </div>
          </div>

          <div class="profile-card__roles">
            <el-tag v-for="role in currentUser.roles" :key="role" round>
              {{ role }}
            </el-tag>
          </div>
        </div>

        <PageSection title="基础信息" description="将学生身份信息与校内资料拆成两组字段，便于学院长期维护。">
          <div class="info-grid">
            <div v-for="item in profile.baseInfo" :key="item.label" class="info-item">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </PageSection>

        <PageSection title="校内信息" description="用于展示辅导员、学籍状态、入学信息与培养方向。">
          <div class="info-grid">
            <div v-for="item in profile.schoolInfo" :key="item.label" class="info-item">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </PageSection>
      </div>

      <div class="section-stack">
        <PageSection title="联系人信息" description="保留监护人与紧急联系人的核心信息，适合业务系统长期留档。">
          <div class="info-list">
            <div v-for="item in profile.contacts" :key="item.label" class="info-list__item">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </PageSection>

        <PageSection title="账号与校园卡" description="统一展示登录、安全和校园卡状态，不做无意义装饰信息。">
          <div class="info-list">
            <div v-for="item in profile.security" :key="item.label" class="info-list__item">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </PageSection>

        <PageSection title="通知偏好" description="学生可控制成绩、账单与校园通知的接收方式。">
          <div class="preference-list">
            <div class="preference-item">
              <div>
                <div class="preference-item__title">考试与成绩通知</div>
                <div class="preference-item__desc">接收成绩发布、考试安排和评语更新提醒。</div>
              </div>
              <el-switch v-model="preferences.scores" />
            </div>
            <div class="preference-item">
              <div>
                <div class="preference-item__title">缴费与财务通知</div>
                <div class="preference-item__desc">接收待缴提醒、到账通知与发票回单提醒。</div>
              </div>
              <el-switch v-model="preferences.billing" />
            </div>
            <div class="preference-item">
              <div>
                <div class="preference-item__title">校园公告与活动通知</div>
                <div class="preference-item__desc">接收社团活动、校园公告和院系事务提醒。</div>
              </div>
              <el-switch v-model="preferences.campus" />
            </div>
          </div>
        </PageSection>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { fetchStudentProfile, type StudentProfileData } from '../api/erp'
import PageHeader from '../components/common/PageHeader.vue'
import PageSection from '../components/common/PageSection.vue'
import { showRequestError } from '../lib/feedback'
import { useAppStore } from '../stores/app'

const store = useAppStore()

const profile = ref<StudentProfileData>({
  baseInfo: [],
  schoolInfo: [],
  contacts: [],
  security: [],
  preferences: {
    scores: true,
    billing: true,
    campus: true,
  },
})

const preferences = reactive({
  scores: true,
  billing: true,
  campus: true,
})

const currentUser = computed(() => {
  return (
    store.currentUser ?? {
      displayName: '学生',
      roles: ['未分配角色'],
      username: '',
    }
  )
})

const userInitial = computed(() => currentUser.value.displayName.slice(0, 1) || '学')

const loadData = async () => {
  if (!store.currentUser?.username) {
    return
  }

  try {
    const data = await fetchStudentProfile(store.currentUser.username)
    profile.value = data
    preferences.scores = data.preferences.scores
    preferences.billing = data.preferences.billing
    preferences.campus = data.preferences.campus
  } catch (error) {
    showRequestError(error, '学生个人信息加载失败。')
  }
}

onMounted(loadData)
</script>

<style scoped>
.student-profile-page {
  gap: 20px;
}

.profile-card {
  padding: 24px;
}

.profile-card__top {
  display: flex;
  align-items: center;
  gap: 16px;
}

.profile-card__top h2 {
  margin: 0;
  font-size: 28px;
}

.profile-card__top p {
  margin: 6px 0 0;
  color: var(--erp-color-text-secondary);
}

.profile-card__roles {
  margin-top: 20px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.info-item,
.info-list__item {
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.09), rgba(255, 255, 255, 0.04));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.info-item span,
.info-list__item span,
.preference-item__desc {
  color: var(--erp-color-text-tertiary);
  font-size: 12px;
}

.info-item strong,
.info-list__item strong,
.preference-item__title {
  margin-top: 8px;
  display: block;
  font-size: 15px;
  font-weight: 600;
}

.info-list,
.preference-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.preference-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.preference-item:last-child {
  padding-bottom: 0;
  border-bottom: none;
}

.preference-item__desc {
  margin-top: 6px;
  line-height: 1.7;
}

@media (max-width: 900px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
