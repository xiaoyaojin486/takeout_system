<template>
  <div class="page cart-page">
    <van-nav-bar title="购物车" fixed placeholder>
      <template #right>
        <span v-if="hasItems" class="nav-action" @click="onClear">清空</span>
      </template>
    </van-nav-bar>

    <div v-if="loading" class="page-loading">
      <van-loading vertical>加载中...</van-loading>
    </div>

    <van-empty v-else-if="!hasItems" description="购物车还是空的">
      <van-button round type="primary" class="empty-btn" @click="goHome">去逛逛</van-button>
    </van-empty>

    <div v-else>
      <div v-for="group in groups" :key="group.merchantId" class="shop-group">
        <div class="group-head" @click="goMerchant(group.merchantId)">
          <van-icon name="shop-o" />
          <span class="group-name text-ellipsis">{{ group.merchantName }}</span>
          <van-tag v-if="Number(group.businessStatus) !== 1" type="default">休息中</van-tag>
          <van-icon name="arrow" class="group-arrow" />
        </div>

        <div
          v-for="item in group.items"
          :key="item.id"
          class="cart-item"
          :class="{ disabled: item.available === false }"
        >
          <van-image
            class="item-img"
            :src="resolveImage(item.dishImage)"
            width="64"
            height="64"
            radius="6"
            fit="cover"
          >
            <template #error>
              <van-icon name="photo-o" size="22" color="#c8c9cc" />
            </template>
          </van-image>

          <div class="item-main">
            <div class="item-name text-ellipsis">{{ item.dishName }}</div>
            <div class="item-sub">
              <van-tag v-if="item.available === false" type="default">已下架</van-tag>
              <span v-else>库存 {{ item.stock ?? 0 }}</span>
            </div>
            <div class="item-bottom">
              <span class="item-price">¥{{ formatAmount(item.price) }}</span>
              <van-stepper
                :model-value="item.quantity"
                min="0"
                :max="maxOf(item)"
                integer
                button-size="22"
                :disabled="item.available === false"
                @change="(val) => changeQuantity(item, val)"
              />
            </div>
          </div>
        </div>

        <div class="group-foot">
          <div class="foot-left">
            <div class="foot-amount">合计 <em>¥{{ formatAmount(group.totalAmount) }}</em></div>
            <div class="foot-tip">
              起送 ¥{{ formatAmount(group.minAmount) }} · 配送 ¥{{ formatAmount(group.deliveryFee) }}
            </div>
            <div v-if="!group.reachMinAmount" class="foot-lack">
              还差 ¥{{ formatAmount(lackOf(group)) }} 起送
            </div>
          </div>
          <van-button
            type="primary"
            round
            size="small"
            :disabled="!group.reachMinAmount || Number(group.businessStatus) !== 1"
            @click="goCheckout(group)"
          >
            去结算
          </van-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import { clearCart, getCart, updateCartItem } from '../api/cart'
import { useCartStore } from '../stores/cart'
import { formatAmount, resolveImage } from '../utils/format'

const router = useRouter()
const cartStore = useCartStore()

const groups = ref([])
const loading = ref(true)

const hasItems = computed(() => groups.value.some((g) => g.items && g.items.length > 0))

function maxOf(item) {
  const stock = Number(item.stock || 0)
  return stock > 0 ? stock : 99
}

function lackOf(group) {
  return Math.max(0, Number(group.minAmount || 0) - Number(group.totalAmount || 0))
}

async function loadCart() {
  loading.value = true
  try {
    const data = await getCart()
    groups.value = data || []
    await cartStore.loadCount()
  } catch (e) {
    // 错误提示已由拦截器统一处理
  } finally {
    loading.value = false
  }
}

async function changeQuantity(item, value) {
  try {
    // quantity 传 0 后端视为移除
    await updateCartItem(item.id, value)
    await loadCart()
  } catch (e) {
    // 错误提示已由拦截器统一处理
  }
}

async function onClear() {
  try {
    await showConfirmDialog({ title: '提示', message: '确定清空购物车吗？' })
  } catch (e) {
    return
  }
  try {
    await clearCart()
    showToast('已清空')
    await loadCart()
  } catch (e) {
    // 错误提示已由拦截器统一处理
  }
}

function goMerchant(merchantId) {
  router.push(`/merchant/${merchantId}`)
}

function goCheckout(group) {
  router.push({ path: '/order/confirm', query: { merchantId: group.merchantId } })
}

function goHome() {
  router.push('/home')
}

onMounted(loadCart)
</script>

<style scoped>
.cart-page {
  background: #f7f8fa;
}

.nav-action {
  font-size: 14px;
  color: #ee0a24;
}

.page-loading {
  display: flex;
  justify-content: center;
  padding-top: 80px;
}

.empty-btn {
  width: 120px;
}

.shop-group {
  margin: 10px;
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
}

.group-head {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px;
  font-size: 15px;
  font-weight: 600;
  border-bottom: 1px solid #f7f8fa;
}

.group-name {
  flex: 1;
  min-width: 0;
}

.group-arrow {
  color: #969799;
  font-weight: 400;
}

.cart-item {
  display: flex;
  padding: 10px 12px;
}

.cart-item.disabled {
  opacity: 0.5;
}

.item-img {
  flex: none;
  background: #f7f8fa;
}

.item-main {
  flex: 1;
  min-width: 0;
  margin-left: 10px;
  display: flex;
  flex-direction: column;
}

.item-name {
  font-size: 14px;
  font-weight: 500;
}

.item-sub {
  margin-top: 2px;
  font-size: 12px;
  color: #969799;
}

.item-bottom {
  margin-top: auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.item-price {
  color: #ee0a24;
  font-size: 15px;
  font-weight: 600;
}

.group-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-top: 1px solid #f7f8fa;
}

.foot-left {
  flex: 1;
  min-width: 0;
}

.foot-amount {
  font-size: 14px;
}

.foot-amount em {
  color: #ee0a24;
  font-style: normal;
  font-weight: 600;
  font-size: 16px;
}

.foot-tip,
.foot-lack {
  font-size: 11px;
  color: #969799;
}

.foot-lack {
  color: #ee0a24;
}
</style>