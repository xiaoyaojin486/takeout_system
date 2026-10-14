<template>
  <div class="login-page">
    <div class="login-box">
      <div class="login-header">
        <el-icon :size="34" color="#409eff"><Bowl /></el-icon>
        <h1 class="login-title">外卖系统</h1>
        <p class="login-subtitle">商家端 / 平台管理后台</p>
      </div>

      <!-- 端选择器：商家账号与管理员账号不能混用，传错端会返回 10001 -->
      <el-radio-group v-model="form.clientType" class="client-switch" size="large">
        <el-radio-button :value="CLIENT_TYPE.MERCHANT">
          <el-icon><Shop /></el-icon>
          <span>商家端</span>
        </el-radio-button>
        <el-radio-button :value="CLIENT_TYPE.ADMIN">
          <el-icon><Setting /></el-icon>
          <span>管理后台</span>
        </el-radio-button>
      </el-radio-group>

      <el-alert
        class="mb-16"
        :title="clientTip"
        type="info"
        :closable="false"
        show-icon
      />

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleLogin"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
            clearable
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            class="login-button"
            :loading="loading"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { homePath } from '@/router'
import { CLIENT_TYPE } from '@/utils/constants'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  clientType: CLIENT_TYPE.MERCHANT
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ]
}

const clientTip = computed(() =>
  form.clientType === CLIENT_TYPE.MERCHANT
    ? '商家端：仅商家账号可登录，登录后进入店铺工作台'
    : '管理后台：仅平台管理员账号可登录，登录后进入平台看板'
)

// 切换端时清空已填账号，避免拿商家账号去登管理后台
watch(
  () => form.clientType,
  () => {
    formRef.value?.clearValidate()
  }
)

async function handleLogin() {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }

  loading.value = true
  try {
    const userInfo = await userStore.login({ ...form })
    ElMessage.success(`登录成功，欢迎回来 ${userInfo.nickname || userInfo.username}`)
    const redirect = route.query.redirect
    // 商家账号只能用商家端，管理员账号只能用管理后台（userType 由后端返回）
    if (redirect && isRedirectAllowed(redirect, userInfo.userType)) {
      router.replace(redirect)
    } else {
      router.replace(homePath(userInfo.userType))
    }
  } catch (e) {
    /* 错误提示已在响应拦截器中统一处理 */
  } finally {
    loading.value = false
  }
}

/** 防止把商家重定向到管理后台地址 */
function isRedirectAllowed(redirect, userType) {
  if (userType === 3) return redirect.startsWith('/admin')
  if (userType === 2) return redirect.startsWith('/merchant')
  return false
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f2d3d 0%, #2c3e50 45%, #409eff 100%);
}

.login-box {
  width: 420px;
  padding: 32px 36px 24px;
  background-color: #fff;
  border-radius: 10px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.25);
}

.login-header {
  text-align: center;
  margin-bottom: 20px;
}

.login-title {
  margin: 8px 0 4px;
  font-size: 24px;
  color: #303133;
}

.login-subtitle {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

.client-switch {
  display: flex;
  width: 100%;
  margin-bottom: 16px;
}

.client-switch :deep(.el-radio-button) {
  flex: 1;
}

.client-switch :deep(.el-radio-button__inner) {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.login-button {
  width: 100%;
}
</style>