<template>
  <div class="page-container">
    <!-- 审核未通过时置顶提示 -->
    <el-alert
      v-if="shop && shop.auditStatus !== 1"
      class="mb-16"
      :type="shop.auditStatus === 2 ? 'error' : 'warning'"
      :closable="false"
      show-icon
      title="店铺未通过审核，暂不可营业"
    >
      <template #default>
        <div>
          <div>店铺未通过审核，暂不可营业</div>
          <div class="text-muted">
            当前状态：{{ auditStatusText(shop.auditStatus) }}
            <template v-if="shop.auditRemark">；驳回原因：{{ shop.auditRemark }}</template>
          </div>
        </div>
      </template>
    </el-alert>

    <el-card v-loading="loading" shadow="never">
      <template #header>
        <div class="card-header">
          <span>{{ isApplyMode ? '入驻申请' : '店铺设置' }}</span>
          <el-tag v-if="shop" :type="auditStatusTag(shop.auditStatus)">
            {{ shop.auditStatusDesc || auditStatusText(shop.auditStatus) }}
          </el-tag>
        </div>
      </template>

      <el-alert
        v-if="isApplyMode"
        class="mb-16"
        type="info"
        :closable="false"
        show-icon
        title="您还未提交入驻申请，请先填写以下信息提交审核"
      />

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="110px"
        class="shop-form"
      >
        <el-form-item label="店铺名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入店铺名称" maxlength="50" />
        </el-form-item>

        <el-form-item label="店铺 Logo" prop="logo">
          <ImageUpload v-model="form.logo" />
        </el-form-item>

        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入联系电话" maxlength="20" />
        </el-form-item>

        <el-form-item label="店铺地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入店铺地址" maxlength="120" />
        </el-form-item>

        <el-form-item label="起送价（元）" prop="minAmount">
          <el-input-number
            v-model="form.minAmount"
            :min="0"
            :max="9999"
            :precision="2"
            :step="1"
          />
        </el-form-item>

        <el-form-item label="配送费（元）" prop="deliveryFee">
          <el-input-number
            v-model="form.deliveryFee"
            :min="0"
            :max="9999"
            :precision="2"
            :step="1"
          />
        </el-form-item>

        <el-form-item label="店铺简介" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="请输入店铺简介"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            {{ isApplyMode ? '提交入驻申请' : '保存修改' }}
          </el-button>
          <el-button v-if="!isApplyMode" @click="loadShop">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 营业状态 -->
    <el-card v-if="shop" v-loading="statusLoading" shadow="never" class="mt-16">
      <template #header>
        <span>营业状态</span>
      </template>
      <div class="business-row">
        <el-switch
          v-model="businessStatus"
          :active-value="1"
          :inactive-value="0"
          :disabled="shop.auditStatus !== 1"
          active-text="营业中"
          inactive-text="休息中"
          inline-prompt
          @change="handleBusinessStatusChange"
        />
        <span class="text-muted business-tip">
          <template v-if="shop.auditStatus !== 1">店铺未通过审核，暂不可切换营业状态</template>
          <template v-else>关闭后顾客将无法下单，已下单订单不受影响</template>
        </span>
      </div>
      <el-descriptions :column="3" border class="mt-16">
        <el-descriptions-item label="评分">
          {{ shop.rating ?? '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="累计销量">
          {{ shop.sales ?? 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="入驻时间">
          {{ shop.createTime || '-' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import ImageUpload from '@/components/ImageUpload.vue'
import { getShop, applyShop, updateShop, updateBusinessStatus } from '@/api/shop'
import { auditStatusText, auditStatusTag } from '@/utils/constants'

const formRef = ref(null)
const loading = ref(false)
const submitting = ref(false)
const statusLoading = ref(false)

/** 店铺信息，null 表示尚未入驻 */
const shop = ref(null)
const businessStatus = ref(0)

const isApplyMode = computed(() => !shop.value)

const form = reactive({
  name: '',
  logo: '',
  phone: '',
  address: '',
  minAmount: 0,
  deliveryFee: 0,
  description: ''
})

const rules = {
  name: [{ required: true, message: '请输入店铺名称', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$|^0\d{2,3}-?\d{7,8}$/, message: '联系电话格式不正确', trigger: 'blur' }
  ],
  address: [{ required: true, message: '请输入店铺地址', trigger: 'blur' }],
  minAmount: [{ required: true, message: '请输入起送价', trigger: 'blur' }],
  deliveryFee: [{ required: true, message: '请输入配送费', trigger: 'blur' }]
}

/** 拉取店铺信息 */
async function loadShop() {
  loading.value = true
  try {
    const data = await getShop()
    shop.value = data || null
    if (data) {
      form.name = data.name || ''
      form.logo = data.logo || ''
      form.phone = data.phone || ''
      form.address = data.address || ''
      form.minAmount = Number(data.minAmount || 0)
      form.deliveryFee = Number(data.deliveryFee || 0)
      form.description = data.description || ''
      businessStatus.value = data.businessStatus ?? 0
    } else {
      resetForm()
    }
  } catch (e) {
    // 店铺不存在时按未入驻处理
    shop.value = null
    resetForm()
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.name = ''
  form.logo = ''
  form.phone = ''
  form.address = ''
  form.minAmount = 0
  form.deliveryFee = 0
  form.description = ''
  formRef.value?.clearValidate()
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  submitting.value = true
  try {
    const payload = { ...form }
    if (isApplyMode.value) {
      await applyShop(payload)
      ElMessage.success('入驻申请已提交，请等待平台审核')
    } else {
      await updateShop(payload)
      ElMessage.success('店铺信息已保存')
    }
    await loadShop()
  } catch (e) {
    /* 已统一提示 */
  } finally {
    submitting.value = false
  }
}

/** 切换营业状态 */
async function handleBusinessStatusChange(value) {
  statusLoading.value = true
  try {
    await updateBusinessStatus({ businessStatus: value })
    ElMessage.success(value === 1 ? '已开始营业' : '已切换为休息中')
    if (shop.value) {
      shop.value.businessStatus = value
    }
  } catch (e) {
    // 失败回滚开关
    businessStatus.value = value === 1 ? 0 : 1
  } finally {
    statusLoading.value = false
  }
}

onMounted(() => {
  loadShop()
})
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.shop-form {
  max-width: 620px;
}

.business-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.business-tip {
  font-size: 13px;
}
</style>