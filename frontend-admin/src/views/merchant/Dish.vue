<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>菜品管理</span>
          <el-button type="primary" :icon="Plus" @click="openDialog()">新增菜品</el-button>
        </div>
      </template>

      <div class="filter-bar">
        <el-input
          v-model="query.keyword"
          placeholder="菜品名称关键词"
          clearable
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select v-model="query.categoryId" placeholder="全部分类" clearable @change="handleSearch">
          <el-option
            v-for="item in categoryList"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable @change="handleSearch">
          <el-option label="售卖中" :value="1" />
          <el-option label="已下架" :value="0" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="list"
        border
        stripe
        empty-text="暂无菜品数据"
      >
        <el-table-column label="图片" width="90" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.image"
              class="dish-thumb"
              :src="resolveImageUrl(row.image)"
              :preview-src-list="[resolveImageUrl(row.image)]"
              preview-teleported
              fit="cover"
            />
            <span v-else class="text-muted">无图</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="菜品名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="所属分类" width="130" show-overflow-tooltip />
        <el-table-column label="价格（元）" width="110" align="right">
          <template #default="{ row }">{{ Number(row.price || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="90" align="center" />
        <el-table-column prop="sales" label="销量" width="90" align="center" />
        <el-table-column label="上架状态" width="110" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              :before-change="() => handleStatusChange(row)"
              inline-prompt
              active-text="上架"
              inactive-text="下架"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑菜品' : '新增菜品'"
      width="620px"
      @closed="handleDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="菜品名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入菜品名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="所属分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option
              v-for="item in categoryList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="菜品图片" prop="image">
          <ImageUpload v-model="form.image" />
        </el-form-item>
        <el-form-item label="价格（元）" prop="price">
          <el-input-number
            v-model="form.price"
            :min="0"
            :max="99999"
            :precision="2"
            :step="1"
          />
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="form.stock" :min="0" :max="999999" :step="1" />
        </el-form-item>
        <el-form-item label="上架状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">上架</el-radio>
            <el-radio :value="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜品描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="请输入菜品描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import ImageUpload from '@/components/ImageUpload.vue'
import {
  getDishPage,
  createDish,
  updateDish,
  deleteDish,
  updateDishStatus
} from '@/api/dish'
import { getCategoryList } from '@/api/category'
import { resolveImageUrl } from '@/utils/image'

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const total = ref(0)
const categoryList = ref([])

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  categoryId: null,
  status: null
})

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const formRef = ref(null)

const form = reactive({
  categoryId: null,
  name: '',
  image: '',
  description: '',
  price: 0,
  stock: 0,
  status: 1
})

const rules = {
  name: [{ required: true, message: '请输入菜品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择所属分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }],
  status: [{ required: true, message: '请选择上架状态', trigger: 'change' }]
}

async function loadCategoryList() {
  try {
    const data = await getCategoryList()
    categoryList.value = Array.isArray(data) ? data : []
  } catch (e) {
    categoryList.value = []
  }
}

async function loadList() {
  loading.value = true
  try {
    const params = {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      categoryId: query.categoryId ?? undefined,
      status: query.status ?? undefined
    }
    const data = await getDishPage(params)
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
  query.categoryId = null
  query.status = null
  handleSearch()
}

/** 上下架开关（before-change 保证请求成功后才切换显示） */
async function handleStatusChange(row) {
  const targetStatus = row.status === 1 ? 0 : 1
  try {
    await updateDishStatus(row.id, targetStatus)
    ElMessage.success(targetStatus === 1 ? '菜品已上架' : '菜品已下架')
    return true
  } catch (e) {
    return false
  }
}

function openDialog(row) {
  isEdit.value = !!row
  editId.value = row?.id ?? null
  form.categoryId = row?.categoryId ?? null
  form.name = row?.name || ''
  form.image = row?.image || ''
  form.description = row?.description || ''
  form.price = Number(row?.price || 0)
  form.stock = row?.stock ?? 0
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
    const payload = {
      categoryId: form.categoryId,
      name: form.name,
      image: form.image || undefined,
      description: form.description || undefined,
      price: form.price,
      stock: form.stock,
      status: form.status
    }
    if (isEdit.value) {
      await updateDish(editId.value, payload)
      ElMessage.success('菜品修改成功')
    } else {
      await createDish(payload)
      ElMessage.success('菜品新增成功')
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
    await ElMessageBox.confirm(`确定要删除菜品「${row.name}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  try {
    await deleteDish(row.id)
    ElMessage.success('菜品删除成功')
    await loadList()
  } catch (e) {
    /* 已统一提示 */
  }
}

onMounted(() => {
  loadCategoryList()
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