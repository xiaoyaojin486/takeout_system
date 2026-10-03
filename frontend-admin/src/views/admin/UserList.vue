<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-button :icon="Refresh" @click="loadList">刷新</el-button>
        </div>
      </template>

      <div class="filter-bar">
        <el-input
          v-model="query.keyword"
          placeholder="用户名 / 昵称 / 手机号"
          clearable
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select v-model="query.userType" placeholder="全部身份" clearable @change="handleSearch">
          <el-option
            v-for="item in USER_TYPE_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable @change="handleSearch">
          <el-option label="正常" :value="1" />
          <el-option label="已停用" :value="0" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="list"
        border
        stripe
        empty-text="暂无用户数据"
      >
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="username" label="用户名" min-width="140" show-overflow-tooltip />
        <el-table-column prop="nickname" label="昵称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column label="身份" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="userTypeTag(row.userType)">
              {{ row.userTypeDesc || userTypeText(row.userType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="userStatusTag(row.status)">
              {{ userStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最近登录" width="170" />
        <el-table-column prop="createTime" label="注册时间" width="170" />
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-tooltip
              :disabled="row.userType !== 3"
              content="管理员账号不可停用"
              placement="top"
            >
              <span>
                <el-button
                  link
                  :type="row.status === 1 ? 'danger' : 'success'"
                  :disabled="row.userType === 3"
                  @click="handleToggleStatus(row)"
                >
                  {{ row.status === 1 ? '停用' : '启用' }}
                </el-button>
              </span>
            </el-tooltip>
            <el-button link type="primary" @click="handleResetPassword(row)">
              重置密码
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
import { getUserPage, updateUserStatus, resetUserPassword } from '@/api/user'
import {
  USER_TYPE_OPTIONS,
  userTypeText,
  userTypeTag,
  userStatusText,
  userStatusTag
} from '@/utils/constants'

const loading = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  userType: null,
  status: null
})

async function loadList() {
  loading.value = true
  try {
    const data = await getUserPage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      userType: query.userType ?? undefined,
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
  query.keyword = ''
  query.userType = null
  query.status = null
  handleSearch()
}

/** 启用 / 停用 */
async function handleToggleStatus(row) {
  const targetStatus = row.status === 1 ? 0 : 1
  const actionText = targetStatus === 1 ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(
      `确定${actionText}用户「${row.username}」吗？${
        targetStatus === 0 ? '停用后该账号将无法登录。' : ''
      }`,
      `${actionText}确认`,
      { type: 'warning', confirmButtonText: `确定${actionText}`, cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  try {
    await updateUserStatus(row.id, { status: targetStatus })
    ElMessage.success(`已${actionText}该用户`)
    await loadList()
  } catch (e) {
    /* 已统一提示 */
  }
}

/** 重置密码 */
async function handleResetPassword(row) {
  let newPassword = ''
  try {
    const result = await ElMessageBox.prompt(
      `请为「${row.username}」设置新密码`,
      '重置密码',
      {
        confirmButtonText: '确定重置',
        cancelButtonText: '取消',
        inputType: 'password',
        inputPlaceholder: '请输入新密码（不少于 6 位）',
        inputValidator: (value) =>
          value && value.trim().length >= 6 ? true : '密码不能少于 6 位'
      }
    )
    newPassword = result.value.trim()
  } catch (e) {
    return
  }
  try {
    await resetUserPassword(row.id, { newPassword })
    ElMessage.success('密码重置成功')
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