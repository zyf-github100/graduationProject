<template>
  <div class="page">
    <PageHeader
      :title="isEdit ? '编辑学生档案' : '新建学生档案'"
      description="表单页遵循统一分组、稳定标签对齐和吸顶操作栏规则，适合学生、教师、课程、费用项目等主数据录入场景。"
      :breadcrumbs="['基础数据', '学生档案', isEdit ? '编辑' : '新建']"
    >
      <template #actions>
        <el-button @click="handleCancel">返回列表</el-button>
      </template>
    </PageHeader>

    <PageSection title="基本信息" description="主数据字段按档案识别优先级排列，减少录入路径切换。">
      <el-form label-position="top" :model="form">
        <el-row :gutter="16">
          <el-col :lg="8" :md="12">
            <el-form-item label="学生姓名" required>
              <el-input v-model="form.studentName" />
            </el-form-item>
          </el-col>
          <el-col :lg="8" :md="12">
            <el-form-item label="学号" required>
              <el-input v-model="form.studentNo" />
            </el-form-item>
          </el-col>
            <el-col :lg="8" :md="12">
              <el-form-item label="性别" required>
                <el-select v-model="form.gender" style="width: 100%">
                  <el-option v-for="gender in options.genders" :key="gender" :label="gender" :value="gender" />
                </el-select>
              </el-form-item>
            </el-col>
          <el-col :lg="8" :md="12">
            <el-form-item label="身份证号">
              <el-input v-model="form.idCardMasked" />
            </el-form-item>
          </el-col>
            <el-col :lg="8" :md="12">
              <el-form-item label="入学日期">
                <el-date-picker
                  v-model="form.admissionDate"
                  style="width: 100%"
                  value-format="YYYY-MM-DD"
                />
              </el-form-item>
            </el-col>
            <el-col :lg="8" :md="12">
              <el-form-item label="学生状态">
                <el-select v-model="form.status" style="width: 100%">
                  <el-option
                    v-for="status in options.statuses"
                    :key="status.value"
                    :label="status.label"
                    :value="status.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
        </el-row>
      </el-form>
    </PageSection>

    <PageSection title="学籍信息" description="年级、班级、校区和辅导员等字段采用统一主数据选择方式。">
      <el-form label-position="top" :model="form">
        <el-row :gutter="16">
            <el-col :lg="6" :md="12">
              <el-form-item label="校区">
                <el-select v-model="form.campus" style="width: 100%">
                  <el-option v-for="campus in options.campuses" :key="campus" :label="campus" :value="campus" />
                </el-select>
              </el-form-item>
            </el-col>
          <el-col :lg="6" :md="12">
            <el-form-item label="年级">
              <el-select v-model="form.gradeName" style="width: 100%">
                <el-option v-for="grade in options.grades" :key="grade" :label="grade" :value="grade" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :lg="6" :md="12">
            <el-form-item label="班级">
              <el-input v-model="form.className" />
            </el-form-item>
          </el-col>
          <el-col :lg="6" :md="12">
            <el-form-item label="辅导员">
              <el-input v-model="form.classTeacher" />
            </el-form-item>
          </el-col>
          <el-col :lg="6" :md="12">
            <el-form-item label="宿舍">
              <el-input v-model="form.dormitory" placeholder="例如：2号楼 301" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </PageSection>

    <PageSection title="联系信息" description="联系人关系和联系方式是审批、收费、通知等模块的重要基础。">
      <el-form label-position="top" :model="form">
        <el-row :gutter="16">
          <el-col :lg="8" :md="12">
            <el-form-item label="紧急联系人姓名">
              <el-input v-model="form.guardianName" />
            </el-form-item>
          </el-col>
          <el-col :lg="8" :md="12">
            <el-form-item label="紧急联系人电话">
              <el-input v-model="form.guardianPhone" />
            </el-form-item>
          </el-col>
          <el-col :lg="8" :md="24">
            <el-form-item label="联系地址">
              <el-input v-model="form.address" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input
                v-model="form.remark"
                type="textarea"
                :rows="4"
                placeholder="记录需长期跟踪的档案说明。"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </PageSection>

    <div class="sticky-action-bar">
      <div class="label-muted">保存后可继续补充联系人信息、导入附件或进入详情页查看留痕。</div>
      <div class="toolbar-inline">
        <el-button @click="handleCancel">取消</el-button>
        <el-button @click="handleSave('草稿')">保存草稿</el-button>
        <el-button type="primary" @click="handleSave('提交')">
          {{ isEdit ? '保存修改' : '保存并建档' }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  createStudent,
  fetchStudentDetail,
  fetchStudentOptions,
  updateStudent,
  type StudentDetailData,
  type StudentOptionsData,
  type StudentSavePayload,
} from '../api/erp'
import PageHeader from '../components/common/PageHeader.vue'
import PageSection from '../components/common/PageSection.vue'
import { showRequestError } from '../lib/feedback'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => Boolean(route.params.id))
const options = ref<StudentOptionsData>({
  campuses: ['主校区'],
  genders: ['女', '男'],
  grades: ['2025级'],
  statuses: [
    { label: '在读', value: 'ACTIVE' },
    { label: '请假中', value: 'LEAVE' },
    { label: '休学', value: 'SUSPENDED' },
  ],
})
const existingContacts = ref<StudentDetailData['contacts']>([])

