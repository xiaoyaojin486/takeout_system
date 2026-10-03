<template>
  <div class="login-page">
    <div class="brand">
      <van-icon name="shop-o" size="52" color="#ee0a24" />
      <h1 class="brand-title">外卖点餐</h1>
      <p class="brand-sub">顾客端 · 登录后即可下单</p>
    </div>

    <van-form @submit="onSubmit">
      <van-cell-group inset>
        <van-field
          v-model="form.username"
          name="username"
          label="账号"
          placeholder="请输入账号"
          clearable
          :rules="[{ required: true, message: '请输入账号' }]"
        />
        <van-field
          v-model="form.password"
          type="password"
          name="password"
          label="密码"
          placeholder="请输入密码"
          clearable
          :rules="[{ required: true, message: '请输入密码' }]"
        />
      </van-cell-group>

      <div class="submit-wrap">
        <van-button
          round
          block
          type="primary"
          native-type="submit"
          :loading="loading"
          loading-text="登录中..."
        >
          登录
        </van-button>
      </div>
    </van-form>

    <div class="extra">
      <span @click="goRegister">没有账号？立即注册</span>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useUserStore } from '../stores/user'
import { useCartStore } from '../stores/cart'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()

const form = reactive({ username: '', password: '' })
const loading = ref(false)

async function onSubmit() {
  loading.value = true
  try {
    await userStore.login({ username: form.username.trim(), password: form.password })
    await cartStore.loadCount()
    showToast('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/home'
    router.replace(redirect)
  } catch (e) {
    // 错误提示已由 request.js 拦截器统一处理
  } finally {
    loading.value = false
  }
}

function goRegister() {
  router.push('/register')
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  padding: 48px 0 24px;
  box-sizing: border-box;
  background: linear-gradient(180deg, #fff5f5 0%, #f7f8fa 240px);
}

.brand {
  text-align: center;
  margin-bottom: 32px;
}

.brand-title {
  margin: 8px 0 4px;
  font-size: 22px;
  color: #323233;
}

.brand-sub {
  margin: 0;
  font-size: 13px;
  color: #969799;
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