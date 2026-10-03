<template>
  <div class="page order-list-page">
    <van-nav-bar title="我的订单" fixed placeholder />

    <van-tabs v-model:active="activeStatus" sticky @change="onTabChange">
      <van-tab v-for="tab in ORDER_TABS" :key="tab.name" :name="tab.name" :title="tab.title" />
    </van-tabs>

    <van-list
      v-model:loading="loading"
      :finished="finished"
      finished-text="没有更多了"
      @load="onLoad"
    >
      <div v-for="order in list" :key="order.id" class="order-card" @click="goDetail(order.id)">
        <div class="order-head">
          <div class="order-shop text-ellipsis">
            <van-icon name="shop-o" />
            <span>{{ order.merchantName }}</span>
          </div>
          <van-tag :type="orderStatusTagType(order.status)">
            {{ orderStatusText(order.status, order.statusDesc) }}
          </van-tag>
        </div>

        <div class="order-items">
          <div v-for="(item, idx) in order.items || []" :key="idx" class="order-item">
            <van-image
              class="order-item-img"
              :src="resolveImage(item.dishImage)"
              width="48"
              height="48"
              radius="6"
              fit="cover"
            >
              <template #error>
                <van-icon name="photo-o" size="18" color="#c8c9cc" />
              </template>
            </van-image>
            <div class="order-item-main">
              <div class="text-ellipsis">{{ item.dishName }}</div>
              <div class="order-item-sub">x{{ item.quantity }}</div>
            </div>
            <span class="order-item-amount">¥{{ formatAmount(item.amount) }}</span>
          </div>
        </div>

        <div class="order-foot">
          <span class="order-count">共 {{ order.itemCount ?? 0 }} 件</span>
          <span class="order-amount">实付 <em>¥{{ formatAmount(order.payAmount) }}</em></span>
        </div>

        <div class="order-actions" @click.stop>
          <van-button
            v-if="Number(order.status) === 10"
            size="small"
            round
            type="primary"
            @click="goPay(order)"
          >
            去支付
          </van-button>
          <van-button
            v-if="[10, 20, 30].includes(Number(order.status))"
            size="small"
            round
            @click="onCancel(order)"
          >
            取消订单
          </van-button>
          <van-button
            v-if="Number(order.status) === 40"
            size="small"
            round
            type="primary"
            @click="onConfirm(order)"
          >
            确认收货
          </van-button>
        </div>
      </div>

      <van-empty v-if="!loading && !list.length" description="暂无相关订单" />
    </van-list>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import { cancelOrder, confirmOrder, getOrderList } from '../api/order'
import { payOrder } from '../api/payment'
import {
  ORDER_TABS,
  formatAmount,
  orderStatusTagType,
  orderStatusText,
  resolveImage
} from '../utils/format'

const router = useRouter()

const activeStatus = ref('all')
const list = ref([])
const loading = ref(false)
const finished = ref(false)
const pageNum = ref(1)
const pageSize = 10

async function fetchList() {
  try {
    const res = await getOrderList({
      pageNum: pageNum.value,
      pageSize,
      // 全部 tab 不传 status
      status: activeStatus.value === 'all' ? undefined : Number(activeStatus.value)
    })
    const records = res?.list || []
    list.value.push(...records)
    pageNum.value += 1
    const total = Number(res?.total ?? 0)
    finished.value = records.length === 0 || list.value.length >= total
  } catch (e) {
    // 错误提示已由拦截器统一处理
    finished.value = true
  } finally {
    loading.value = false
  }
}

function onLoad() {
  fetchList()
}

function onTabChange() {
  list.value = []
  pageNum.value = 1
  finished.value = false
  loading.value = true
  fetchList()
}

function reset() {
  list.value = []
  pageNum.value = 1
  finished.value = false
}

async function goPay(order) {
  try {
    await payOrder(order.orderNo, {})
    showToast('支付成功')
    reset()
    loading.value = true
    fetchList()
  } catch (e) {
    // 错误提示已由拦截器统一处理
  }
}

async function onCancel(order) {
  try {
    await showConfirmDialog({ title: '取消订单', message: '确定取消该订单吗？' })
  } catch (e) {
    return
  }
  try {
    await cancelOrder(order.id, '用户主动取消')
    showToast('订单已取消')
    reset()
    loading.value = true
    fetchList()
  } catch (e) {
    // 错误提示已由拦截器统一处理
  }
}

async function onConfirm(order) {
  try {
    await showConfirmDialog({ title: '确认收货', message: '确认已收到商品吗？' })
  } catch (e) {
    return
  }
  try {
    await confirmOrder(order.id)
    showToast('已确认收货')
    reset()
    loading.value = true
    fetchList()
  } catch (e) {
    // 错误提示已由拦截器统一处理
  }
}

function goDetail(id) {
  router.push(`/order/${id}`)
}
</script>

<style scoped>
.order-list-page {
  background: #f7f8fa;
}

.order-card {
  margin: 10px;
  padding: 12px;
  background: #fff;
  border-radius: 10px;
}

.order-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.order-shop {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 15px;
  font-weight: 600;
}

.order-items {
  margin-top: 10px;
}

.order-item {
  display: flex;
  align-items: center;
  padding: 6px 0;
}

.order-item-img {
  flex: none;
  background: #f7f8fa;
}

.order-item-main {
  flex: 1;
  min-width: 0;
  margin-left: 8px;
  font-size: 14px;
}

.order-item-sub {
  font-size: 12px;
  color: #969799;
}

.order-item-amount {
  font-size: 13px;
  color: #646566;
}

.order-foot {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #f7f8fa;
  font-size: 13px;
  color: #646566;
}

.order-amount em {
  color: #ee0a24;
  font-style: normal;
  font-size: 16px;
  font-weight: 600;
}

.order-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 10px;
}
</style>