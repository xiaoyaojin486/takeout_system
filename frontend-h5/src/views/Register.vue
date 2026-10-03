<template>
  <div class="register-page">
    <van-nav-bar title="注册账号" left-arrow @click-left="onBack" />

    <van-form @submit="onSubmit">
      <van-cell-group inset>
        <van-field
          v-model="form.username"
          label="账号"
          placeholder="4-20 位字母或数字"
          clearable
          :rules="[
            { required: true, message: '请输入账号' },
            { pattern: /^[A-Za-z0-9_]{4,20}$/, message: '账号为 4-20 位字母、数字或下划线' }
          ]"
        />
        <van-field
          v-model="form.phone"
          type="tel"
          label="手机号"
          placeholder="请输入手机号"
          clearable
          :rules="[
            { required: true, message: '请输入手机号' },
            { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }
          ]"
        />
        <van-field
          v-model="form.nickname"
          label="昵称"
          placeholder="选填，默认使用账号"
          clearable
        />
        <van-field
          v-model="form.password"
          type="password"
          label="密码"
          placeholder="6-20 位"
          clearable
          :rules="[
            { required: true, message: '请输入密码' },
            { pattern: /^.{6,20}$/, message: '密码长度为 6-20 位' }
          ]"
        />
        <van-field
          v-model="form.confirmPassword"
          type="password"
          label="确认密码"
          placeholder="请再次输入密码"
          clearable
          :rules="[{ validator: validateConfirm, message: '两次输入的密码不一致' }]"
        />
      </van-cell-group>

      <div class="submit-wrap">
        <van-button
          round
          block
          type="primary"
          native-type="submit"
          :loading="loading"
          loading-text="注册中..."
        >
          注册
        </van-button>
      </div>
    </van-form>

    <div class="extra">
      <span @click="goLogin">已有账号？返回登录</span>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { register } from '../api/user'

const router = useRouter()

const form = reactive({
  username: '',
  phone: '',
  nickname: '',
  password: '',
  confirmPassword: ''
})
const loading = ref(false)

function validateConfirm(value) {
  return value === form.password
}

async function onSubmit() {
  loading.value = true
  try {
    await register({
      username: form.username.trim(),
      password: form.password,
      phone: form.phone.trim(),
      nickname: form.nickname.trim() || undefined
    })
    showToast('注册成功，请登录')
    router.replace('/login')
  } catch (e) {
    // 错误提示已由拦截器统一处理
  } finally {
    loading.value = false
  }
}

function onBack() {
  router.back()
}

function goLogin() {
  router.replace('/login')
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  background: #f7f8fa;
}

.submit-wrap {
  margin: 24px 16px 0;
}

.extra {
  margin-top: 20px;
  text-align: center;
  font-size: 13px;
  color: #ee0a24;
}
</style>