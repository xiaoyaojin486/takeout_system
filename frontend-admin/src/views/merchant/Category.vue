<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>分类管理</span>
          <div>
            <el-button :icon="Refresh" @click="loadList">刷新</el-button>
            <el-button type="primary" :icon="Plus" @click="openDialog()">新增分类</el-button>
          </div>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="pageList"
        border
        stripe
        empty-text="暂无分类数据"
      >
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="name" label="分类名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="sort" label="排序" width="100" align="center" />
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-pagination">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑分类' : '新增分类'"
      width="460px"
      @closed="handleDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" maxlength="20" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
          <span class="text-muted ml-8">数值越小越靠前</span>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import {
  getCategoryList,
  createCategory,
  updateCategory,
  deleteCategory
} from '@/api/category'

const loading = ref(false)
const submitting = ref(false)
const list = ref([])

// 分类接口不分页，这里做前端分页，保持与其他表格一致的交互
const pageNum = ref(1)
const pageSize = ref(10)

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const formRef = ref(null)

const form = reactive({
  name: '',
  sort: 0,
  status: 1
})

const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  sort: [{ required: true, message: '请输入排序值', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const total = computed(() => list.value.length)
const pageList = computed(() => {
  const start = (pageNum.value - 1) * pageSize.value
  return list.value.slice(start, start + pageSize.value)
})

async function loadList() {
  loading.value = true
  try {
    const data = await getCategoryList()
    list.value = Array.isArray(data) ? data : []
    // 删除后页码可能越界，回退一页
    const maxPage = Math.max(1, Math.ceil(list.value.length / pageSize.value))
    if (pageNum.value > maxPage) {
      pageNum.value = maxPage
    }
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  isEdit.value = !!row
  editId.value = row?.id ?? null
  form.name = row?.name || ''
  form.sort = row?.sort ?? 0
  form.status = row?.status ?? 1
  dialogVisible.value = true
}

function handleDialogClosed() {
  formRef.value?.clearValidate()
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  submitting.value = true
  try {
    const payload = { name: form.name, sort: form.sort, status: form.status }
    if (isEdit.value) {
      await updateCategory(editId.value, payload)
      ElMessage.success('分类修改成功')
    } else {
      await createCategory(payload)
      ElMessage.success('分类新增成功')
    }
    dialogVisible.value = false
    await loadList()
  } catch (e) {
    /* 已统一提示 */
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除分类「${row.name}」吗？该分类下的菜品可能受影响。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  try {
    await deleteCategory(row.id)
    ElMessage.success('分类删除成功')
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

.ml-8 {
  margin-left: 8px;
  font-size: 12px;
}
</style>