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
    const { accessToken } = res.data.data
    localStorage.setItem('token', accessToken)
    return true
  } catch {
    return false
  }
}

// 响应拦截器：Result 解包 + 异常处理
api.interceptors.response.use(
  res => {
    const body = res.data
    if (body && typeof body.code === 'number') {
      if (body.code === 0 || body.code === 200) {
        res.data = body.data
        res.code = body.code
        return res
      }
      toastError(body.message)
      return Promise.reject(new Error(body.message))
    }
    return res
  },
  async err => {
    const { config, response } = err
    if (response?.status === 401 && !config._retry) {
      config._retry = true
      if (!isRefreshing) {
        isRefreshing = true
        const success = await tryRefreshToken()
        isRefreshing = false
        if (success) {
          // 重试队列中的请求
          refreshQueue.forEach(cb => cb())
          refreshQueue = []
          // 重试当前请求
          config.headers.Authorization = `Bearer ${localStorage.getItem('token')}`
          return api(config)
        }
        // 刷新失败，清空登录态
        localStorage.removeItem('token')
        localStorage.removeItem('refreshToken')
        window.location.href = '/login'
        return Promise.reject(err)
      }
      // 等待刷新完成
      return new Promise(resolve => {
        refreshQueue.push(() => {
          config.headers.Authorization = `Bearer ${localStorage.getItem('token')}`
          resolve(api(config))
        })
      })
    }
    const msg = response?.data?.message || err.message || '请求失败'
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

export function sendChatMsg(userInputMsg) {
  const params = new URLSearchParams()
  params.append('userInputMsg', userInputMsg)
  return api.post('/ai/chat', params, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  })
}
