<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>商家列表</span>
          <el-button :icon="Refresh" @click="loadList">刷新</el-button>
        </div>
      </template>

      <div class="filter-bar">
        <el-input
          v-model="query.keyword"
          placeholder="商家名称关键词"
          clearable
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select v-model="query.auditStatus" placeholder="全部审核状态" clearable @change="handleSearch">
          <el-option
            v-for="item in AUDIT_STATUS_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-select
          v-model="query.businessStatus"
          placeholder="全部营业状态"
          clearable
          @change="handleSearch"
        >
          <el-option label="营业中" :value="1" />
          <el-option label="休息中" :value="0" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="list"
        border
        stripe
        empty-text="暂无商家数据"
      >
        <el-table-column label="Logo" width="80" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.logo"
              class="dish-thumb"
              :src="resolveImageUrl(row.logo)"
              :preview-src-list="[resolveImageUrl(row.logo)]"
              preview-teleported
              fit="cover"
            />
            <span v-else class="text-muted">无图</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="店铺名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="ownerUsername" label="店主账号" width="110" />
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column label="审核状态" width="110" align="center">
          <template #default="{ row }">
            <el-tooltip
              :disabled="row.auditStatus !== 2 || !row.auditRemark"
              :content="`驳回原因：${row.auditRemark || ''}`"
            >
              <el-tag :type="auditStatusTag(row.auditStatus)">
                {{ row.auditStatusDesc || auditStatusText(row.auditStatus) }}
              </el-tag>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="营业状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="businessStatusTag(row.businessStatus)">
              {{ row.businessStatusDesc || businessStatusText(row.businessStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="90" align="center">
          <template #default="{ row }">{{ row.rating ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="销量" width="90" align="center">
          <template #default="{ row }">{{ row.sales ?? 0 }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="入驻时间" width="170" />
        <el-table-column label="操作" width="130" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.businessStatus === 1"
              link
              type="danger"
              @click="handleForceClose(row)"
            >
              强制打烊
            </el-button>
            <el-button v-else link type="success" @click="handleReopen(row)">
              恢复营业
            </el-button>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getMerchantPage, updateMerchantBusinessStatus } from '@/api/merchant'
import {
  AUDIT_STATUS_OPTIONS,
  auditStatusText,
  auditStatusTag,
  businessStatusText,
  businessStatusTag
} from '@/utils/constants'
import { resolveImageUrl } from '@/utils/image'

const loading = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  auditStatus: null,
  businessStatus: null
})

async function loadList() {
  loading.value = true
  try {
    const data = await getMerchantPage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      auditStatus: query.auditStatus ?? undefined,
      businessStatus: query.businessStatus ?? undefined
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
  query.keyword = ''
  query.auditStatus = null
  query.businessStatus = null
  handleSearch()
}

/** 强制打烊 */
async function handleForceClose(row) {
  try {
    await ElMessageBox.confirm(
      `确定强制让「${row.name}」打烊吗？打烊后该店铺将无法接单。`,
      '强制打烊',
      { type: 'warning', confirmButtonText: '确定打烊', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  try {
    await updateMerchantBusinessStatus(row.id, { businessStatus: 0 })
    ElMessage.success('已强制打烊')
    await loadList()
  } catch (e) {
    /* 已统一提示 */
  }
}

/** 恢复营业 */
async function handleReopen(row) {
  try {
    await ElMessageBox.confirm(`确定恢复「${row.name}」营业吗？`, '恢复营业', {
      type: 'info',
      confirmButtonText: '确定恢复',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  try {
    await updateMerchantBusinessStatus(row.id, { businessStatus: 1 })
    ElMessage.success('已恢复营业')
    await loadList()
  } catch (e) {
    /* 已统一提示 */
  }
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>