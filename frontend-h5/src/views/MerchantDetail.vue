<template>
  <div class="merchant-page">
    <van-nav-bar
      :title="merchant?.name || '店铺详情'"
      left-arrow
      fixed
      placeholder
      @click-left="onBack"
    />

    <div v-if="merchant" class="merchant-header">
      <van-image
        :src="resolveImage(merchant.logo)"
        width="60"
        height="60"
        radius="8"
        fit="cover"
        class="header-logo"
      >
        <template #error>
          <van-icon name="shop-o" size="26" color="#c8c9cc" />
        </template>
      </van-image>
      <div class="header-main">
        <div class="header-name text-ellipsis">{{ merchant.name }}</div>
        <div class="header-meta">
          <van-tag v-if="merchant.businessStatus === 1" type="success" plain>营业中</van-tag>
          <van-tag v-else type="default" plain>休息中</van-tag>
          <span>评分 {{ merchant.rating ?? '-' }}</span>
          <span>月售 {{ merchant.sales ?? 0 }}</span>
        </div>
        <div class="header-meta sub">
          <span>起送 ¥{{ formatAmount(merchant.minAmount) }}</span>
          <span>配送 ¥{{ formatAmount(merchant.deliveryFee) }}</span>
        </div>
      </div>
    </div>

    <!-- 左侧分类锚点 + 右侧菜品列表 -->
    <div v-if="groups.length" class="menu">
      <div class="menu-cate">
        <div
          v-for="group in groups"
          :key="group.categoryId"
          class="cate-item"
          :class="{ active: group.categoryId === activeCategoryId }"
          @click="selectCategory(group.categoryId)"
        >
          {{ group.categoryName }}
        </div>
      </div>

      <div ref="scrollRef" class="menu-dish" @scroll.passive="onScroll">
        <div
          v-for="group in groups"
          :key="group.categoryId"
          class="dish-group"
          :ref="(el) => setGroupRef(group.categoryId, el)"
        >
          <div class="group-title">{{ group.categoryName }}</div>

          <div
            v-for="dish in group.dishes"
            :key="dish.id"
            class="dish-item"
            :class="{ disabled: !isAvailable(dish) }"
          >
            <van-image
              class="dish-img"
              :src="resolveImage(dish.image)"
              width="72"
              height="72"
              radius="6"
              fit="cover"
            >
              <template #error>
                <van-icon name="photo-o" size="24" color="#c8c9cc" />
              </template>
            </van-image>

            <div class="dish-main">
              <div class="dish-name text-ellipsis">{{ dish.name }}</div>
              <div class="dish-desc text-ellipsis">{{ dish.description || '暂无描述' }}</div>
              <div class="dish-sales">
                月售 {{ dish.sales ?? 0 }} · 库存 {{ dish.stock ?? 0 }}
              </div>
              <div class="dish-bottom">
                <span class="dish-price">¥{{ formatAmount(dish.price) }}</span>
                <van-tag v-if="!isAvailable(dish)" type="default">已售罄</van-tag>
                <van-stepper
                  v-else
                  :model-value="quantities[dish.id] || 0"
                  min="0"
                  :max="stepperMax(dish)"
                  integer
                  button-size="22"
                  :disabled="!!changing[dish.id]"
                  @change="(val) => changeQuantity(dish, val)"
                />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <van-empty v-else-if="!loading" description="该店铺暂无上架菜品" />
    <div v-else class="page-loading">
      <van-loading vertical>加载中...</van-loading>
    </div>

    <!-- 底部购物车栏 -->
    <div class="cart-bar">
      <div class="cart-icon" @click="goCart">
        <van-icon
          name="shopping-cart-o"
          size="26"
          :color="totalQuantity > 0 ? '#ee0a24' : '#969799'"
        />
        <van-badge v-if="totalQuantity > 0" :content="totalQuantity" />
      </div>

      <div class="cart-amount">
        <div class="amount">¥{{ formatAmount(totalAmount) }}</div>
        <div v-if="merchant && !reachMinAmount" class="amount-tip">
          还差 ¥{{ formatAmount(lackAmount) }} 起送
        </div>
        <div v-else-if="merchant" class="amount-tip">
          另需配送费 ¥{{ formatAmount(merchant.deliveryFee) }}
        </div>
      </div>

      <van-button
        class="cart-btn"
        type="primary"
        round
        :disabled="totalQuantity === 0 || !reachMinAmount"
        @click="goCheckout"
      >
        去结算
      </van-button>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getMerchantDetail } from '../api/merchant'
