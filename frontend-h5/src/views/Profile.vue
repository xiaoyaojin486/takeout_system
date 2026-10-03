<template>
  <div class="profile-page">
    <van-nav-bar title="个人资料" left-arrow @click-left="onBack" />

    <van-cell-group inset title="基本资料">
      <van-field v-model="form.nickname" label="昵称" placeholder="请输入昵称" clearable />
      <van-field
        v-model="form.phone"
        type="tel"
        label="手机号"
        placeholder="请输入手机号"
        clearable
        :rules="[{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }]"
      />
      <van-field
        v-model="form.avatar"
        label="头像地址"
        placeholder="图片 URL（选填）"
        clearable
      />
      <van-field label="账号" :model-value="userStore.userInfo?.username || ''" readonly />
    </van-cell-group>

    <div class="submit-wrap">
      <van-button round block type="primary" :loading="saving" @click="onSaveProfile">
        保存资料
      </van-button>
    </div>

    <van-cell-group inset title="修改密码">
      <van-field
        v-model="pwd.oldPassword"
        type="password"
        label="原密码"
        placeholder="请输入原密码"
        clearable
      />
      <van-field
        v-model="pwd.newPassword"
        type="password"
        label="新密码"
        placeholder="6-20 位"
        clearable
      />
      <van-field
        v-model="pwd.confirmPassword"
        type="password"
        label="确认新密码"
        placeholder="请再次输入新密码"
        clearable
      />
    </van-cell-group>

    <div class="submit-wrap">
      <van-button round block plain type="primary" :loading="changing" @click="onChangePassword">
        修改密码
      </van-button>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { changePassword, getProfile, updateProfile } from '../api/user'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

const saving = ref(false)
const changing = ref(false)

const form = reactive({ nickname: '', phone: '', avatar: '' })
const pwd = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

async function loadProfile() {
  try {
    const profile = await getProfile()
    form.nickname = profile?.nickname || ''
    form.phone = profile?.phone || ''
    form.avatar = profile?.avatar || ''
    // 同步到全局 store，保证「我的」页展示最新数据
    userStore.userInfo = profile
  } catch (e) {
    // 错误提示已由拦截器统一处理
  }
}

async function onSaveProfile() {
  if (form.phone && !/^1[3-9]\d{9}$/.test(form.phone)) {
    showToast('手机号格式不正确')
    return
  }
  saving.value = true
  try {
    await updateProfile({
      nickname: form.nickname.trim() || undefined,
      phone: form.phone.trim() || undefined,
      avatar: form.avatar.trim() || undefined
    })
    await userStore.loadProfile()
    showToast('保存成功')
  } catch (e) {
    // 错误提示已由拦截器统一处理
  } finally {
    saving.value = false
  }
}

async function onChangePassword() {
  if (!pwd.oldPassword) {
    showToast('请输入原密码')
    return
  }
  if (!pwd.newPassword || pwd.newPassword.length < 6 || pwd.newPassword.length > 20) {
    showToast('新密码长度为 6-20 位')
    return
  }
  if (pwd.newPassword !== pwd.confirmPassword) {
    showToast('两次输入的新密码不一致')
    return
  }
  changing.value = true
  try {
    await changePassword({ oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
    showToast('密码修改成功')
    pwd.oldPassword = ''
    pwd.newPassword = ''
    pwd.confirmPassword = ''
  } catch (e) {
    // 错误提示已由拦截器统一处理
  } finally {
    changing.value = false
  }
}

function onBack() {
  router.back()
}

onMounted(loadProfile)
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  padding-bottom: 24px;
  box-sizing: border-box;
  background: #f7f8fa;
}

.submit-wrap {
  margin: 16px 16px 24px;
}
</style>