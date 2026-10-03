<template>
  <div class="page home-page">
    <van-nav-bar title="发现好店" fixed placeholder />

    <van-search
      v-model="keyword"
      placeholder="搜索店铺名称"
      shape="round"
      @search="onSearch"
      @clear="onSearch"
    />

    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="onLoad"
      >
        <div v-for="item in list" :key="item.id" class="shop-card" @click="goMerchant(item.id)">
          <van-image
            class="shop-logo"
            :src="resolveImage(item.logo)"
            width="64"
            height="64"
            radius="8"
            fit="cover"
          >
            <template #error>
              <van-icon name="shop-o" size="28" color="#c8c9cc" />
            </template>
          </van-image>

          <div class="shop-main">
            <div class="shop-name text-ellipsis">{{ item.name }}</div>
            <div class="shop-meta">
              <van-tag v-if="item.businessStatus === 1" type="success" plain>营业中</van-tag>
              <van-tag v-else type="default" plain>休息中</van-tag>
              <span>评分 {{ item.rating ?? '-' }}</span>
              <span>月售 {{ item.sales ?? 0 }}</span>
            </div>
            <div class="shop-meta sub">
              <span>起送 ¥{{ formatAmount(item.minAmount) }}</span>
              <span>配送 ¥{{ formatAmount(item.deliveryFee) }}</span>
            </div>
            <div class="shop-desc text-ellipsis">{{ item.description || '暂无简介' }}</div>
          </div>
        </div>

        <van-empty v-if="!loading && !list.length" description="没有找到相关店铺" />
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { getMerchantList } from '../api/merchant'
import { resolveImage, formatAmount } from '../utils/format'

const router = useRouter()

const list = ref([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const keyword = ref('')

const pageNum = ref(1)
const pageSize = 10

async function fetchList() {
  try {
    const res = await getMerchantList({
      pageNum: pageNum.value,
      pageSize,
      keyword: keyword.value.trim() || undefined
    })
    const records = res?.list || []
    list.value.push(...records)
    pageNum.value += 1
    const total = Number(res?.total ?? 0)
    finished.value = records.length === 0 || list.value.length >= total
  } catch (e) {
    // 错误提示已由拦截器统一处理，避免 van-list 无限重试
    finished.value = true
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

function onLoad() {
  fetchList()
}

function reset() {
  list.value = []
  pageNum.value = 1
  finished.value = false
}

function onSearch() {
  reset()
  loading.value = true
  fetchList()
}

function onRefresh() {
  reset()
  loading.value = true
  fetchList()
}

function goMerchant(id) {
  router.push(`/merchant/${id}`)
}
</script>

<style scoped>
.home-page {
  background: #f7f8fa;
}

.shop-card {
  display: flex;
  align-items: flex-start;
  padding: 12px;
  margin: 10px;
  background: #fff;
  border-radius: 10px;
}

.shop-logo {
  flex: none;
  background: #f7f8fa;
  border-radius: 8px;
}

.shop-main {
  flex: 1;
  min-width: 0;
  margin-left: 10px;
}

.shop-name {
  font-size: 16px;
  font-weight: 600;
}

.shop-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
  font-size: 12px;
  color: #646566;
}

.shop-meta.sub {
  color: #969799;
}

.shop-desc {
  margin-top: 6px;
  font-size: 12px;
  color: #969799;
}
</style>