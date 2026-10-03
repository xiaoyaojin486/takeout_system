<template>
  <div class="page-container">
    <el-alert
      v-if="overview.pendingAuditCount > 0"
      class="mb-16"
      type="warning"
      :closable="false"
      show-icon
    >
      <template #default>
        <div class="pending-alert">
          <span>当前有 {{ overview.pendingAuditCount }} 家商家等待审核</span>
          <el-button type="warning" size="small" @click="goAudit">去审核</el-button>
        </div>
      </template>
    </el-alert>

    <el-row v-loading="loading" :gutter="16">
      <el-col v-for="card in statCards" :key="card.key" :xs="12" :sm="8" :md="6">
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

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>商家成交排行（近 {{ rankDays }} 天 TOP{{ rankLimit }}）</span>
          <div class="card-header-actions">
            <el-select v-model="rankDays" size="small" style="width: 130px" @change="loadRank">
              <el-option label="近 7 天" :value="7" />
              <el-option label="近 30 天" :value="30" />
              <el-option label="近 90 天" :value="90" />
            </el-select>
            <el-button size="small" :icon="Refresh" @click="loadAll">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table
        v-loading="rankLoading"
        :data="rankList"
        border
        stripe
        empty-text="暂无成交数据"
      >
        <el-table-column label="排名" width="80" align="center">
          <template #default="{ $index }">
            <el-tag v-if="$index === 0" type="danger" effect="dark">NO.1</el-tag>
            <el-tag v-else-if="$index === 1" type="warning" effect="dark">NO.2</el-tag>
            <el-tag v-else-if="$index === 2" type="primary" effect="dark">NO.3</el-tag>
            <span v-else>{{ $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="merchantName" label="商家名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="订单数" width="120" align="center">
          <template #default="{ row }">{{ row.orderCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="成交金额（元）" width="160" align="right">
          <template #default="{ row }">{{ Number(row.amount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column :label="`日均成交（元）`" width="160" align="right">
          <template #default="{ row }">
            {{ (Number(row.amount || 0) / rankDays).toFixed(2) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import { getAdminOverview, getAdminMerchantRank } from '@/api/stats'

const router = useRouter()

const overview = reactive({
  customerCount: 0,
  merchantCount: 0,
  pendingAuditCount: 0,
  todayOrderCount: 0,
  todayAmount: 0,
  totalOrderCount: 0,
  totalAmount: 0,
  canceledCount: 0
})

const loading = ref(false)
const rankLoading = ref(false)
const rankDays = ref(30)
const rankLimit = 10
const rankList = ref([])

const statCards = computed(() => [
  {
    key: 'customerCount',
    label: '顾客总数',
    value: overview.customerCount ?? 0,
    icon: 'User',
    color: '#409eff'
  },
  {
    key: 'merchantCount',
    label: '商家总数',
    value: overview.merchantCount ?? 0,
    icon: 'Shop',
    color: '#67c23a'
  },
  {
    key: 'pendingAuditCount',
    label: '待审核商家',
    value: overview.pendingAuditCount ?? 0,
    icon: 'Stamp',
    color: '#e6a23c'
  },
  {
    key: 'todayOrderCount',
    label: '今日订单数',
    value: overview.todayOrderCount ?? 0,
    icon: 'ShoppingCart',
    color: '#909399'
  },
  {
    key: 'todayAmount',
    label: '今日成交额（元）',
    value: Number(overview.todayAmount || 0).toFixed(2),
    icon: 'Money',
    color: '#f56c6c'
  },
  {
    key: 'totalOrderCount',
    label: '累计订单数',
    value: overview.totalOrderCount ?? 0,
    icon: 'Tickets',
    color: '#00b5ad'
  },
  {
    key: 'totalAmount',
    label: '累计成交额（元）',
    value: Number(overview.totalAmount || 0).toFixed(2),
    icon: 'TrendCharts',
    color: '#7c4dff'
  },
  {
    key: 'canceledCount',
    label: '累计取消订单',
    value: overview.canceledCount ?? 0,
    icon: 'CircleClose',
    color: '#c0c4cc'
  }
])

async function loadOverview() {
  loading.value = true
  try {
    const data = await getAdminOverview()
    Object.assign(overview, data || {})
  } catch (e) {
    /* 已统一提示 */
  } finally {
    loading.value = false
  }
}

async function loadRank() {
  rankLoading.value = true
  try {
    const data = await getAdminMerchantRank(rankDays.value, rankLimit)
    rankList.value = Array.isArray(data) ? data : []
  } catch (e) {
    rankList.value = []
  } finally {
    rankLoading.value = false
  }
}

function loadAll() {
  loadOverview()
  loadRank()
}

function goAudit() {
  router.push('/admin/merchant-audit')
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

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
}

.stat-value {
  font-size: 20px;
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