<template>
  <div class="page teacher-profile-page">
    <PageHeader
      title="个人信息"
      description="教师个人信息页延续统一设计语言，聚焦教学身份、联系方式和账号安全等正式信息。"
      :breadcrumbs="['教师服务', '个人信息']"
    >
      <template #actions>
        <el-button>更新联系方式</el-button>
        <el-button type="primary">保存设置</el-button>
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

        <PageSection title="基础信息" description="展示教师岗位、部门、办公地点等稳定资料。">
          <div class="info-grid">
            <div v-for="item in profile.baseInfo" :key="item.label" class="info-item">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </PageSection>
      </div>

      <div class="section-stack">
        <PageSection title="教学信息" description="保留授课班级、主授科目与辅导员身份等教学核心字段。">
          <div class="info-list">
            <div v-for="item in profile.teachingInfo" :key="item.label" class="info-list__item">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </PageSection>

        <PageSection title="账号安全" description="统一展示账号安全信息，适合高校系统长期维护。">
          <div class="info-list">
            <div v-for="item in profile.security" :key="item.label" class="info-list__item">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </PageSection>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchTeacherProfile, type TeacherProfileData } from '../api/erp'
import PageHeader from '../components/common/PageHeader.vue'
import PageSection from '../components/common/PageSection.vue'
import { showRequestError } from '../lib/feedback'
import { useAppStore } from '../stores/app'

const store = useAppStore()

const profile = ref<TeacherProfileData>({
  baseInfo: [],
  teachingInfo: [],
  security: [],
})

const currentUser = computed(() => {
  return (
    store.currentUser ?? {
      displayName: '教师',
      roles: ['未分配角色'],
      username: '',
    }
  )
})

const userInitial = computed(() => currentUser.value.displayName.slice(0, 1) || '师')

const loadData = async () => {
  if (!store.currentUser?.username) {
    return
  }

  try {
    profile.value = await fetchTeacherProfile(store.currentUser.username)
  } catch (error) {
    showRequestError(error, '教师个人信息加载失败。')
  }
}

onMounted(loadData)
</script>

<style scoped>
.teacher-profile-page {
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
.info-list__item span {
  color: var(--erp-color-text-tertiary);
  font-size: 12px;
}

.info-item strong,
.info-list__item strong {
  margin-top: 8px;
  display: block;
  font-size: 15px;
  font-weight: 600;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

@media (max-width: 900px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
