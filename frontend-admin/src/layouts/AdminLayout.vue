<template>
  <el-container class="admin-layout">
    <!-- 左侧菜单：按当前 userType 动态挂载 -->
    <el-aside :width="collapsed ? '64px' : '220px'" class="layout-aside">
      <div class="layout-logo">
        <el-icon :size="22"><Bowl /></el-icon>
        <span v-show="!collapsed" class="logo-text">{{ roleTitle }}</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="collapsed"
        :collapse-transition="false"
        router
        background-color="#001529"
        text-color="#bfcbd9"
        active-text-color="#ffffff"
      >
        <el-menu-item v-for="item in menus" :key="item.index" :index="item.index">
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <el-icon class="collapse-btn" :size="20" @click="collapsed = !collapsed">
            <Expand v-if="collapsed" />
            <Fold v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>{{ roleTitle }}</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-tag type="info" effect="plain" class="mr-12">
            {{ userStore.userInfo?.userTypeDesc || roleTitle }}
          </el-tag>
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="30" :src="avatarUrl">
                {{ displayInitial }}
              </el-avatar>
              <span class="user-name">{{ userStore.displayName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="refresh">
                  <el-icon><Refresh /></el-icon>刷新用户信息
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="layout-main">
        <router-view v-slot="{ Component }">
          <component :is="Component" />
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { resolveImageUrl } from '@/utils/image'
import { APP_TITLE, CLIENT_TYPE_TITLE } from '@/utils/constants'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const collapsed = ref(false)

/** 当前角色标题：商家端 / 管理后台 */
const roleTitle = computed(() => {
  if (userStore.userType === 3) return CLIENT_TYPE_TITLE.ADMIN
  if (userStore.userType === 2) return CLIENT_TYPE_TITLE.MERCHANT
  return APP_TITLE
})

/** 取静态路由表中与 userType 匹配的那一组（角色动态菜单核心） */
const currentGroup = computed(() => {
  const groupMeta = userStore.userType
  return (
    (router.options.routes || []).find(
      (item) => item.meta?.requiredUserType === groupMeta
    ) || null
  )
})

/** 过滤后的菜单项 */
const menus = computed(() => {
  const group = currentGroup.value
  if (!group) return []
  return (group.children || []).map((child) => ({
    index: `${group.path}/${child.path}`,
    title: child.meta?.title || '',
    icon: child.meta?.icon || 'Menu'
  }))
})

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta?.title || '')
const avatarUrl = computed(() => resolveImageUrl(userStore.userInfo?.avatar))
const displayInitial = computed(() =>
  (userStore.displayName || 'U').charAt(0).toUpperCase()
)

async function handleCommand(command) {
  if (command === 'refresh') {
    await userStore.fetchMe()
    ElMessage.success('用户信息已更新')
    return
  }
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定退出',
        cancelButtonText: '取消',
        type: 'warning'
      })
    } catch (e) {
      return
    }
    await userStore.logout()
    ElMessage.success('已退出登录')
    router.replace('/login')
  }
}
</script>

<style scoped>
.admin-layout {
  height: 100vh;
}

.layout-aside {
  background-color: #001529;
  transition: width 0.2s;
  overflow-x: hidden;
}

.layout-logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  background-color: #002140;
  white-space: nowrap;
}

.logo-text {
  letter-spacing: 1px;
}

.layout-aside :deep(.el-menu) {
  border-right: none;
}

.layout-aside :deep(.el-menu-item.is-active) {
  background-color: #409eff !important;
}

.layout-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  cursor: pointer;
  color: #606266;
}

.collapse-btn:hover {
  color: #409eff;
}

.header-right {
  display: flex;
  align-items: center;
}

.mr-12 {
  margin-right: 12px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.user-name {
  font-size: 14px;
  color: #303133;
}

.layout-main {
  background-color: #f0f2f5;
  padding: 16px;
  overflow-y: auto;
}
</style>