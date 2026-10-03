<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>订单管理</span>
          <el-button :icon="Refresh" @click="loadList">刷新</el-button>
        </div>
      </template>

      <el-tabs v-model="activeStatus" @tab-change="handleTabChange">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane
          v-for="tab in statusTabs"
          :key="tab.value"
          :label="tab.label"
          :name="String(tab.value)"
        >
          <template #label>
            <el-badge
              v-if="tab.value === 20 && pendingCount > 0"
              :value="pendingCount"
              type="danger"
            >
              {{ tab.label }}
            </el-badge>
            <span v-else>{{ tab.label }}</span>
          </template>
        </el-tab-pane>
      </el-tabs>

      <el-table
        v-loading="loading"
        :data="list"
        border
        stripe
        empty-text="暂无订单数据"
      >
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="order-items">
              <el-table :data="row.items || []" size="small" border>
                <el-table-column label="菜品" prop="dishName" min-width="150" />
                <el-table-column label="单价（元）" width="110" align="right">
                  <template #default="scope">
                    {{ Number(scope.row.price || 0).toFixed(2) }}
                  </template>
                </el-table-column>
                <el-table-column label="数量" prop="quantity" width="90" align="center" />
                <el-table-column label="小计（元）" width="110" align="right">
                  <template #default="scope">
                    {{ Number(scope.row.amount || 0).toFixed(2) }}
                  </template>
                </el-table-column>
              </el-table>
              <div v-if="row.remark" class="remark">
                <span class="text-muted">订单备注：</span>{{ row.remark }}
              </div>
              <div v-if="row.cancelReason" class="remark">
                <span class="text-muted">取消原因：</span>{{ row.cancelReason }}
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="orderNo" label="订单号" min-width="180" show-overflow-tooltip />
        <el-table-column label="收货人" min-width="180">
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
        <el-table-column label="操作" width="190" align="center" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 20">
              <el-button link type="primary" @click="handleAccept(row)">接单</el-button>
              <el-button link type="danger" @click="handleReject(row)">拒单</el-button>
            </template>
            <template v-else-if="row.status === 30">
              <el-button link type="primary" @click="handleDeliver(row)">出餐</el-button>
              <el-button link type="danger" @click="handleCancel(row)">取消</el-button>
            </template>
            <template v-else-if="row.status === 40">
              <el-button link type="success" @click="handleFinish(row)">送达</el-button>
            </template>
            <span v-else class="text-muted">—</span>
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
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import {
  getMerchantOrderPage,
  acceptOrder,
  rejectOrder,
  cancelOrder,
  deliverOrder,
  finishOrder
} from '@/api/order'
import { orderStatusText, orderStatusTag } from '@/utils/constants'
import { getMerchantOverview } from '@/api/stats'

const route = useRoute()

const statusTabs = [
  { value: 20, label: '待接单' },
  { value: 30, label: '备餐中' },
  { value: 40, label: '配送中' },
  { value: 50, label: '已完成' },
  { value: 60, label: '已取消' }
]

const activeStatus = ref('all')
const list = ref([])
const total = ref(0)
const loading = ref(false)
const pendingCount = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  status: null
})

async function loadList() {
  loading.value = true
  try {
    const data = await getMerchantOrderPage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
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

/** 待接单数量，用于 tab 角标提醒 */
async function loadPendingCount() {
  try {
    const data = await getMerchantOverview()
    pendingCount.value = Number(data?.pendingAcceptCount || 0)
  } catch (e) {
    pendingCount.value = 0
  }
}

function handleTabChange(name) {
  activeStatus.value = name
  query.status = name === 'all' ? null : Number(name)
  query.pageNum = 1
  loadList()
}

/** 接单 */
async function handleAccept(row) {
  try {
    await ElMessageBox.confirm(`确定接单？订单号：${row.orderNo}`, '接单确认', {
      type: 'info',
      confirmButtonText: '确定接单',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  try {
    await acceptOrder(row.id)
    ElMessage.success('接单成功，请尽快备餐')
    await refreshAfterAction()
  } catch (e) {
    /* 已统一提示 */
  }
}

/** 拒单：必须填写原因 */
async function handleReject(row) {
  let reason = ''
  try {
    const result = await ElMessageBox.prompt('请填写拒单原因（顾客可见）', '拒单', {
      confirmButtonText: '确定拒单',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：菜品已售罄 / 门店休息中',
      inputValidator: (value) => (value && value.trim() ? true : '拒单原因不能为空')
    })
    reason = result.value.trim()
  } catch (e) {
    return
  }
  try {
    await rejectOrder(row.id, { reason })
    ElMessage.success('已拒单')
    await refreshAfterAction()
  } catch (e) {
    /* 已统一提示 */
  }
}

/** 商家取消订单 */
async function handleCancel(row) {
  let reason = ''
  try {
    const result = await ElMessageBox.prompt('请填写取消原因（可留空）', '取消订单', {
      confirmButtonText: '确定取消',
      cancelButtonText: '再想想',
      inputPlaceholder: '例如：食材不足无法制作'
    })
    reason = (result.value || '').trim()
  } catch (e) {
    return
  }
  try {
    await cancelOrder(row.id, { reason })
    ElMessage.success('订单已取消')
    await refreshAfterAction()
  } catch (e) {
    /* 已统一提示 */
  }
}

/** 出餐 */
async function handleDeliver(row) {
  try {
    await ElMessageBox.confirm(`确认「${row.orderNo}」已出餐并开始配送？`, '出餐确认', {
      type: 'info',
      confirmButtonText: '确定出餐',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  try {
    await deliverOrder(row.id)
    ElMessage.success('已出餐，订单进入配送中')
    await refreshAfterAction()
  } catch (e) {
    /* 已统一提示 */
  }
}

/** 送达 */
async function handleFinish(row) {
  try {
    await ElMessageBox.confirm(`确认「${row.orderNo}」已送达？`, '送达确认', {
      type: 'info',
      confirmButtonText: '确定送达',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  try {
    await finishOrder(row.id)
    ElMessage.success('订单已完成')
    await refreshAfterAction()
  } catch (e) {
    /* 已统一提示 */
  }
}

async function refreshAfterAction() {
  await loadList()
  loadPendingCount()
}

onMounted(() => {
  // 支持从工作台带 status 参数跳转（如待接单）
  const statusFromQuery = route.query.status
  if (statusFromQuery) {
    activeStatus.value = String(statusFromQuery)
    query.status = Number(statusFromQuery)
  }
  loadList()
  loadPendingCount()
})
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.order-items {
  padding: 8px 24px 12px;
}

.address {
  font-size: 12px;
  line-height: 1.4;
}

.remark {
  margin-top: 8px;
  font-size: 13px;
}
</style>