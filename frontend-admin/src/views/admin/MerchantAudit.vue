<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>商家审核（仅显示待审核商家）</span>
          <div class="card-header-actions">
            <el-input
              v-model="query.keyword"
              placeholder="商家名称关键词"
              clearable
              style="width: 200px"
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            />
            <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
            <el-button :icon="Refresh" @click="loadList">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="list"
        border
        stripe
        empty-text="暂无待审核商家"
      >
        <el-table-column label="Logo" width="90" align="center">
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
        <el-table-column prop="ownerUsername" label="店主账号" width="120" />
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column prop="address" label="店铺地址" min-width="180" show-overflow-tooltip />
        <el-table-column label="起送价（元）" width="110" align="right">
          <template #default="{ row }">{{ Number(row.minAmount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="配送费（元）" width="110" align="right">
          <template #default="{ row }">{{ Number(row.deliveryFee || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="170" />
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="success" @click="handleApprove(row)">通过</el-button>
            <el-button link type="danger" @click="handleReject(row)">驳回</el-button>
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
import { getMerchantPage, auditMerchant } from '@/api/merchant'
import { resolveImageUrl } from '@/utils/image'

const loading = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  // 只列待审核商家
  auditStatus: 0
})

async function loadList() {
  loading.value = true
  try {
    const data = await getMerchantPage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      auditStatus: query.auditStatus
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

/** 审核通过 */
async function handleApprove(row) {
  try {
    await ElMessageBox.confirm(
      `确定通过商家「${row.name}」的入驻申请吗？通过后该店铺可正式营业。`,
      '审核通过',
      { type: 'warning', confirmButtonText: '确定通过', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  try {
    await auditMerchant(row.id, { auditStatus: 1 })
    ElMessage.success('审核已通过')
    await loadList()
  } catch (e) {
    /* 已统一提示 */
  }
}

/** 审核驳回：必须填写驳回意见 */
async function handleReject(row) {
  let auditRemark = ''
  try {
    const result = await ElMessageBox.prompt(
      `请填写驳回「${row.name}」的原因（商家可见）`,
      '审核驳回',
      {
        confirmButtonText: '确定驳回',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: '例如：营业执照信息不完整 / 店铺地址不准确',
        inputValidator: (value) => (value && value.trim() ? true : '驳回意见不能为空')
      }
    )
    auditRemark = result.value.trim()
  } catch (e) {
    return
  }
  try {
    await auditMerchant(row.id, { auditStatus: 2, auditRemark })
    ElMessage.success('已驳回该入驻申请')
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

.card-header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>