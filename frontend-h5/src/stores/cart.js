import { defineStore } from 'pinia'
import { getCartCount } from '../api/cart'

/**
 * 购物车 store：只维护件数角标，明细数据由各页面自行请求
 */
export const useCartStore = defineStore('cart', {
  state: () => ({
    count: 0
  }),

  actions: {
    setCount(count) {
      this.count = Number(count) || 0
    },

    addCount(delta) {
      this.count = Math.max(0, this.count + (Number(delta) || 0))
    },

    /** 从服务端同步角标（未登录或失败时静默忽略） */
    async loadCount() {
      try {
        const count = await getCartCount()
        this.setCount(count)
      } catch (e) {
        // 忽略：未登录 / 网络异常时角标不更新
      }
    },

    reset() {
      this.count = 0
    }
  }
})