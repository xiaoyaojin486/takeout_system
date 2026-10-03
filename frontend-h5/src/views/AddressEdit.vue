<template>
  <div class="address-edit-page">
    <van-nav-bar :title="isEdit ? '编辑地址' : '新增地址'" left-arrow @click-left="onBack" />

    <van-form @submit="onSubmit">
      <van-cell-group inset>
        <van-field
          v-model="form.receiver"
          label="收货人"
          placeholder="请输入收货人姓名"
          clearable
          :rules="[{ required: true, message: '请输入收货人姓名' }]"
        />
        <van-field
          v-model="form.phone"
          type="tel"
          label="手机号"
          placeholder="请输入手机号"
          clearable
          :rules="[
            { required: true, message: '请输入手机号' },
            { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }
          ]"
        />
        <van-field v-model="form.province" label="省份" placeholder="如 广东省" clearable />
        <van-field v-model="form.city" label="城市" placeholder="如 深圳市" clearable />
        <van-field v-model="form.district" label="区县" placeholder="如 南山区" clearable />
        <van-field
          v-model="form.detail"
          label="详细地址"
          type="textarea"
          rows="2"
          autosize
          placeholder="街道、门牌号、楼层等"
          :rules="[{ required: true, message: '请输入详细地址' }]"
        />
        <van-field v-model="form.label" label="标签" placeholder="如 家 / 公司（选填）" clearable />
      </van-cell-group>

      <div class="submit-wrap">
        <van-button
          round
          block
          type="primary"
          native-type="submit"
          :loading="saving"
          loading-text="保存中..."
        >
          保存
        </van-button>
      </div>
    </van-form>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { addAddress, getAddressList, updateAddress } from '../api/address'

const route = useRoute()
const router = useRouter()

const addressId = route.params.id
const isEdit = !!addressId

const saving = ref(false)
const form = reactive({
  receiver: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  label: ''
})

/** 无地址详情接口，编辑时从列表里取 */
async function loadDetail() {
  if (!isEdit) return
  try {
    const list = await getAddressList()
    const target = (list || []).find((a) => String(a.id) === String(addressId))
    if (target) {
      form.receiver = target.receiver || ''
      form.phone = target.phone || ''
      form.province = target.province || ''
      form.city = target.city || ''
      form.district = target.district || ''
      form.detail = target.detail || ''
      form.label = target.label || ''
    }
  } catch (e) {
    // 错误提示已由拦截器统一处理
  }
}

async function onSubmit() {
  saving.value = true
  try {
    const payload = {
      receiver: form.receiver.trim(),
      phone: form.phone.trim(),
      province: form.province.trim() || undefined,
      city: form.city.trim() || undefined,
      district: form.district.trim() || undefined,
      detail: form.detail.trim(),
      label: form.label.trim() || undefined
    }
    if (isEdit) await updateAddress(addressId, payload)
    else await addAddress(payload)
    showToast('保存成功')
    router.back()
  } catch (e) {
    // 错误提示已由拦截器统一处理
  } finally {
    saving.value = false
  }
}

function onBack() {
  router.back()
}

onMounted(loadDetail)
</script>

<style scoped>
.address-edit-page {
  min-height: 100vh;
  background: #f7f8fa;
}

.submit-wrap {
  margin: 24px 16px 0;
}
</style>