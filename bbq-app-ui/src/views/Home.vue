<template>
  <div class="home">
    <!-- ===== 菜单 Tab ===== -->
    <div v-show="activeTab === 0" class="page">
      <!-- Hero -->
      <section class="hero">
        <div class="hero-embers">
          <span v-for="n in 6" :key="n" class="ember-dot" :style="emberStyle(n)"></span>
        </div>
        <button class="admin-entry" @click="router.push('/m-admin')">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z"/><circle cx="12" cy="12" r="3"/></svg>
        </button>
        <div class="hero-badge">炭火现烤 · 夜宵优选</div>
        <h1 class="hero-title">今晚吃点<br><span class="accent">热辣烧烤</span></h1>
        <p class="hero-desc">手机轻松点单，肉串海鲜蔬菜一页搞定，支持现点现烤与口味备注。</p>
        <div class="hero-stats">
          <div class="hero-stat">
            <span class="stat-num">18</span>
            <span class="stat-unit">min</span>
            <span class="stat-label">平均出餐</span>
          </div>
          <div class="stat-divider"></div>
          <div class="hero-stat">
            <span class="stat-num">4.9</span>
            <span class="stat-unit">分</span>
            <span class="stat-label">人气好评</span>
          </div>
          <div class="stat-divider"></div>
          <div class="hero-stat">
            <span class="stat-num">¥20</span>
            <span class="stat-unit">起</span>
            <span class="stat-label">深夜可点</span>
          </div>
        </div>
      </section>

      <!-- 秒杀入口 -->
      <div class="flash-entry" :class="{ 'flash-entry--empty': !flashSale }" @click="flashSale && router.push('/flash-sale')">
        <div class="flash-entry-left">
          <div class="flash-entry-icon">⚡</div>
          <div class="flash-entry-info">
            <span class="flash-entry-title">限时秒杀</span>
            <span class="flash-entry-countdown" v-if="flashSale">{{ flashEntryCountdown }}</span>
            <span class="flash-entry-countdown flash-countdown-empty" v-else>暂无活动</span>
          </div>
        </div>
        <div class="flash-entry-right" v-if="flashSale">
          <span class="flash-entry-price">¥{{ Number(flashSale.price).toFixed(0) }}</span>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
        </div>
      </div>

      <!-- 分类 -->
      <div class="section-title">
        <h2>招牌分类</h2>
        <span class="section-hint">左右滑动</span>
      </div>
      <div class="chips">
        <button
          v-for="cat in categories"
          :key="cat.id"
          class="chip"
          :class="{ active: activeCategory === cat.id }"
          @click="activeCategory = cat.id"
        >
          <span class="chip-icon">{{ getCatIcon(cat.name) }}</span>
          {{ cat.name }}
        </button>
      </div>

      <!-- 菜品列表 -->
      <div class="section-title">
        <h2>{{ currentCategoryName }}</h2>
        <span class="section-hint">新鲜现串，辣度可调</span>
      </div>
      <section class="menu-list">
        <article
          v-for="(item, index) in filteredProducts"
          :key="item.id"
          class="card"
          :style="{ animationDelay: index * 80 + 'ms' }"
        >
          <div class="card-visual">
            <div class="thumb">
              <img v-if="item.image || item.imageUrl" :src="item.image || item.imageUrl" class="thumb-img" />
              <img v-else :src="item.foodImage" class="thumb-img" />
            </div>
            <div class="thumb-glow"></div>
          </div>
          <div class="card-body">
            <div class="card-header">
              <h3>{{ item.name }}</h3>
              <span class="card-price">¥{{ item.price.toFixed(0) }}</span>
            </div>
            <p class="desc">{{ item.description }}</p>
            <div class="tags">
              <span class="tag" v-for="tag in item.tags" :key="tag">{{ tag }}</span>
            </div>
            <div class="card-bottom">
              <span class="unit-label">{{ item.unit }} / 份量足</span>
              <div class="stepper">
                <button type="button" v-if="getQty(item.id) > 0" class="stepper-minus" @click="cart.removeItem(item.id)">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><line x1="5" y1="12" x2="19" y2="12"/></svg>
                </button>
                <strong v-if="getQty(item.id) > 0" class="stepper-count">{{ getQty(item.id) }}</strong>
                <button type="button" class="stepper-plus" @click="cart.addItem(item)">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                </button>
              </div>
            </div>
          </div>
        </article>
      </section>

      <section class="notice">
        <div class="notice-icon">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
        </div>
        <p>默认微辣微孜然，如需"免辣 / 加辣 / 不要香菜 / 单独打包"，下单时备注即可。</p>
      </section>

      <!-- 购物车悬浮栏 -->
      <aside class="cart-bar" v-if="cart.totalCount > 0" :class="{ expanded: cartExpanded }">
        <!-- 展开的商品列表 -->
        <transition name="cart-list">
          <div class="cart-dropdown" v-show="cartExpanded">
            <div class="cart-dropdown-header">
              <span class="cart-dropdown-title">已选商品</span>
              <button class="cart-clear-btn" @click="cart.clear(); cartExpanded = false">清空</button>
            </div>
            <div class="cart-dropdown-list">
              <div class="cart-dropdown-item" v-for="item in cart.items" :key="item.productId">
                <span class="cdi-name">{{ item.name }}</span>
                <span class="cdi-price">¥{{ item.price.toFixed(0) }}</span>
                <div class="cdi-stepper">
                  <button class="cdi-btn" @click="cart.removeItem(item.productId)">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><line x1="5" y1="12" x2="19" y2="12"/></svg>
                  </button>
                  <span class="cdi-qty">{{ item.quantity }}</span>
                  <button class="cdi-btn cdi-btn-plus" @click="cart.addItem({ id: item.productId, name: item.name, price: item.price })">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </transition>
        <!-- 底栏 -->
        <div class="cart-bar-inner" @click="cartExpanded = !cartExpanded">
          <div class="cart-left">
            <div class="cart-icon-wrap">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/></svg>
              <span class="cart-badge">{{ cart.totalCount }}</span>
            </div>
            <div class="cart-info">
              <strong>¥{{ cart.totalPrice.toFixed(0) }}</strong>
              <small>{{ cartSummary }}</small>
            </div>
          </div>
          <button class="checkout-btn" @click.stop="router.push('/cart')">去结算</button>
        </div>
      </aside>
    </div>

    <!-- ===== 点单记录 Tab ===== -->
    <div v-show="activeTab === 1" class="page">
      <div class="section-title">
        <h2>点单记录</h2>
        <span class="section-hint">{{ orders.length }} 笔订单</span>
      </div>
      <div v-if="orders.length === 0" class="empty-state">
        <div class="empty-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" opacity="0.3"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
        </div>
        <p>暂无点单记录</p>
        <button class="empty-action" @click="activeTab = 0">去点单</button>
      </div>
      <div v-else class="order-list">
        <div class="order-card" v-for="order in orders" :key="order.id">
          <div class="order-header">
            <div class="order-meta">
              <span class="order-time">{{ order.time }}</span>
              <span class="order-table">桌号 {{ order.tableNo }}</span>
            </div>
            <span class="order-total">¥{{ order.total.toFixed(2) }}</span>
          </div>
          <div class="order-items">
            <div class="order-item" v-for="item in order.items" :key="item.productId">
              <img class="oi-img" :src="getFoodImage(item.productName)" />
              <span class="oi-name">{{ item.productName }}</span>
              <span class="oi-qty">×{{ item.quantity }}</span>
              <span class="oi-price">¥{{ (item.price * item.quantity).toFixed(2) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ===== 热榜 Tab ===== -->
    <div v-show="activeTab === 2" class="page">
      <div class="section-title">
        <h2>热门排行</h2>
        <span class="section-hint">实时热度</span>
      </div>
      <div v-if="hotLoading" class="empty-state">
        <p>加载中...</p>
      </div>
      <div v-else-if="hotProducts.length === 0" class="empty-state">
        <p>暂无热度数据</p>
      </div>
      <div v-else class="hot-list">
        <div class="hot-item" v-for="(item, index) in hotProducts" :key="item.id">
          <span class="hot-rank" :class="{ 'hot-rank--top': index < 3 }">{{ index + 1 }}</span>
          <img v-if="item.image" :src="item.image" class="hot-img" />
          <div class="hot-info">
            <span class="hot-name">{{ item.name }}</span>
            <span class="hot-price">¥{{ Number(item.price).toFixed(0) }}</span>
          </div>
          <span class="hot-pv">{{ item.pv }} 次浏览</span>
        </div>
      </div>
      <!-- 客服入口 -->
      <div class="cs-entry" @click="router.push('/chat')">
        <div class="cs-entry-left">
          <span class="cs-avatar">🐨</span>
          <span class="cs-text">还是不知道吃什么，来找小考问问</span>
        </div>
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="cs-arrow">
          <polyline points="9 18 15 12 9 6"/>
        </svg>
      </div>
    </div>

    <!-- ===== 我的 Tab ===== -->
    <div v-show="activeTab === 3" class="page">
      <div class="profile-section">
        <div class="profile-card">
          <div class="profile-avatar">{{ isLoggedIn ? (nickname?.charAt(0) || 'U') : '?' }}</div>
          <div class="profile-info">
            <div class="profile-name">{{ isLoggedIn ? (nickname || '用户') : '未登录' }}</div>
            <div class="profile-role" v-if="isLoggedIn">
              <span class="role-tag" :class="role === 'admin' ? 'role-admin' : 'role-user'">
                {{ role === 'admin' ? '管理员' : '用户' }}
              </span>
            </div>
            <div class="profile-hint" v-else>登录后可查看点单记录</div>
          </div>
        </div>

        <div class="profile-menu" v-if="isLoggedIn">
          <button class="menu-item" @click="activeTab = 1">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
            <span>点单记录</span>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="arrow"><polyline points="9 18 15 12 9 6"/></svg>
          </button>
          <button class="menu-item" @click="router.push('/m-admin')" v-if="role === 'admin'">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z"/><circle cx="12" cy="12" r="3"/></svg>
            <span>管理后台</span>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="arrow"><polyline points="9 18 15 12 9 6"/></svg>
          </button>
          <button class="menu-item menu-item--danger" @click="logout">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
            <span>退出登录</span>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="arrow"><polyline points="9 18 15 12 9 6"/></svg>
          </button>
        </div>

        <div v-else class="login-prompt">
          <button class="login-btn" @click="router.push('/login')">去登录</button>
        </div>
      </div>
    </div>

    <!-- Liquid Glass Dock -->
    <nav class="dock">
      <div class="dock-track">
        <div class="dock-indicator" :style="{ transform: `translateX(${activeTab * 100}%)` }"></div>
        <button
          v-for="(item, i) in dockItems"
          :key="i"
          class="dock-item"
          :class="{ active: activeTab === i }"
          @click="activeTab = i"
        >
          <div class="dock-icon">
            <component :is="item.icon" />
          </div>
          <span class="dock-label">{{ item.label }}</span>
        </button>
      </div>
    </nav>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, h } from 'vue'
import { useRouter } from 'vue-router'
import { getCategories, getProducts, getFlashSales, getHotProducts } from '../api'
import { useCartStore } from '../stores/cart'

const router = useRouter()
const cart = useCartStore()

const activeTab = ref(0)
const activeCategory = ref(null)
const flashSale = ref(null)
const flashEntryCountdown = ref('')
const cartExpanded = ref(false)
let flashTimer = null

// Dock icons as render functions
const MenuIcon = () => h('svg', { width: 22, height: 22, viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }, [
  h('path', { d: 'M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z' }),
  h('polyline', { points: '9 22 9 12 15 12 15 22' }),
])
const OrderIcon = () => h('svg', { width: 22, height: 22, viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }, [
  h('path', { d: 'M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z' }),
  h('polyline', { points: '14 2 14 8 20 8' }),
  h('line', { x1: '16', y1: '13', x2: '8', y2: '13' }),
  h('line', { x1: '16', y1: '17', x2: '8', y2: '17' }),
])
const UserIcon = () => h('svg', { width: 22, height: 22, viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }, [
  h('path', { d: 'M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2' }),
  h('circle', { cx: '12', cy: '7', r: '4' }),
])
const HotIcon = () => h('svg', { width: 22, height: 22, viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }, [
  h('path', { d: 'M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z' }),
])

const dockItems = [
  { label: '菜单', icon: MenuIcon },
  { label: '记录', icon: OrderIcon },
  { label: '热榜', icon: HotIcon },
  { label: '我的', icon: UserIcon },
]
const categories = ref([])
const products = ref([])
const orders = ref([])
const hotProducts = ref([])
const hotLoading = ref(false)

const isLoggedIn = ref(false)
const nickname = ref('')
const role = ref('')

function refreshAuth() {
  isLoggedIn.value = !!localStorage.getItem('token')
  nickname.value = localStorage.getItem('nickname') || ''
  role.value = localStorage.getItem('role') || ''
}

const emojiMap = {
  '羊肉串': '🍢', '牛肉串': '🥩', '鸡翅': '🍗', '五花肉': '🥓',
  '烤腰子': '🫘', '烤生蚝': '🦪', '烤鱿鱼': '🦑', '烤大虾': '🦐',
  '烤馒头片': '🍞', '烤韭菜': '🥬', '烤茄子': '🍆',
  '啤酒': '🍺', '王老吉': '🧃', '酸梅汤': '🥤',
}
const imageMap = {
  '羊肉串': 'https://images.unsplash.com/photo-1555939594-58d7cb561ad1?w=200&h=200&fit=crop',
  '牛肉串': 'https://images.unsplash.com/photo-1544025162-d76694265947?w=200&h=200&fit=crop',
  '鸡翅': 'https://images.unsplash.com/photo-1527477396000-e27163b4bbed?w=200&h=200&fit=crop',
  '五花肉': 'https://images.unsplash.com/photo-1544025162-d76694265947?w=200&h=200&fit=crop&q=80',
  '烤腰子': 'https://images.unsplash.com/photo-1555939594-58d7cb561ad1?w=200&h=200&fit=crop&q=80',
  '烤生蚝': 'https://images.unsplash.com/photo-1559737558-2f5a35f4523b?w=200&h=200&fit=crop',
  '烤鱿鱼': 'https://images.unsplash.com/photo-1565680018093-ebb6505b4d59?w=200&h=200&fit=crop',
  '烤大虾': 'https://images.unsplash.com/photo-1559737558-2f5a35f4523b?w=200&h=200&fit=crop&q=80',
  '烤馒头片': 'https://images.unsplash.com/photo-1509440159596-0249088772ff?w=200&h=200&fit=crop',
  '烤韭菜': 'https://images.unsplash.com/photo-1540420773420-3366772f4999?w=200&h=200&fit=crop',
  '烤茄子': 'https://images.unsplash.com/photo-1518977956812-cd3dbadaaf31?w=200&h=200&fit=crop',
  '啤酒': 'https://images.unsplash.com/photo-1535958636474-b021ee887b13?w=200&h=200&fit=crop',
  '王老吉': 'https://images.unsplash.com/photo-1544145945-f90425340c7e?w=200&h=200&fit=crop',
  '酸梅汤': 'https://images.unsplash.com/photo-1544145945-f90425340c7e?w=200&h=200&fit=crop&q=80',
}
const tagsMap = {
  '羊肉串': ['销量王', '推荐加辣'], '牛肉串': ['低脂感', '鲜嫩'],
  '鸡翅': ['招牌酱料', '现烤'], '五花肉': ['肥瘦相间', '经典'],
  '烤腰子': ['滋补', '限量'], '烤生蚝': ['蒜蓉控', '海味'],
  '烤鱿鱼': ['弹嫩', '甜辣口'], '烤大虾': ['鲜甜', '必点'],
  '烤馒头片': ['碳水快乐', '解馋'], '烤韭菜': ['解腻', '清香'],
  '烤茄子': ['蒜香爆棚', '经典'], '啤酒': ['冰镇', '必搭'],
  '王老吉': ['降火', '冰爽'], '酸梅汤': ['自制', '解渴'],
}
const unitMap = { '啤酒': '瓶', '王老吉': '罐', '酸梅汤': '杯' }
const catIconMap = { '烤串': '🍢', '海鲜': '🦪', '蔬菜': '🥬', '主食': '🍞', '酒水': '🍺' }

function getEmoji(name) { return emojiMap[name] || '🔥' }
function getFoodImage(name) { return imageMap[name] || imageMap['羊肉串'] }
function getCatIcon(name) { return catIconMap[name] || '🔥' }

const enrichedProducts = computed(() =>
  products.value.map(p => ({
    ...p,
    emoji: emojiMap[p.name] || '🔥',
    foodImage: imageMap[p.name] || imageMap['羊肉串'],
    tags: tagsMap[p.name] || ['推荐'],
    unit: unitMap[p.name] || '串',
  }))
)
const filteredProducts = computed(() =>
  enrichedProducts.value.filter(p => p.categoryId === activeCategory.value)
)
const currentCategoryName = computed(() => {
  const cat = categories.value.find(c => c.id === activeCategory.value)
  return cat ? cat.name : '推荐菜品'
})
const cartSummary = computed(() => {
  if (cart.items.length === 0) return '先挑几串喜欢的吧'
  const names = cart.items.slice(0, 3).map(i => `${i.name} x${i.quantity}`).join('、')
  return cart.items.length > 3 ? `${names} 等 ${cart.items.length} 款` : names
})

function getQty(productId) {
  const item = cart.items.find(i => i.productId === productId)
  return item ? item.quantity : 0
}

function loadOrders() {
  try { orders.value = JSON.parse(localStorage.getItem('shaokao_orders') || '[]') }
  catch { orders.value = [] }
}

async function loadHotProducts() {
  hotLoading.value = true
  try {
    const res = await getHotProducts(10)
    hotProducts.value = res.data || []
  } catch { hotProducts.value = [] }
  finally { hotLoading.value = false }
}

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('role')
  localStorage.removeItem('nickname')
  refreshAuth()
  activeTab.value = 0
}