import { getMenu } from '../api/dish'
import { addToCart, getCart, updateCartItem } from '../api/cart'
import { useCartStore } from '../stores/cart'
import { formatAmount, resolveImage } from '../utils/format'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()

const merchantId = Number(route.params.id)
const merchant = ref(null)
const groups = ref([])
const loading = ref(true)
const activeCategoryId = ref(null)

const scrollRef = ref(null)
const groupRefs = reactive({})
/** dishId -> 数量 */
const quantities = reactive({})
/** dishId -> 购物车条目 id */
const cartItemIds = reactive({})
/** dishId -> 是否正在提交 */
const changing = reactive({})

/** 菜品是否可选：下架 / 库存为 0 / available=false 均置灰 */
function isAvailable(dish) {
  return dish.available !== false && Number(dish.status) !== 0 && Number(dish.stock || 0) > 0
}

function stepperMax(dish) {
  const stock = Number(dish.stock || 0)
  return stock > 0 ? stock : 99
}

const totalQuantity = computed(() => {
  let n = 0
  groups.value.forEach((g) => {
    g.dishes.forEach((d) => {
      n += quantities[d.id] || 0
    })
  })
  return n
})

const totalAmount = computed(() => {
  let sum = 0
  groups.value.forEach((g) => {
    g.dishes.forEach((d) => {
      sum += (quantities[d.id] || 0) * Number(d.price || 0)
    })
  })
  return sum
})

const minAmount = computed(() => Number(merchant.value?.minAmount || 0))
const reachMinAmount = computed(() => totalAmount.value >= minAmount.value)
const lackAmount = computed(() => Math.max(0, minAmount.value - totalAmount.value))

async function loadAll() {
  loading.value = true
  try {
    const [detail, menu] = await Promise.all([getMerchantDetail(merchantId), getMenu(merchantId)])
    merchant.value = detail
    groups.value = menu || []
    if (groups.value.length) activeCategoryId.value = groups.value[0].categoryId
    await syncCartQuantities()
  } catch (e) {
    // 错误提示已由拦截器统一处理
  } finally {
    loading.value = false
  }
}

/** 从服务端购物车同步当前店铺的数量与条目 id */
async function syncCartQuantities() {
  try {
    const cartGroups = await getCart()
    Object.keys(quantities).forEach((k) => delete quantities[k])
    Object.keys(cartItemIds).forEach((k) => delete cartItemIds[k])
    const shop = (cartGroups || []).find((g) => Number(g.merchantId) === merchantId)
    if (shop) {
      shop.items.forEach((item) => {
        quantities[item.dishId] = item.quantity
        cartItemIds[item.dishId] = item.id
      })
    }
    cartStore.loadCount()
  } catch (e) {
    // 忽略：购物车为空或未登录
  }
}

async function changeQuantity(dish, value) {
  const dishId = dish.id
  if (changing[dishId]) return
  changing[dishId] = true
  const prev = quantities[dishId] || 0
  try {
    if (value <= 0) {
      const cartItemId = cartItemIds[dishId]
      if (cartItemId) await updateCartItem(cartItemId, 0)
      quantities[dishId] = 0
      delete cartItemIds[dishId]
    } else if (!prev) {
      await addToCart({ dishId, quantity: value })
      // 重新拉取以获取购物车条目 id（后续改数量需要）
      await syncCartQuantities()
    } else {
      await updateCartItem(cartItemIds[dishId], value)
      quantities[dishId] = value
    }
    cartStore.loadCount()
  } catch (e) {
    // 错误提示已由拦截器统一处理
  } finally {
    changing[dishId] = false
  }
}

