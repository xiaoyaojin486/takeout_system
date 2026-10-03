<template>
  <div class="app-root">
    <router-view />

    <!-- 底部导航：首页 / 购物车 / 订单 / 我的 -->
    <van-tabbar
      v-if="showTabbar"
      route
      safe-area-inset-bottom
      active-color="#ee0a24"
      inactive-color="#646566"
    >
      <van-tabbar-item replace to="/home" icon="wap-home-o">首页</van-tabbar-item>
      <van-tabbar-item replace to="/cart" icon="shopping-cart-o" :badge="cartBadge">
        购物车
      </van-tabbar-item>
      <van-tabbar-item replace to="/orders" icon="orders-o">订单</van-tabbar-item>
      <van-tabbar-item replace to="/mine" icon="user-o">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup>
import { computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useCartStore } from './stores/cart'
import { getAccessToken } from './utils/request'

const route = useRoute()
const cartStore = useCartStore()

// 仅在标注了 tabbar 的页面展示底部导航
const showTabbar = computed(() => route.meta.tabbar === true)
// 件数为 0 时不显示角标
const cartBadge = computed(() => (cartStore.count > 0 ? cartStore.count : ''))

onMounted(() => {
  if (getAccessToken()) cartStore.loadCount()
})

// 登录后再进入带 tabbar 的页面时刷新一次角标
watch(
  () => route.path,
  () => {
    if (getAccessToken() && route.meta.tabbar) cartStore.loadCount()
  }
)
</script>

<style>
/* 品牌主色（Vant 主题变量） */
:root {
  --van-primary-color: #ee0a24;
}

html,
body,
#app {
  height: 100%;
}

body {
  margin: 0;
  background: #f7f8fa;
  color: #323233;
  font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
}

.app-root {
  min-height: 100%;
}

/* 带底部 tabbar 的页面统一留白，避免内容被导航遮挡 */
.page {
  min-height: 100vh;
  padding-bottom: 60px;
  box-sizing: border-box;
}

.text-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>