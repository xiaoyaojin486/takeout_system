<template>
  <div class="order-detail-page">
    <van-nav-bar title="订单详情" left-arrow @click-left="onBack" />

    <div v-if="loading" class="page-loading">
      <van-loading vertical>加载中...</van-loading>
    </div>

    <van-empty v-else-if="!order" description="订单不存在" />

    <div v-else>
      <!-- 状态区 -->
      <div class="status-card">
        <div class="status-title">{{ orderStatusText(order.status, order.statusDesc) }}</div>
        <div class="status-sub">
          订单号 {{ order.orderNo }} · 下单时间 {{ order.createTime || '-' }}
        </div>
      </div>

      <!-- 状态时间轴 -->
      <van-cell-group v-if="steps.length" inset title="订单进度">
        <div class="steps-wrap">
          <van-steps direction="vertical" :active="steps.length - 1">
            <van-step v-for="(step, idx) in steps" :key="idx">
              <div class="step-title">{{ step.title }}</div>
              <div class="step-desc">{{ step.time }}</div>
              <div v-if="step.remark" class="step-desc">{{ step.remark }}</div>
            </van-step>
          </van-steps>
        </div>
      </van-cell-group>

      <!-- 收货信息 -->
      <van-cell-group inset title="收货信息">
        <van-cell title="收货人" :value="order.receiver" />
        <van-cell title="联系电话" :value="order.receiverPhone" />
        <van-cell title="收货地址" :label="order.receiverAddress" />
      </van-cell-group>

      <!-- 商品清单 -->
      <van-cell-group inset title="商品清单">
        <van-cell
          v-for="(item, idx) in order.items || []"
          :key="idx"
          :title="item.dishName"
          :label="`x${item.quantity}`"
          :value="`¥${formatAmount(item.amount)}`"
        />
      </van-cell-group>

      <!-- 金额明细 -->
      <van-cell-group inset title="金额明细">
        <van-cell title="商品金额" :value="`¥${formatAmount(order.totalAmount)}`" />
        <van-cell title="配送费" :value="`¥${formatAmount(order.deliveryFee)}`" />
        <van-cell title="实付金额" :value="`¥${formatAmount(order.payAmount)}`" />
      </van-cell-group>

      <!-- 其他信息 -->
      <van-cell-group inset title="其他信息">
        <van-cell title="备注" :value="order.remark || '无'" />
        <van-cell v-if="order.cancelReason" title="取消原因" :value="order.cancelReason" />
        <van-cell v-if="order.payTime" title="支付时间" :value="order.payTime" />
        <van-cell v-if="order.finishTime" title="完成时间" :value="order.finishTime" />
      </van-cell-group>

      <div class="detail-actions">
        <van-button
          v-if="Number(order.status) === 10"
          type="primary"
          round
          block
          :loading="paying"
          @click="onPay"
        >
          去支付
        </van-button>
        <van-button
          v-if="[10, 20, 30].includes(Number(order.status))"
          round
          block
          plain
          @click="onCancel"
        >
          取消订单
        </van-button>
        <van-button
          v-if="Number(order.status) === 40"
          type="primary"
          round
          block
          @click="onConfirm"
        >
          确认收货
        </van-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import { cancelOrder, confirmOrder, getOrderDetail } from '../api/order'
import { payOrder } from '../api/payment'
import { formatAmount, orderStatusText } from '../utils/format'

const route = useRoute()
const router = useRouter()

const orderId = route.params.id
const detail = ref(null)
const loading = ref(true)
const paying = ref(false)

const order = computed(() => detail.value?.order || null)

/** 由状态日志生成时间轴（按时间正序） */
const steps = computed(() => {
  const logs = detail.value?.statusLogs || []
  return logs
    .map((log) => ({
      title: log.toStatusDesc || orderStatusText(log.toStatus),
      time: log.createTime || '',
      remark: log.remark || ''
    }))
    .sort((a, b) => String(a.time).localeCompare(String(b.time)))
})

async function loadDetail() {
  loading.value = true
  try {
    detail.value = await getOrderDetail(orderId)
  } catch (e) {
    // 错误提示已由拦截器统一处理
  } finally {
    loading.value = false
  }
}

async function onPay() {
  if (!order.value) return
  paying.value = true
  try {
    await payOrder(order.value.orderNo, {})
    showToast('支付成功')
    await loadDetail()
  } catch (e) {
    // 错误提示已由拦截器统一处理
  } finally {
    paying.value = false
  }
}

async function onCancel() {
  try {
    await showConfirmDialog({ title: '取消订单', message: '确定取消该订单吗？' })
  } catch (e) {
    return
  }
  try {
    await cancelOrder(orderId, '用户主动取消')
    showToast('订单已取消')
    await loadDetail()
  } catch (e) {
    // 错误提示已由拦截器统一处理
  }
}

async function onConfirm() {
  try {
    await showConfirmDialog({ title: '确认收货', message: '确认已收到商品吗？' })
  } catch (e) {
    return
  }
  try {
    await confirmOrder(orderId)
    showToast('已确认收货')
    await loadDetail()
  } catch (e) {
    // 错误提示已由拦截器统一处理
  }
}

function onBack() {
  router.back()
}

onMounted(loadDetail)
</script>

<style scoped>
.order-detail-page {
  min-height: 100vh;
  padding-bottom: 24px;
  box-sizing: border-box;
  background: #f7f8fa;
}

.page-loading {
  display: flex;
  justify-content: center;
  padding-top: 80px;
}

.status-card {
  padding: 18px 16px;
  background: linear-gradient(135deg, #ff6034, #ee0a24);
  color: #fff;
}

.status-title {
  font-size: 20px;
  font-weight: 600;
}

.status-sub {
  margin-top: 6px;
  font-size: 12px;
  opacity: 0.9;
}

.steps-wrap {
  padding: 4px 8px;
}

.step-title {
  font-size: 14px;
  color: #323233;
}

.step-desc {
  margin-top: 2px;
  font-size: 12px;
  color: #969799;
}

.detail-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin: 16px;
}
</style>