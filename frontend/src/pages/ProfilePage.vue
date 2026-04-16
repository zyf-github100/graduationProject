<template>
  <div class="page">
    <PageHeader
      title="个人中心"
      description="设置页延续系统设计语言，用于展示账号信息、安全设置和通知偏好，不另起一套视觉体系。"
      :breadcrumbs="['个人中心']"
    >
      <template #actions>
        <el-button>查看登录日志</el-button>
        <el-button type="primary">保存设置</el-button>
      </template>
    </PageHeader>

    <div class="page-grid two-columns">
      <div class="section-stack">
        <div class="surface profile-card">
          <div class="profile-card__top">
            <el-avatar :size="72">{{ userInitial }}</el-avatar>
            <div>
              <h2>{{ currentUser.displayName }}</h2>
              <p>{{ currentUser.orgUnit }} · {{ currentUser.username }}</p>
            </div>
          </div>

          <div class="profile-card__roles">
            <el-tag
              v-for="role in currentUser.roles"
              :key="role"
              round
            >
              {{ role }}
            </el-tag>
          </div>

          <el-divider />

          <div class="profile-card__meta">
            <div
              v-for="item in profileSecurityItems"
              :key="item.label"
              class="profile-card__meta-item"
            >
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </div>
      </div>

      <div class="section-stack">
        <PageSection title="账户安全" description="安全相关操作保持表单化结构，适合长期维护和权限扩展。">
          <el-form label-position="top">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="当前密码">
                  <el-input type="password" show-password />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="新密码">
                  <el-input type="password" show-password />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-alert
                  type="info"
                  :closable="false"
                  show-icon
                  title="高风险权限账号建议每 90 天更新密码，导出权限需单独审计。"
                />
              </el-col>
            </el-row>
          </el-form>
        </PageSection>

        <PageSection title="通知偏好" description="消息设置与系统消息服务对齐，支持站内信、短信和流程提醒的偏好控制。">
          <div class="preference-list">
            <div class="preference-item">
              <div>
                <div class="preference-item__title">审批待办提醒</div>
                <div class="preference-item__desc">接收请假、调课、报修等流程待办提醒。</div>
              </div>
              <el-switch v-model="preferences.todo" />
            </div>
            <div class="preference-item">
              <div>
                <div class="preference-item__title">收费异常提醒</div>
                <div class="preference-item__desc">接收到期账单、逾期账单和对账异常通知。</div>
              </div>
              <el-switch v-model="preferences.billing" />
            </div>
            <div class="preference-item">
              <div>
                <div class="preference-item__title">成绩发布通知</div>
                <div class="preference-item__desc">接收成绩审核和发布时间窗口提醒。</div>
              </div>
              <el-switch v-model="preferences.grade" />
            </div>
          </div>
        </PageSection>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive } from 'vue'
import PageHeader from '../components/common/PageHeader.vue'
import PageSection from '../components/common/PageSection.vue'
import { showRequestError } from '../lib/feedback'
import { useAppStore } from '../stores/app'

const store = useAppStore()

const preferences = reactive({
  todo: true,
  billing: true,
  grade: false,
})

const currentUser = computed(() => {
  return (
    store.currentUser ?? {
      displayName: '管理员',
      orgUnit: '未分配组织',
      orgUnitId: 0,
      roles: ['未分配角色'],
      userId: 0,
      userType: 'ADMIN',
      username: '',
    }
  )
})

const userInitial = computed(() => currentUser.value.displayName.slice(0, 1) || '管')

const profileSecurityItems = computed(() => {
  const permissions = store.permissions
  return [
    { label: '账号标识', value: currentUser.value.username || '--' },
    { label: '所属组织', value: currentUser.value.orgUnit || '--' },
    { label: '权限条目', value: `${permissions?.permissions.length ?? 0} 项` },
    {
      label: '数据范围',
      value: permissions?.dataScopes.length ? permissions.dataScopes.join('、') : '默认数据范围',
    },
    {
      label: '导出权限',
      value: permissions?.exportPermissions.length
        ? permissions.exportPermissions.join('、')
        : '未配置导出权限',
    },
  ]
})

onMounted(async () => {
  try {
    await store.initializeSession()
  } catch (error) {
    showRequestError(error, '个人中心初始化失败。')
  }
})
</script>

<style scoped>
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
  font-size: 26px;
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

.profile-card__meta {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.profile-card__meta-item {
  display: flex;
  justify-content: space-between;
  gap: var(--erp-space-4);
  color: var(--erp-color-text-secondary);
}

.preference-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.preference-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--erp-space-4);
  padding: 14px 0;
  border-bottom: 1px solid var(--erp-color-border);
}

.preference-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.preference-item__title {
  font-weight: 600;
}

.preference-item__desc {
  margin-top: 6px;
  color: var(--erp-color-text-secondary);
  font-size: var(--erp-font-size-body-sm);
}
</style>
