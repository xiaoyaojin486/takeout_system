<template>
  <div class="page-container">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-header">
          <span>经营概览</span>
          <el-button :icon="Refresh" @click="loadAll">刷新</el-button>
        </div>
      </template>
      <div v-loading="overviewLoading">
        <el-row :gutter="16">
          <el-col v-for="card in statCards" :key="card.key" :xs="12" :sm="8" :md="4">
            <div class="mini-stat">
              <div class="mini-label">{{ card.label }}</div>
              <div class="mini-value">{{ card.value }}</div>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <el-card shadow="never" class="chart-card">
      <template #header>
        <div class="card-header">
          <span>营业额 / 订单数趋势</span>
          <el-radio-group v-model="trendDays" size="small" @change="loadTrend">
            <el-radio-button :value="7">近 7 日</el-radio-button>
            <el-radio-button :value="15">近 15 日</el-radio-button>
            <el-radio-button :value="30">近 30 日</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div v-loading="trendLoading">
        <TrendChart v-if="trendData.length" :data="trendData" height="340px" />
        <el-empty v-else description="暂无趋势数据" />
      </div>
    </el-card>

    <el-card shadow="never" class="chart-card">
      <template #header>
        <div class="card-header">
          <span>热销菜品 TOP5（近 {{ hotDays }} 天）</span>
          <el-select v-model="hotDays" size="small" style="width: 130px" @change="loadHotDish">
            <el-option label="近 7 天" :value="7" />
            <el-option label="近 30 天" :value="30" />
            <el-option label="近 90 天" :value="90" />
          </el-select>
        </div>
      </template>
      <div v-loading="hotLoading">
        <div v-show="hotDish.length" ref="hotChartRef" style="width: 100%; height: 320px"></div>
        <el-empty v-if="!hotDish.length && !hotLoading" description="暂无热销菜品数据" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import TrendChart from '@/components/TrendChart.vue'
import {
  getMerchantOverview,
  getMerchantTrend,
  getMerchantHotDish
} from '@/api/stats'

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

const overviewLoading = ref(false)
const trendLoading = ref(false)
const hotLoading = ref(false)

const trendDays = ref(7)
const trendData = ref([])

const hotDays = ref(30)
const hotDish = ref([])

const hotChartRef = ref(null)
let hotChart = null

const statCards = computed(() => [
  { key: 'todayAmount', label: '今日营业额（元）', value: amount(overview.todayAmount) },
  { key: 'todayOrderCount', label: '今日订单数', value: overview.todayOrderCount ?? 0 },
  { key: 'monthAmount', label: '本月营业额（元）', value: amount(overview.monthAmount) },
  { key: 'monthOrderCount', label: '本月订单数', value: overview.monthOrderCount ?? 0 },
  { key: 'pendingAcceptCount', label: '待接单', value: overview.pendingAcceptCount ?? 0 },
  { key: 'pendingPaymentCount', label: '待支付', value: overview.pendingPaymentCount ?? 0 }
])

function amount(value) {
  return Number(value || 0).toFixed(2)
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

async function loadHotDish() {
  hotLoading.value = true
  try {
    const data = await getMerchantHotDish(hotDays.value, 5)
    hotDish.value = Array.isArray(data) ? data : []
  } catch (e) {
    hotDish.value = []
  } finally {
    hotLoading.value = false
  }
}

/** 热销 TOP5 横向柱状图 */
function renderHotChart() {
  if (!hotChartRef.value) return
  if (!hotChart) {
    hotChart = echarts.init(hotChartRef.value)
  }
  // 横向柱状图：数量最多的显示在最上方
  const sorted = [...hotDish.value].sort(
    (a, b) => Number(a.quantity || 0) - Number(b.quantity || 0)
  )
  hotChart.setOption(
    {
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        formatter: (params) => {
          const item = params[0]
          const row = sorted[item.dataIndex] || {}
          return `${item.name}<br/>销量：${row.quantity ?? 0}<br/>金额：${Number(
            row.amount || 0
          ).toFixed(2)} 元`
        }
      },
      grid: { left: 100, right: 60, top: 20, bottom: 20 },
      xAxis: {
        type: 'value',
        name: '销量（份）',
        minInterval: 1,
        splitLine: { lineStyle: { type: 'dashed' } }
      },
      yAxis: {
        type: 'category',
        data: sorted.map((item) => item.dishName)
      },
      series: [
        {
          name: '销量',
          type: 'bar',
          barMaxWidth: 22,
          data: sorted.map((item) => Number(item.quantity || 0)),
          itemStyle: { color: '#409eff', borderRadius: [0, 4, 4, 0] },
          label: { show: true, position: 'right' }
        }
      ]
    },
    true
  )
}

function handleResize() {
  hotChart?.resize()
}

function loadAll() {
  loadOverview()
  loadTrend()
  loadHotDish()
}

watch(hotDish, async () => {
  await nextTick()
  renderHotChart()
})

onMounted(() => {
  loadAll()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  hotChart?.dispose()
  hotChart = null
})
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.mini-stat {
  padding: 8px 0;
}

.mini-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
}

.mini-value {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}
</style>