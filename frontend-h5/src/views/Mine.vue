<template>
  <div class="page mine-page">
    <div class="user-card">
      <van-image
        class="avatar"
        round
        width="56"
        height="56"
        :src="resolveImage(userStore.userInfo?.avatar)"
      >
        <template #error>
          <van-icon name="user-o" size="28" color="#c8c9cc" />
        </template>
      </van-image>

      <div class="user-main">
        <div class="user-name">{{ userStore.displayName }}</div>
        <div class="user-sub">
          {{ userStore.userInfo ? `账号：${userStore.userInfo.username}` : '登录后可享受更多服务' }}
        </div>
      </div>

      <van-button v-if="!userStore.isLoggedIn" size="small" type="primary" round @click="goLogin">
        登录
      </van-button>
    </div>

    <van-cell-group inset>
      <van-cell title="我的订单" icon="orders-o" is-link @click="goOrders" />
      <van-cell title="收货地址" icon="location-o" is-link @click="goAddress" />
      <van-cell title="个人资料" icon="contact-o" is-link @click="goProfile" />
    </van-cell-group>

    <div v-if="userStore.isLoggedIn" class="logout-wrap">
      <van-button round block plain type="danger" @click="onLogout">退出登录</van-button>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import { useUserStore } from '../stores/user'
import { useCartStore } from '../stores/cart'
import { resolveImage } from '../utils/format'

const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()

async function loadProfile() {
  if (!userStore.isLoggedIn) return
  try {
    await userStore.loadProfile()
  } catch (e) {
    // 错误提示已由拦截器统一处理
  }
}

async function onLogout() {
  try {
    await showConfirmDialog({ title: '退出登录', message: '确定退出当前账号吗？' })
  } catch (e) {
    return
  }
  await userStore.logout()
  cartStore.reset()
  showToast('已退出登录')
  router.replace('/login')
}

function goLogin() {
  router.push('/login')
}

function goOrders() {
  router.push('/orders')
}

function goAddress() {
  router.push('/address')
}

function goProfile() {
  router.push('/profile')
}

onMounted(loadProfile)
</script>

<style scoped>
.mine-page {
  background: #f7f8fa;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 16px;
  background: linear-gradient(135deg, #ff6034, #ee0a24);
  color: #fff;
}

.avatar {
  flex: none;
  background: #fff;
}

.user-main {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 18px;
  font-weight: 600;
}

.user-sub {
  margin-top: 4px;
  font-size: 12px;
  opacity: 0.9;
}

.logout-wrap {
  margin: 24px 16px 0;
}
</style>