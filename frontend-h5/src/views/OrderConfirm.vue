<template>
  <div class="confirm-page">
    <van-nav-bar title="确认订单" left-arrow @click-left="onBack" />

    <!-- 收货地址 -->
    <div class="address-card" @click="openAddress">
      <van-icon name="location-o" size="20" color="#ee0a24" />
      <div v-if="selectedAddress" class="address-main">
        <div class="address-line">
          <span class="receiver">{{ selectedAddress.receiver }}</span>
          <span class="phone">{{ selectedAddress.phone }}</span>
          <van-tag v-if="selectedAddress.isDefault" type="primary" plain>默认</van-tag>
        </div>
        <div class="address-detail">{{ fullAddressOf(selectedAddress) }}</div>
      </div>
      <div v-else class="address-main empty">请选择收货地址</div>
      <van-icon name="arrow" color="#969799" />
    </div>

    <!-- 商品清单 -->
    <van-cell-group v-if="group" inset title="商品清单">
      <van-cell
        v-for="item in group.items"
        :key="item.id"
        :title="item.dishName"
        :label="`x${item.quantity}`"
        :value="`¥${formatAmount(item.amount)}`"
      />
      <van-cell v-if="!group.items || !group.items.length" title="购物车为空" />
    </van-cell-group>

    <!-- 备注 -->
    <van-cell-group inset title="订单备注">
      <van-field
        v-model="remark"
        rows="2"
        autosize
        type="textarea"
        maxlength="100"
        placeholder="口味、偏好等要求（选填）"
        show-word-limit
      />
    </van-cell-group>

    <!-- 金额明细 -->
    <van-cell-group v-if="group" inset title="金额明细">
      <van-cell title="商品金额" :value="`¥${formatAmount(totalAmount)}`" />
      <van-cell title="配送费" :value="`¥${formatAmount(deliveryFee)}`" />
      <van-cell title="实付金额" :value="`¥${formatAmount(payAmount)}`" />
    </van-cell-group>

    <div class="submit-bar">
      <div class="pay-amount">实付 <em>¥{{ formatAmount(payAmount) }}</em></div>
      <van-button
        type="primary"
        round
        :loading="submitting"
        loading-text="提交中..."
        :disabled="!canSubmit"
        @click="onSubmit"
      >
        提交订单
      </van-button>
    </div>

    <!-- 地址选择弹层 -->
    <van-popup v-model:show="addressPopup" position="bottom" round>
      <div class="popup-title">选择收货地址</div>
      <div class="popup-list">
        <van-cell
          v-for="addr in addresses"
          :key="addr.id"
          :title="addr.receiver"
          :label="fullAddressOf(addr)"
          clickable
          @click="chooseAddress(addr)"
        >
          <template #value>
            <van-icon
              v-if="selectedAddress && selectedAddress.id === addr.id"
              name="success"
              color="#ee0a24"
            />
          </template>
        </van-cell>
        <van-empty v-if="!addresses.length" description="暂无收货地址" />
      </div>
      <div class="popup-foot">
        <van-button block plain type="primary" @click="goAddressEdit">新增收货地址</van-button>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getCart } from '../api/cart'
import { getAddressList } from '../api/address'
import { createOrder } from '../api/order'
import { useCartStore } from '../stores/cart'
import { formatAmount, uuid } from '../utils/format'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()

const merchantId = Number(route.query.merchantId)

const group = ref(null)
const addresses = ref([])
const selectedAddress = ref(null)
const remark = ref('')
const addressPopup = ref(false)
const submitting = ref(false)

const totalAmount = computed(() => Number(group.value?.totalAmount || 0))
const deliveryFee = computed(() => Number(group.value?.deliveryFee || 0))
const payAmount = computed(() => totalAmount.value + deliveryFee.value)

const canSubmit = computed(
  () =>
    !!selectedAddress.value &&
    !!group.value &&
    group.value.items &&
    group.value.items.length > 0 &&
    group.value.reachMinAmount !== false
)

function fullAddressOf(addr) {
  if (!addr) return ''
  if (addr.fullAddress) return addr.fullAddress
  return `${addr.province || ''}${addr.city || ''}${addr.district || ''}${addr.detail || ''}`
}

async function loadGroup() {
  try {
    const cartGroups = await getCart()
    group.value = (cartGroups || []).find((g) => Number(g.merchantId) === merchantId) || null
  } catch (e) {
    // 错误提示已由拦截器统一处理
  }
}

async function loadAddresses() {
  try {
    const list = await getAddressList()
    addresses.value = list || []
    // 默认选中默认地址（后端已把默认地址排在最前）
    if (!selectedAddress.value && addresses.value.length) {
      selectedAddress.value = addresses.value.find((a) => a.isDefault) || addresses.value[0]
    }
  } catch (e) {
    // 错误提示已由拦截器统一处理
  }
}

function openAddress() {
  // 每次打开都刷新一次，保证从新增地址页返回后数据最新
  loadAddresses()
  addressPopup.value = true
}

function chooseAddress(addr) {
  selectedAddress.value = addr
  addressPopup.value = false
}

function goAddressEdit() {
  addressPopup.value = false
  router.push('/address/edit')
}

async function onSubmit() {
  if (!selectedAddress.value) {
    showToast('请选择收货地址')
    return
  }
  if (!group.value || !group.value.items || !group.value.items.length) {
    showToast('购物车为空，无法下单')
    return
  }

  submitting.value = true
  try {
    const payload = {
      addressId: selectedAddress.value.id,
      remark: remark.value.trim() || undefined,
      // 只传菜品 id 与数量，金额由后端重算
      items: group.value.items.map((item) => ({ dishId: item.dishId, quantity: item.quantity }))
    }
    // 一次性幂等 token，防止重复下单
    const res = await createOrder(payload, uuid())
    await cartStore.loadCount()
    showToast('下单成功')
    router.replace(`/order/${res.orderId}`)
  } catch (e) {
    // 错误提示已由拦截器统一处理
  } finally {
    submitting.value = false
  }
}

function onBack() {
  router.back()
}

onMounted(async () => {
  await Promise.all([loadGroup(), loadAddresses()])
})
</script>

<style scoped>
.confirm-page {
  min-height: 100vh;
  padding-bottom: 72px;
  box-sizing: border-box;
  background: #f7f8fa;
}

.address-card {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 10px;
  padding: 14px 12px;
  background: #fff;
  border-radius: 10px;
}

.address-main {
  flex: 1;
  min-width: 0;
}

.address-main.empty {
  color: #969799;
  font-size: 14px;
}

.address-line {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
}

.phone {
  font-size: 13px;
  font-weight: 400;
  color: #646566;
}

.address-detail {
  margin-top: 4px;
  font-size: 13px;
  color: #646566;
}

.submit-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 12px;
  background: #fff;
  border-top: 1px solid #f2f3f5;
}

.pay-amount {
  font-size: 14px;
}

.pay-amount em {
  color: #ee0a24;
  font-style: normal;
  font-size: 18px;
  font-weight: 600;
}

.popup-title {
  padding: 16px;
  font-size: 16px;
  font-weight: 600;
  text-align: center;
}

.popup-list {
  max-height: 50vh;
  overflow-y: auto;
}

.popup-foot {
  padding: 12px 16px 24px;
}
</style>