<template>
  <div class="page-container">
    <el-alert
      v-if="overview.pendingAcceptCount > 0"
      class="mb-16"
      type="warning"
      :closable="false"
      show-icon
      :title="`您有 ${overview.pendingAcceptCount} 个订单待接单，请尽快处理`"
    >
      <template #default>
        <div class="pending-alert">
          <span>您有 {{ overview.pendingAcceptCount }} 个订单待接单，请尽快处理</span>
          <el-button type="warning" size="small" @click="goPendingOrders">
            去接单
          </el-button>
        </div>
      </template>
    </el-alert>

    <el-row v-loading="overviewLoading" :gutter="16">
      <el-col v-for="card in statCards" :key="card.key" :xs="12" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-body">
            <div class="stat-icon" :style="{ backgroundColor: card.color }">
              <el-icon :size="22"><component :is="card.icon" /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-label">{{ card.label }}</div>
              <div class="stat-value">{{ card.value }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="chart-card">
      <template #header>
        <div class="card-header">
          <span>近 {{ trendDays }} 日营业额 / 订单数趋势</span>
          <div class="card-header-actions">
            <el-radio-group v-model="trendDays" size="small" @change="loadTrend">
              <el-radio-button :value="7">近 7 日</el-radio-button>
              <el-radio-button :value="15">近 15 日</el-radio-button>
              <el-radio-button :value="30">近 30 日</el-radio-button>
            </el-radio-group>
            <el-button size="small" :icon="Refresh" @click="loadAll">刷新</el-button>
          </div>
        </div>
      </template>
      <div v-loading="trendLoading">
        <TrendChart v-if="trendData.length" :data="trendData" height="340px" />
        <el-empty v-else description="暂无趋势数据" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import TrendChart from '@/components/TrendChart.vue'
import { getMerchantOverview, getMerchantTrend } from '@/api/stats'

const router = useRouter()

const overview = reactive({
  merchantName: '',
  businessStatus: 0,
  todayOrderCount: 0,
  todayAmount: 0,
  monthOrderCount: 0,
  monthAmount: 0,
  pendingAcceptCount: 0,
  pendingPaymentCount: 0
})

const trendDays = ref(7)
const trendData = ref([])
const trendLoading = ref(false)
const overviewLoading = ref(false)

const statCards = computed(() => [
  {
    key: 'todayAmount',
    label: '今日营业额（元）',
    value: formatAmount(overview.todayAmount),
    icon: 'Money',
    color: '#409eff'
  },
  {
    key: 'todayOrderCount',
    label: '今日订单数',
    value: overview.todayOrderCount ?? 0,
    icon: 'ShoppingCart',
    color: '#67c23a'
  },
  {
    key: 'pendingAcceptCount',
    label: '待接单',
    value: overview.pendingAcceptCount ?? 0,
    icon: 'Bell',
    color: '#e6a23c'
  },
  {
    key: 'monthAmount',
    label: '本月营业额（元）',
    value: formatAmount(overview.monthAmount),
    icon: 'TrendCharts',
    color: '#909399'
  }
])

function formatAmount(value) {
  const num = Number(value || 0)
  return num.toFixed(2)
}

async function loadOverview() {
  overviewLoading.value = true
  try {
    const data = await getMerchantOverview()
    Object.assign(overview, data || {})
  } catch (e) {
    /* 已统一提示 */
  } finally {
    overviewLoading.value = false
  }
}

async function loadTrend() {
  trendLoading.value = true
  try {
    const data = await getMerchantTrend(trendDays.value)
    trendData.value = Array.isArray(data) ? data : []
  } catch (e) {
    trendData.value = []
  } finally {
    trendLoading.value = false
  }
}

function loadAll() {
  loadOverview()
  loadTrend()
}

function goPendingOrders() {
  router.push({ path: '/merchant/order', query: { status: 20 } })
}

onMounted(() => {
  loadAll()
})
</script>

<style scoped>
.pending-alert {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-body {
  display: flex;
  align-items: center;
  gap: 14px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.stat-content {
  min-width: 0;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
}

.stat-value {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>