const form = reactive<StudentSavePayload>({
  studentName: '',
  studentNo: '',
  gender: '女',
  idCardMasked: '',
  admissionDate: '',
  status: 'ACTIVE',
  campus: '主校区',
  gradeName: '2025级',
  className: '',
  classTeacher: '',
  dormitory: '',
  guardianName: '',
  guardianPhone: '',
  address: '',
  remark: '',
})

const assignForm = (detail: StudentSavePayload) => {
  form.studentName = detail.studentName
  form.studentNo = detail.studentNo
  form.gender = detail.gender
  form.idCardMasked = detail.idCardMasked
  form.admissionDate = detail.admissionDate
  form.status = detail.status
  form.campus = detail.campus
  form.gradeName = detail.gradeName
  form.className = detail.className
  form.classTeacher = detail.classTeacher
  form.dormitory = detail.dormitory
  form.guardianName = detail.guardianName
  form.guardianPhone = detail.guardianPhone
  form.address = detail.address
  form.remark = detail.remark
}

const loadOptions = async () => {
  try {
    options.value = await fetchStudentOptions()
  } catch (error) {
    showRequestError(error, '学生表单选项加载失败。')
  }
}

const loadStudent = async () => {
  if (!isEdit.value) {
    return
  }

  try {
    const detail = await fetchStudentDetail(Number(route.params.id))
    existingContacts.value = detail.contacts
    assignForm({
      studentName: detail.studentName,
      studentNo: detail.studentNo,
      gender: detail.gender,
      idCardMasked: detail.idCardMasked,
      admissionDate: detail.admissionDate,
      status: detail.status,
      campus: detail.campus,
      gradeName: detail.gradeName,
      className: detail.className,
      classTeacher: detail.classTeacher,
      dormitory: detail.dormitory,
      guardianName: detail.guardianName,
      guardianPhone: detail.guardianPhone,
      address: detail.address,
      remark: detail.remark,
      contacts: detail.contacts,
    })
  } catch (error) {
    showRequestError(error, '学生档案加载失败。')
  }
}

const handleSave = async (mode: string) => {
  const payload: StudentSavePayload = {
    ...form,
    contacts: existingContacts.value,
  }

  try {
    const record = isEdit.value
      ? await updateStudent(Number(route.params.id), payload)
      : await createStudent(payload)

    ElMessage.success(mode === '草稿' ? '已保存当前录入内容。' : isEdit.value ? '学生档案已更新。' : '学生档案已创建。')
    await router.push(`/students/${record.id}`)
  } catch (error) {
    showRequestError(error, isEdit.value ? '学生档案保存失败。' : '学生建档失败。')
  }
}

const handleCancel = () => {
  router.push(isEdit.value ? `/students/${route.params.id}` : '/students')
}

onMounted(async () => {
  await loadOptions()
  await loadStudent()
})
</script>