function setGroupRef(categoryId, el) {
  if (el) groupRefs[categoryId] = el
}

function selectCategory(categoryId) {
  activeCategoryId.value = categoryId
  nextTick(() => {
    const container = scrollRef.value
    const el = groupRefs[categoryId]
    if (container && el) container.scrollTo({ top: el.offsetTop, behavior: 'smooth' })
  })
}

/** 滚动右侧列表时高亮左侧对应分类 */
function onScroll() {
  const container = scrollRef.value
  if (!container) return
  const scrollTop = container.scrollTop
  let current = groups.value[0]?.categoryId ?? null
  groups.value.forEach((g) => {
    const el = groupRefs[g.categoryId]
    if (el && el.offsetTop - 12 <= scrollTop) current = g.categoryId
  })
  activeCategoryId.value = current
}

function goCart() {
  router.push('/cart')
}

function goCheckout() {
  router.push({ path: '/order/confirm', query: { merchantId } })
}

function onBack() {
  router.back()
}

onMounted(loadAll)
</script>

<style scoped>
.merchant-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #fff;
}

.merchant-header {
  display: flex;
  padding: 12px;
  background: #fff;
  border-bottom: 1px solid #f2f3f5;
}

.header-logo {
  flex: none;
  background: #f7f8fa;
}

.header-main {
  flex: 1;
  min-width: 0;
  margin-left: 10px;
}

.header-name {
  font-size: 16px;
  font-weight: 600;
}

.header-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
  font-size: 12px;
  color: #646566;
}

.header-meta.sub {
  color: #969799;
}

.menu {
  flex: 1;
  min-height: 0;
  display: flex;
  overflow: hidden;
}

.menu-cate {
  width: 88px;
  flex: none;
  overflow-y: auto;
  background: #f7f8fa;
}

.cate-item {
  padding: 14px 8px;
  font-size: 13px;
  color: #646566;
  text-align: center;
  word-break: break-all;
}

.cate-item.active {
  background: #fff;
  color: #ee0a24;
  font-weight: 600;
}

.menu-dish {
  flex: 1;
  overflow-y: auto;
  padding: 0 12px;
}

.group-title {
  padding: 10px 0 6px;
  font-size: 13px;
  font-weight: 600;
  color: #323233;
}

.dish-item {
  display: flex;
  padding: 10px 0;
  border-bottom: 1px solid #f7f8fa;
}

.dish-item.disabled {
  opacity: 0.5;
}

.dish-img {
  flex: none;
  background: #f7f8fa;
}

.dish-main {
  flex: 1;
  min-width: 0;
  margin-left: 10px;
  display: flex;
  flex-direction: column;
}

.dish-name {
  font-size: 14px;
  font-weight: 500;
}

.dish-desc,
.dish-sales {
  font-size: 12px;
  color: #969799;
  margin-top: 2px;
}

.dish-bottom {
  margin-top: auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.dish-price {
  color: #ee0a24;
  font-size: 15px;
  font-weight: 600;
}

.page-loading {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cart-bar {
  display: flex;
  align-items: center;
  height: 52px;
  padding: 0 12px;
  background: #fff;
  border-top: 1px solid #f2f3f5;
}

.cart-icon {
  position: relative;
  width: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cart-amount {
  flex: 1;
  min-width: 0;
  margin-left: 8px;
}

.amount {
  font-size: 17px;
  font-weight: 600;
  color: #ee0a24;
}

.amount-tip {
  font-size: 11px;
  color: #969799;
}

.cart-btn {
  width: 110px;
}
</style>