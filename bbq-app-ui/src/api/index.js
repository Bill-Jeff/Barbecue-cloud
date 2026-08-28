import axios from 'axios'
import { showToast } from 'vant'

const api = axios.create({
  baseURL: '/app-api/flashsale/api',
  timeout: 10000,
})

// 请求拦截器：自动带 token
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

function toastError(msg) {
  showToast({
    message: msg,
    icon: 'warning-o',
    className: 'toast-error',
    duration: 2000,
  })
}

// 防止并发刷新
let isRefreshing = false
let refreshQueue = []

async function tryRefreshToken() {
  const refreshToken = localStorage.getItem('refreshToken')
  if (!refreshToken) return false
  try {
    const res = await axios.post('/app-api/flashsale/api/admin/refresh', { refreshToken })
    const body = res.data
    // 后端返回 Result 包装：code 非 0/200 视为刷新失败
    if (body && typeof body.code === 'number' && body.code !== 0 && body.code !== 200) {
      return false
    }
    const { accessToken, refreshToken: newRefreshToken } = body?.data || {}
    if (!accessToken) return false
    localStorage.setItem('token', accessToken)
    if (newRefreshToken) localStorage.setItem('refreshToken', newRefreshToken)
    return true
  } catch {
    return false
  }
}

// 统一处理 401（HTTP 401 或业务 code 401）：刷新 token 并重试，只重试一次
async function handle401(config) {
  if (!config || config._retry) return false
  config._retry = true
  if (!isRefreshing) {
    isRefreshing = true
    const success = await tryRefreshToken()
    isRefreshing = false
    if (success) {
      // 放行排队中的请求
      refreshQueue.forEach(resolve => resolve(true))
      refreshQueue = []
      config.headers.Authorization = `Bearer ${localStorage.getItem('token')}`
      return true
    }
    // 刷新失败：让排队请求结束，清空登录态跳登录页
    refreshQueue.forEach(resolve => resolve(false))
    refreshQueue = []
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    if (window.location.pathname !== '/login') {
      window.location.href = '/login'
    }
    return false
  }
  // 已有刷新在进行中，排队等待结果
  return new Promise(resolve => {
    refreshQueue.push(resolve)
  })
}

// 响应拦截器：Result 解包 + 异常处理
api.interceptors.response.use(
  async res => {
    const body = res.data
    if (body && typeof body.code === 'number') {
      if (body.code === 0 || body.code === 200) {
        res.data = body.data
        res.code = body.code
        return res
      }
      // 业务层 401（HTTP 200 + code 401「账号未登录」）：自动刷新 token 并重试一次
      if (body.code === 401) {
        const ok = await handle401(res.config)
        if (ok) return api(res.config)
        return Promise.reject(new Error(body.msg || body.message || '账号未登录'))
      }
      toastError(body.msg || body.message)
      return Promise.reject(new Error(body.msg || body.message))
    }
    return res
  },
  async err => {
    const { config, response } = err
    // HTTP 层 401：同样走刷新重试
    if (response?.status === 401) {
      const ok = await handle401(config)
      if (ok) return api(config)
      return Promise.reject(err)
    }
    const msg = response?.data?.msg || response?.data?.message || err.message || '请求失败'
    toastError(msg)
    return Promise.reject(err)
  }
)

// ========== 前台接口 ==========

export function getCategories() {
  return api.get('/categories')
}

export function getProducts(categoryId) {
  return api.get('/products', { params: { categoryId } })
}

export function createOrder(data) {
  return api.post('/orders', data)
}

// 超级管理员分页查看全部顾客订单（点单记录，滚动加载）
export function listAllOrders(offset = 0, limit = 10) {
  return api.get('/orders', { params: { offset, limit } })
}

// ========== 登录 ==========

export function adminLogin(username, password) {
  return api.post('/admin/login', { username, password })
}

export function adminCheck() {
  return api.get('/admin/check')
}

// ========== 后台管理接口 ==========

export function adminListCategories() {
  return api.get('/admin/categories')
}

export function adminSaveCategory(data) {
  return api.post('/admin/categories', data)
}

export function adminDeleteCategory(id) {
  return api.delete(`/admin/categories/${id}`)
}

export function adminListProducts() {
  return api.get('/admin/products')
}

export function adminSaveProduct(data) {
  return api.post('/admin/products', data)
}

export function adminDeleteProduct(id) {
  return api.delete(`/admin/products/${id}`)
}

export function adminUpload(file) {
  const formData = new FormData()
  formData.append('file', file)
  return api.post('/admin/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

// ========== 用户管理 ==========

export function adminListUsers() {
  return api.get('/admin/users')
}

export function adminSaveUser(data) {
  return api.post('/admin/users', data)
}

export function adminDeleteUser(id) {
  return api.delete(`/admin/users/${id}`)
}

// ========== 秒杀活动 ==========

export function getFlashSales() {
  return api.get('/flash-sales')
}

export function adminListFlashSales() {
  return api.get('/admin/flash-sales')
}

export function adminSaveFlashSale(data) {
  return api.post('/admin/flash-sales', data)
}

export function adminDeleteFlashSale(id) {
  return api.delete(`/admin/flash-sales/${id}`)
}

export function createFlashSaleOrder(data) {
  return api.post('/flash-sales/order', data)
}

// ========== 热榜 ==========

export function getHotProducts(limit = 10) {
  return api.get('/hot-products', { params: { limit } })
}

export function addPv(productId) {
  return api.post(`/pv/${productId}`)
}

// ========== AI 客服 ==========
// 小考 -> ali-chat；小石 -> open-chat

export function sendAliChat(userInputMsg) {
  const params = new URLSearchParams()
  params.append('userInputMsg', userInputMsg)
  return api.post('/ai/ali-chat', params, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  })
}

export function sendOpenChat(userInputMsg) {
  const params = new URLSearchParams()
  params.append('userInputMsg', userInputMsg)
  return api.post('/ai/open-chat', params, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  })
}