function emberStyle(n) {
  const x = 15 + Math.random() * 70
  const delay = n * 0.8 + Math.random() * 2
  const dur = 3 + Math.random() * 3
  const size = 2 + Math.random() * 3
  return {
    left: x + '%',
    bottom: '10%',
    width: size + 'px',
    height: size + 'px',
    animationDelay: delay + 's',
    animationDuration: dur + 's',
  }
}

function updateFlashCountdown() {
  if (!flashSale.value) return
  const end = new Date(flashSale.value.saleTime).getTime()
  const diff = end - Date.now()
  if (diff <= 0) {
    flashEntryCountdown.value = '即将售罄'
    return
  }
  const h = Math.floor(diff / 3600000)
  const m = Math.floor((diff % 3600000) / 60000)
  const s = Math.floor((diff % 60000) / 1000)
  flashEntryCountdown.value = `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

onMounted(async () => {
  refreshAuth()
  const [catRes, prodRes, flashRes] = await Promise.all([
    getCategories(), getProducts(), getFlashSales().catch(() => ({ data: { data: [] } })),
  ])
  categories.value = catRes.data
  products.value = prodRes.data.list
  if (categories.value.length > 0) activeCategory.value = categories.value[0].id
  loadOrders()
  loadHotProducts()
  // 取最近的秒杀活动
  if (flashRes.data && flashRes.data.length > 0) {
    flashSale.value = flashRes.data[0]
    updateFlashCountdown()
    flashTimer = setInterval(updateFlashCountdown, 1000)
  }
})

watch(activeTab, (val) => {
  if (val === 2) loadHotProducts()
})

onUnmounted(() => {
  if (flashTimer) clearInterval(flashTimer)
})
</script>

<style scoped>
.home {
  min-height: 100vh;
  padding-bottom: 100px;
}

.page {
  position: relative;
  z-index: 1;
  width: min(100%, 430px);
  margin: 0 auto;
  padding: 16px 16px 20px;
}

/* ===== Hero ===== */
.hero {
  position: relative;
  overflow: hidden;
  padding: 28px 22px 24px;
  border-radius: var(--radius-xl);
  background: var(--surface);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--line);
  box-shadow: var(--shadow);
  animation: rise-in 0.7s ease-out both;
}
.hero::before {
  content: "";
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 90% 10%, rgba(232, 98, 44, 0.06) 0%, transparent 50%);
  pointer-events: none;
}

.hero-embers {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}
.ember-dot {
  position: absolute;
  border-radius: 50%;
  background: var(--ember);
  box-shadow: 0 0 6px var(--ember), 0 0 12px rgba(255, 78, 32, 0.5);
  animation: smoke-drift 4s ease-out infinite;
  opacity: 0;
}

.admin-entry {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 2;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid var(--line);
  background: var(--surface-glass);
  color: var(--muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}
.admin-entry:active {
  background: rgba(0, 0, 0, 0.06);
  transform: scale(0.92);
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  border-radius: 999px;
  border: 1px solid rgba(232, 98, 44, 0.25);
  background: rgba(232, 98, 44, 0.1);
  font-size: 11px;
  letter-spacing: 0.12em;
  color: var(--accent-bright);
  font-weight: 500;
}

.hero-title {
  margin: 16px 0 12px;
  font-family: var(--font-display);
  font-size: 36px;
  line-height: 1.15;
  font-weight: 400;
  color: var(--text-bright);
}
.hero-title .accent {
  background: linear-gradient(135deg, var(--accent-bright), var(--gold));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-desc {
  margin: 0;
  max-width: 260px;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.7;
}

.hero-stats {
  display: flex;
  align-items: center;
  gap: 0;
  margin-top: 20px;
  padding: 14px 0;
  border-top: 1px solid var(--line);
}
.hero-stat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}
.stat-num {
  font-size: 22px;
  font-weight: 900;
  color: var(--text-bright);
  line-height: 1;
}
.stat-unit {
  font-size: 11px;
  color: var(--muted-soft);
  margin-left: 2px;
}
.stat-label {
  font-size: 11px;
  color: var(--muted-soft);
  margin-top: 4px;
}
.stat-divider {
  width: 1px;
  height: 32px;
  background: var(--line);
}

/* ===== Flash sale entry ===== */
.flash-entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14px;
  padding: 14px 18px;
  border-radius: var(--radius-xl);
  background: linear-gradient(135deg, #fff5f0, #fff0e8);
  border: 1px solid rgba(255, 78, 32, 0.2);
  cursor: pointer;
  animation: rise-in 0.6s ease both;
  transition: transform 0.15s;
}
.flash-entry:active { transform: scale(0.98); }
.flash-entry--empty {
  opacity: 0.6;
  cursor: default;
}
.flash-countdown-empty {
  color: var(--muted-soft);
  font-size: 13px;
  font-weight: 500;
}

.flash-entry-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.flash-entry-icon {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff4e20, #ff7a3d);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  box-shadow: 0 4px 16px rgba(255, 78, 32, 0.35);
}
.flash-entry-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.flash-entry-title {
  font-size: 16px;
  font-weight: 800;
  color: var(--text-bright);
}
.flash-entry-countdown {
  font-size: 15px;
  font-weight: 800;
  color: var(--ember);
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.04em;
}
.flash-entry-right {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--ember);
}
.flash-entry-price {
  font-size: 18px;
  font-weight: 900;
}

/* ===== Section title ===== */
.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 24px 2px 12px;
}
.section-title h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 900;
  color: var(--text-bright);
  letter-spacing: 0.02em;
}
.section-hint {
  color: var(--muted-soft);
  font-size: 12px;
}

/* ===== Chips ===== */
.chips {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding: 4px 2px;
  scrollbar-width: none;
}
.chips::-webkit-scrollbar { display: none; }

.chip {
  flex: 0 0 auto;
  border: 0;
  padding: 10px 16px;
  border-radius: 999px;
  background: var(--surface);
  color: var(--muted);
  box-shadow: inset 0 0 0 1px var(--line);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;
  display: flex;
  align-items: center;
  gap: 6px;
}
.chip-icon { font-size: 15px; }
.chip.active {
  background: linear-gradient(135deg, var(--accent), var(--accent-bright));
  color: #fff;
  box-shadow: 0 8px 24px rgba(232, 98, 44, 0.3);
  transform: translateY(-1px);
}

/* ===== Menu cards ===== */
.menu-list {
  display: grid;
  gap: 14px;
  margin-top: 14px;
}
.card {
  display: grid;
  grid-template-columns: 96px 1fr;
  gap: 14px;
  padding: 14px;
  border-radius: var(--radius-lg);
  background: var(--surface);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--line);
  animation: rise-in 0.5s ease both;
  transition: transform 0.2s, box-shadow 0.2s;
}
.card:active {
  transform: scale(0.985);
}

.card-visual {
  position: relative;
}
.thumb {
  width: 96px;
  height: 96px;
  border-radius: var(--radius-md);
  background: rgba(255, 245, 230, 0.8);
  display: grid;
  place-items: center;
  overflow: hidden;
  box-shadow: inset 0 -4px 12px rgba(0, 0, 0, 0.04);
}
.thumb-emoji {
  font-size: 42px;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.3));
}
.thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.thumb-glow {
  position: absolute;
  bottom: -4px;
  left: 20%;
  right: 20%;
  height: 8px;
  border-radius: 50%;
  background: rgba(232, 98, 44, 0.2);
  filter: blur(6px);
}

.card-body {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
}
.card-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--text-bright);
  line-height: 1.3;
}
.card-price {
  font-size: 20px;
  font-weight: 900;
  color: var(--accent-bright);
  line-height: 1;
  white-space: nowrap;
}
.desc {
  margin: 6px 0 0;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}
.tag {
  padding: 3px 10px;
  border-radius: 999px;
  background: var(--accent-soft);
  color: var(--accent-bright);
  font-size: 11px;
  font-weight: 600;
  border: 1px solid rgba(232, 98, 44, 0.15);
}

.card-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
  padding-top: 10px;
}
.unit-label {
  font-size: 11px;
  color: var(--muted-soft);
}

/* Stepper */
.stepper {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.stepper-minus,
.stepper-plus {
  width: 30px;
  height: 30px;
  border: 0;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}
.stepper-minus {
  background: var(--surface-glass);
  color: var(--muted);
  border: 1px solid var(--line-bright);
}
.stepper-plus {
  background: linear-gradient(135deg, var(--accent), var(--accent-bright));
  color: #fff;
  box-shadow: 0 4px 16px rgba(232, 98, 44, 0.35);
}
.stepper-minus:active,
.stepper-plus:active {
  transform: scale(0.88);
}
.stepper-count {
  width: 20px;
  text-align: center;
  font-size: 15px;
  font-weight: 800;
  color: var(--text-bright);
}

/* ===== Cart bar ===== */
.cart-bar {
  position: fixed;
  left: 50%;
  bottom: 90px;
  transform: translateX(-50%);
  width: min(calc(100% - 24px), 410px);
  z-index: 10;
  animation: float-up 0.6s ease-out both;
}
.cart-bar-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 12px 12px 16px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--line);
  box-shadow:
    0 12px 40px rgba(160, 100, 40, 0.1),
    0 2px 8px rgba(160, 100, 40, 0.04);
}
.cart-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.cart-icon-wrap {
  position: relative;
  color: var(--accent-bright);
}
.cart-badge {
  position: absolute;
  top: -6px;
  right: -8px;
  min-width: 18px;
  height: 18px;
  border-radius: 999px;
  background: var(--ember);
  color: #fff;
  font-size: 10px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  box-shadow: 0 2px 8px rgba(255, 78, 32, 0.5);
}
.cart-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.cart-info strong {
  font-size: 20px;
  font-weight: 900;
  color: var(--text-bright);
}
.cart-info small {
  font-size: 11px;
  color: var(--muted-soft);
}
.checkout-btn {
  border: 0;
  padding: 12px 24px;
  border-radius: 14px;
  background: linear-gradient(135deg, var(--accent), var(--gold));
  color: #fff;
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(232, 98, 44, 0.3);
  transition: transform 0.15s;
}
.checkout-btn:active {
  transform: scale(0.94);
}

/* Cart dropdown */
.cart-dropdown {
  margin-bottom: 10px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--line);
  box-shadow: 0 8px 32px rgba(160, 100, 40, 0.12);
  overflow: hidden;
  max-height: 260px;
  display: flex;
  flex-direction: column;
}
.cart-dropdown-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px 8px;
  border-bottom: 1px solid var(--line);
}
.cart-dropdown-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-bright);
}
.cart-clear-btn {
  border: 0;
  background: none;
  color: var(--muted-soft);
  font-size: 12px;
  cursor: pointer;
  padding: 2px 6px;
}
.cart-clear-btn:active { color: #ef5350; }
.cart-dropdown-list {
  overflow-y: auto;
  padding: 4px 0;
}
.cart-dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
}
.cdi-name {
  flex: 1;
  font-size: 13px;
  font-weight: 500;
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cdi-price {
  font-size: 14px;
  font-weight: 700;
  color: var(--accent-bright);
  min-width: 40px;
  text-align: right;
}
.cdi-stepper {
  display: flex;
  align-items: center;
  gap: 6px;
}
.cdi-btn {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 1px solid var(--line-bright);
  background: var(--surface-glass);
  color: var(--muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  padding: 0;
}
.cdi-btn-plus {
  background: linear-gradient(135deg, var(--accent), var(--accent-bright));
  border: none;
  color: #fff;
}
.cdi-btn:active { transform: scale(0.88); }
.cdi-qty {
  width: 18px;
  text-align: center;
  font-size: 14px;
  font-weight: 700;
  color: var(--text-bright);
}

/* Cart list transition */
.cart-list-enter-active,
.cart-list-leave-active {
  transition: all 0.25s ease;
}
.cart-list-enter-from,
.cart-list-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

/* ===== Notice ===== */
.notice {
  display: flex;
  gap: 12px;
  margin: 20px 2px 0;
  padding: 16px;
  border-radius: var(--radius-lg);
  background: var(--surface);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid var(--line);
}
.notice-icon {
  flex-shrink: 0;
  color: var(--gold);
  margin-top: 1px;
}
.notice p {
  margin: 0;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.7;
}

/* ===== Order history ===== */
.order-list {
  display: grid;
  gap: 12px;
}
.order-card {
  background: var(--surface);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-radius: var(--radius-lg);
  border: 1px solid var(--line);
  overflow: hidden;
  animation: rise-in 0.4s ease both;
}
.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  border-bottom: 1px solid var(--line);
}
.order-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.order-time {
  font-size: 13px;
  color: var(--text);
  font-weight: 500;
}
.order-table {
  font-size: 11px;
  color: var(--muted-soft);
}
.order-total {
  font-size: 20px;
  font-weight: 900;
  color: var(--accent-bright);
}
.order-items {
  padding: 6px 16px 10px;
}
.order-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  font-size: 13px;
}
.oi-img { width: 28px; height: 28px; border-radius: 6px; object-fit: cover; }
.oi-name { flex: 1; font-weight: 500; color: var(--text); }
.oi-qty { color: var(--muted-soft); font-size: 12px; }
.oi-price { color: var(--accent-bright); font-weight: 700; min-width: 60px; text-align: right; }

/* ===== Empty state ===== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 20px;
}
.empty-icon {
  margin-bottom: 16px;
  color: var(--muted-soft);
}
.empty-state p {
  color: var(--muted-soft);
  font-size: 14px;
  margin-bottom: 24px;
}
.empty-action {
  padding: 10px 32px;
  border-radius: 999px;
  border: 1px solid var(--line-bright);
  background: var(--surface);
  color: var(--text);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.empty-action:active {
  transform: scale(0.95);
  background: var(--surface-raised);
}

/* ===== Hot list ===== */
.hot-list {
  display: grid;
  gap: 10px;
}
.hot-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  border-radius: var(--radius-lg);
  background: var(--surface);
  border: 1px solid var(--line);
  animation: rise-in 0.4s ease both;
}
.hot-rank {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 800;
  color: var(--muted-soft);
  background: var(--surface-glass);
  flex-shrink: 0;
}
.hot-rank--top {
  background: linear-gradient(135deg, var(--ember), #ff7a3d);
  color: #fff;
}
.hot-img {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
}
.hot-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}
.hot-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.hot-price {
  font-size: 13px;
  color: var(--accent-bright);
  font-weight: 700;
}
.hot-pv {
  font-size: 13px;
  font-weight: 700;
  color: var(--accent-bright);
  white-space: nowrap;
  flex-shrink: 0;
}

/* ===== Customer service entry ===== */
.cs-entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  padding: 16px 18px;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, #fff8f0, #fff0e0);
  border: 1px solid rgba(240, 168, 48, 0.2);
  cursor: pointer;
  animation: rise-in 0.5s ease both;
  transition: transform 0.15s;
}
.cs-entry:active { transform: scale(0.98); }
.cs-entry-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.cs-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ffecd2, #fcb69f);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}
.cs-text {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-bright);
}
.cs-arrow {
  color: var(--muted);
  flex-shrink: 0;
}

/* ===== Profile ===== */
.profile-section {
  padding-top: 10px;
}
.profile-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 28px 22px;
  margin-bottom: 20px;
  border-radius: var(--radius-xl);
  background: var(--surface);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid var(--line);
  box-shadow: var(--shadow);
}
.profile-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent), var(--gold));
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 800;
  color: #fff;
  box-shadow: 0 4px 16px rgba(232, 98, 44, 0.3);
  flex-shrink: 0;
}
.profile-info { flex: 1; }
.profile-name {
  font-size: 20px;
  font-weight: 800;
  color: var(--text-bright);
  margin-bottom: 4px;
}
.profile-hint {
  font-size: 12px;
  color: var(--muted-soft);
}
.role-tag {
  display: inline-flex;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
}
.role-admin {
  background: rgba(255, 78, 32, 0.15);
  color: var(--ember);
  border: 1px solid rgba(255, 78, 32, 0.2);
}
.role-user {
  background: rgba(76, 175, 80, 0.12);
  color: #66bb6a;
  border: 1px solid rgba(76, 175, 80, 0.2);
}

/* Profile menu */
.profile-menu {
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: var(--surface);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid var(--line);
}
.menu-item {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 16px 18px;
  border: 0;
  background: transparent;
  color: var(--text);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border-bottom: 1px solid var(--line);
  transition: background 0.15s;
  text-align: left;
}
.menu-item:last-child { border-bottom: 0; }
.menu-item:active { background: var(--surface-glass); }
.menu-item span { flex: 1; }
.menu-item .arrow { color: var(--muted-soft); }
.menu-item--danger { color: #ef5350; }

/* Login prompt */
.login-prompt {
  text-align: center;
  padding: 60px 20px;
}
.login-btn {
  padding: 14px 48px;
  border-radius: 999px;
  border: none;
  background: linear-gradient(135deg, var(--accent), var(--accent-bright));
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 12px 32px rgba(232, 98, 44, 0.3);
  transition: transform 0.15s;
}
.login-btn:active { transform: scale(0.95); }

@media (max-width: 380px) {
  .hero-title { font-size: 30px; }
  .card { grid-template-columns: 1fr; }
  .thumb { height: 120px; width: 100%; }
  .hero-stats { gap: 0; }
}

/* ===== Liquid Glass Dock ===== */
.dock {
  position: fixed;
  left: 50%;
  bottom: 12px;
  transform: translateX(-50%);
  z-index: 100;
  width: min(calc(100% - 32px), 320px);
  animation: dock-rise 0.7s cubic-bezier(0.16, 1, 0.3, 1) both;
}
@keyframes dock-rise {
  from { opacity: 0; transform: translateX(-50%) translateY(30px) scale(0.9); }
  to { opacity: 1; transform: translateX(-50%) translateY(0) scale(1); }
}

.dock-track {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: 8px 6px 10px;
  border-radius: 28px;
  /* Frosted matte glass — warm */
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(24px) saturate(1.2);
  -webkit-backdrop-filter: blur(24px) saturate(1.2);
  border: 1px solid var(--line);
  box-shadow:
    0 8px 30px rgba(160, 100, 40, 0.08),
    0 2px 8px rgba(160, 100, 40, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  overflow: hidden;
}

/* Active indicator pill */
.dock-indicator {
  position: absolute;
  top: 4px;
  left: 4px;
  width: calc(25% - 3px);
  height: calc(100% - 12px);
  border-radius: 22px;
  background: rgba(232, 98, 44, 0.08);
  transition: transform 0.45s cubic-bezier(0.34, 1.56, 0.64, 1);
  z-index: 0;
}

.dock-item {
  position: relative;
  z-index: 1;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  padding: 6px 0;
  border: none;
  background: none;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.dock-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  color: var(--muted);
  transition: color 0.35s ease, transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.dock-item.active .dock-icon {
  color: var(--accent);
  transform: scale(1.15);
}

.dock-label {
  font-size: 10px;
  font-weight: 600;
  color: var(--muted-soft);
  transition: color 0.35s ease;
  letter-spacing: 0.04em;
}

.dock-item.active .dock-label {
  color: var(--accent);
}
</style>
