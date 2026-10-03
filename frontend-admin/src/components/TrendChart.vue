<template>
  <div ref="chartRef" :style="{ width: '100%', height: height }"></div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'

/**
 * 趋势折线图
 * data: [{ statDate, orderCount, amount }]（后端已补零，直接画）
 */
const props = defineProps({
  data: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  height: {
    type: String,
    default: '340px'
  },
  amountName: {
    type: String,
    default: '营业额（元）'
  },
  countName: {
    type: String,
    default: '订单数（单）'
  }
})

const chartRef = ref(null)
let chartInstance = null

function buildOption() {
  const dates = props.data.map((item) => item.statDate)
  const amounts = props.data.map((item) => Number(item.amount || 0))
  const counts = props.data.map((item) => Number(item.orderCount || 0))

  return {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: [props.amountName, props.countName],
      top: 0
    },
    grid: {
      left: 50,
      right: 50,
      top: 48,
      bottom: 40
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates,
      axisLabel: { color: '#909399' }
    },
    yAxis: [
      {
        type: 'value',
        name: '营业额',
        axisLabel: { color: '#909399' },
        splitLine: { lineStyle: { type: 'dashed' } }
      },
      {
        type: 'value',
        name: '订单数',
        minInterval: 1,
        axisLabel: { color: '#909399' },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: props.amountName,
        type: 'line',
        smooth: true,
        symbolSize: 6,
        data: amounts,
        itemStyle: { color: '#409eff' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64,158,255,0.35)' },
            { offset: 1, color: 'rgba(64,158,255,0.02)' }
          ])
        }
      },
      {
        name: props.countName,
        type: 'line',
        smooth: true,
        symbolSize: 6,
        yAxisIndex: 1,
        data: counts,
        itemStyle: { color: '#67c23a' }
      }
    ]
  }
}

function renderChart() {
  if (!chartInstance) return
  chartInstance.setOption(buildOption(), true)
}

function handleResize() {
  chartInstance?.resize()
}

onMounted(() => {
  chartInstance = echarts.init(chartRef.value)
  renderChart()
  window.addEventListener('resize', handleResize)
})

watch(
  () => props.data,
  () => {
    renderChart()
  },
  { deep: true }
)

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
  chartInstance = null
})
</script>