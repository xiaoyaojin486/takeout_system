<template>
  <div class="address-list-page">
    <van-nav-bar title="收货地址" left-arrow @click-left="onBack" />

    <div v-if="loading" class="page-loading">
      <van-loading vertical>加载中...</van-loading>
    </div>

    <van-empty v-else-if="!list.length" description="还没有收货地址" />

    <div v-else class="addr-list">
      <div v-for="addr in list" :key="addr.id" class="addr-card">
        <div class="addr-main" @click="goEdit(addr.id)">
          <div class="addr-line">
            <span class="receiver">{{ addr.receiver }}</span>
            <span class="phone">{{ addr.phone }}</span>
            <van-tag v-if="addr.isDefault" type="primary" plain>默认</van-tag>
            <van-tag v-if="addr.label" plain>{{ addr.label }}</van-tag>
          </div>
          <div class="addr-detail">{{ fullAddressOf(addr) }}</div>
        </div>

        <div class="addr-foot">
          <van-checkbox :model-value="!!addr.isDefault" @click="onSetDefault(addr)">
            设为默认
          </van-checkbox>
          <div class="addr-ops">
            <span @click="goEdit(addr.id)"><van-icon name="edit" /> 编辑</span>
            <span @click="onDelete(addr)"><van-icon name="delete-o" /> 删除</span>
          </div>
        </div>
      </div>
    </div>

    <div class="add-bar">
      <van-button type="primary" round block icon="plus" @click="goEdit()">新增收货地址</van-button>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import { deleteAddress, getAddressList, setDefaultAddress } from '../api/address'

const router = useRouter()

const list = ref([])
const loading = ref(true)

function fullAddressOf(addr) {
  if (addr.fullAddress) return addr.fullAddress
  return `${addr.province || ''}${addr.city || ''}${addr.district || ''}${addr.detail || ''}`
}

async function loadList() {
  loading.value = true
  try {
    const data = await getAddressList()
    list.value = data || []
  } catch (e) {
    // 错误提示已由拦截器统一处理
  } finally {
    loading.value = false
  }
}

async function onSetDefault(addr) {
  if (addr.isDefault) return
  try {
    await setDefaultAddress(addr.id)
    showToast('已设为默认')
    await loadList()
  } catch (e) {
    // 错误提示已由拦截器统一处理
  }
}

async function onDelete(addr) {
  try {
    await showConfirmDialog({ title: '删除地址', message: '确定删除该收货地址吗？' })
  } catch (e) {
    return
  }
  try {
    await deleteAddress(addr.id)
    showToast('已删除')
    await loadList()
  } catch (e) {
    // 错误提示已由拦截器统一处理
  }
}

function goEdit(id) {
  if (id) router.push(`/address/edit/${id}`)
  else router.push('/address/edit')
}

function onBack() {
  router.back()
}

onMounted(loadList)
</script>

<style scoped>
.address-list-page {
  min-height: 100vh;
  padding-bottom: 84px;
  box-sizing: border-box;
  background: #f7f8fa;
}

.page-loading {
  display: flex;
  justify-content: center;
  padding-top: 80px;
}

.addr-list {
  padding: 10px;
}

.addr-card {
  margin-bottom: 10px;
  padding: 12px;
  background: #fff;
  border-radius: 10px;
}

.addr-line {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
}

.phone {
  font-size: 13px;
  font-weight: 400;
  color: #646566;
}

.addr-detail {
  margin-top: 6px;
  font-size: 13px;
  color: #646566;
}

.addr-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #f7f8fa;
  font-size: 13px;
  color: #646566;
}

.addr-ops {
  display: flex;
  gap: 16px;
}

.add-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 10px 16px calc(10px + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1px solid #f2f3f5;
}
</style>