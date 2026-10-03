<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>全平台订单总览</span>
          <el-button :icon="Refresh" @click="loadList">刷新</el-button>
        </div>
      </template>

      <div class="filter-bar">
        <el-select
          v-model="query.merchantId"
          placeholder="全部商家"
          clearable
          filterable
          @change="handleSearch"
        >
          <el-option
            v-for="item in merchantOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable @change="handleSearch">
          <el-option
            v-for="item in ORDER_STATUS_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="list"
        border
        stripe
        empty-text="暂无订单数据"
      >
        <el-table-column prop="orderNo" label="订单号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="merchantName" label="所属商家" min-width="140" show-overflow-tooltip />
        <el-table-column label="收货人" min-width="160">
          <template #default="{ row }">
            <div>{{ row.receiver }} {{ row.receiverPhone }}</div>
            <div class="text-muted address">{{ row.receiverAddress }}</div>
          </template>
        </el-table-column>
        <el-table-column label="件数" width="80" align="center">
          <template #default="{ row }">{{ row.itemCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="实付（元）" width="110" align="right">
          <template #default="{ row }">{{ Number(row.payAmount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="orderStatusTag(row.status)">
              {{ row.statusDesc || orderStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="170" />
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-pagination">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </el-card>

    <!-- 订单详情抽屉 -->
    <el-drawer v-model="detailVisible" title="订单详情" size="620px">
      <div v-loading="detailLoading">
        <template v-if="detail.order">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="订单号" :span="2">
              {{ detail.order.orderNo }}
            </el-descriptions-item>
            <el-descriptions-item label="所属商家" :span="2">
              {{ detail.order.merchantName }}
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="orderStatusTag(detail.order.status)">
                {{ detail.order.statusDesc || orderStatusText(detail.order.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="下单时间">
              {{ detail.order.createTime || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="收货人" :span="2">
              {{ detail.order.receiver }} {{ detail.order.receiverPhone }}
            </el-descriptions-item>
            <el-descriptions-item label="收货地址" :span="2">
              {{ detail.order.receiverAddress }}
            </el-descriptions-item>
            <el-descriptions-item label="商品金额">
              {{ Number(detail.order.totalAmount || 0).toFixed(2) }} 元
            </el-descriptions-item>
            <el-descriptions-item label="配送费">
              {{ Number(detail.order.deliveryFee || 0).toFixed(2) }} 元
            </el-descriptions-item>
            <el-descriptions-item label="实付金额">
              <span class="text-danger">
                {{ Number(detail.order.payAmount || 0).toFixed(2) }} 元
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="备注">
              {{ detail.order.remark || '无' }}
            </el-descriptions-item>
            <el-descriptions-item v-if="detail.order.cancelReason" label="取消原因" :span="2">
              {{ detail.order.cancelReason }}
            </el-descriptions-item>
          </el-descriptions>

          <el-divider content-position="left">菜品明细</el-divider>
          <el-table :data="detail.order.items || []" size="small" border>
            <el-table-column prop="dishName" label="菜品" min-width="140" />
            <el-table-column label="单价（元）" width="100" align="right">
              <template #default="{ row }">{{ Number(row.price || 0).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" width="80" align="center" />
            <el-table-column label="小计（元）" width="100" align="right">
              <template #default="{ row }">{{ Number(row.amount || 0).toFixed(2) }}</template>
            </el-table-column>
          </el-table>

          <el-divider content-position="left">状态时间轴</el-divider>
          <el-timeline v-if="detail.statusLogs && detail.statusLogs.length">
            <el-timeline-item
              v-for="(log, index) in detail.statusLogs"
              :key="index"
              :timestamp="log.createTime"
              placement="top"
            >
              <div>
                <el-tag size="small" :type="orderStatusTag(log.toStatus)">
                  {{ log.toStatusDesc || orderStatusText(log.toStatus) }}
                </el-tag>
                <span class="log-desc">
                  {{ log.fromStatusDesc ? `${log.fromStatusDesc} → ` : '' }}
                  {{ log.toStatusDesc || orderStatusText(log.toStatus) }}
                </span>
              </div>
              <div class="text-muted log-meta">
                操作方：{{ log.operatorTypeDesc || '-' }}
                <template v-if="log.remark">；备注：{{ log.remark }}</template>
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无状态记录" :image-size="80" />
        </template>
        <el-empty v-else description="暂无数据" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getAdminOrderPage, getAdminOrderDetail } from '@/api/order'
import { getMerchantPage } from '@/api/merchant'
import { ORDER_STATUS_OPTIONS, orderStatusText, orderStatusTag } from '@/utils/constants'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const merchantOptions = ref([])

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  merchantId: null,
  status: null
})

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = reactive({
  order: null,
  statusLogs: []
})

async function loadMerchantOptions() {
  try {
    const data = await getMerchantPage({ pageNum: 1, pageSize: 100 })
    merchantOptions.value = data?.list || []
  } catch (e) {
    merchantOptions.value = []
  }
}

async function loadList() {
  loading.value = true
  try {
    const data = await getAdminOrderPage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      merchantId: query.merchantId ?? undefined,
      status: query.status ?? undefined
    })
    list.value = data?.list || []
    total.value = Number(data?.total || 0)
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadList()
}

function handleReset() {
  query.merchantId = null
  query.status = null
  handleSearch()
}

/** 打开订单详情抽屉 */
async function openDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  detail.order = null
  detail.statusLogs = []
  try {
    const data = await getAdminOrderDetail(row.id)
    detail.order = data?.order || null
    detail.statusLogs = data?.statusLogs || []
  } catch (e) {
    /* 已统一提示 */
  } finally {
    detailLoading.value = false
  }
}

onMounted(() => {
  loadMerchantOptions()
  loadList()
})
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.address {
  font-size: 12px;
  line-height: 1.4;
}

.log-desc {
  margin-left: 8px;
  font-size: 13px;
}

.log-meta {
  font-size: 12px;
  margin-top: 4px;
}

:deep(.el-drawer__body) {
  padding-top: 8px;
}
</style>