<template>
  <div class="user-home">
    <van-nav-bar
      :title="activeTab === 0 ? '菜单' : '点单记录'"
      placeholder fixed
    >
      <template #right>
        <van-icon name="ellipsis" size="20" @click="showActions = true" />
      </template>
    </van-nav-bar>

    <!-- ===== 菜单 Tab ===== -->
    <div v-show="activeTab === 0" class="tab-content">
      <!-- 分类 -->
      <div class="chips">
        <button
          v-for="cat in categories"
          :key="cat.id"
          class="chip"
          :class="{ active: activeCategory === cat.id }"
          @click="activeCategory = cat.id"
        >{{ cat.name }}</button>
      </div>

      <!-- 菜品列表 -->
      <section class="menu-list">
        <article
          v-for="(item, index) in filteredProducts"
          :key="item.id"
          class="card"
          :style="{ animationDelay: index * 60 + 'ms' }"
        >
          <div class="thumb">
            <img v-if="item.image" :src="item.image" class="thumb-img" />
            <img v-else :src="item.foodImage" class="thumb-img" />
          </div>
          <div>
            <h3>{{ item.name }}</h3>
            <p class="desc">{{ item.description }}</p>
            <div class="tags">
              <span class="tag" v-for="tag in item.tags" :key="tag">{{ tag }}</span>
            </div>
            <div class="card-bottom">
              <div class="price">
                <strong>¥{{ item.price.toFixed(0) }}</strong>
                <span>{{ item.unit }} / 份量足</span>
              </div>
              <div class="stepper">
                <button type="button" v-if="getQty(item.id) > 0" @click="cart.removeItem(item.id)">-</button>
                <strong v-if="getQty(item.id) > 0">{{ getQty(item.id) }}</strong>
                <button type="button" class="plus" @click="cart.addItem(item)">+</button>
              </div>
            </div>
          </div>
        </article>
      </section>

      <!-- 购物车栏 -->
      <aside class="cart" v-if="cart.totalCount > 0">
        <div class="cart-top">
          <div>
            <strong>已选烧烤</strong><br>
            <small>{{ cartSummary }}</small>
          </div>
          <div class="cart-count">{{ cart.totalCount }} 串</div>
        </div>
        <div class="cart-bottom">
          <div class="total">
            <strong>¥{{ cart.totalPrice.toFixed(0) }}</strong>
            <span>配送费另计</span>
          </div>
          <button class="checkout" @click="router.push('/cart')">提交订单</button>
        </div>
      </aside>
    </div>

    <!-- ===== 点单记录 Tab ===== -->
    <div v-show="activeTab === 1" class="tab-content">
      <div v-if="orders.length === 0" class="empty-history">
        <van-empty description="暂无点单记录" />
      </div>
      <div v-else class="order-list">
        <div class="order-card" v-for="order in orders" :key="order.id">
          <div class="order-header">
            <span class="order-time">{{ order.time }}</span>
            <span class="order-table">桌号: {{ order.tableNo }}</span>
            <span class="order-total">¥{{ order.total.toFixed(2) }}</span>
          </div>
          <div class="order-items">
            <div class="order-item" v-for="item in order.items" :key="item.productId">
              <img class="oi-img" :src="getFoodImage(item.productName)" />
              <span class="oi-name">{{ item.productName }}</span>
              <span class="oi-qty">x{{ item.quantity }}</span>
              <span class="oi-price">¥{{ (item.price * item.quantity).toFixed(2) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部 Tab 栏 -->
    <van-tabbar v-model="activeTab" active-color="#c94f2d" :fixed="true" placeholder>
      <van-tabbar-item icon="wap-home">菜单</van-tabbar-item>
      <van-tabbar-item icon="orders-o">点单记录</van-tabbar-item>
    </van-tabbar>

    <!-- 右上角操作 -->
    <van-action-sheet
      v-model:show="showActions"
      :actions="actions"
      cancel-text="取消"
      @select="onAction"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCategories, getProducts } from '../api'
import { useCartStore } from '../stores/cart'

const router = useRouter()
const cart = useCartStore()

const activeTab = ref(0)
const activeCategory = ref(null)
const categories = ref([])
const products = ref([])
const orders = ref([])
const showActions = ref(false)

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

function getEmoji(name) { return emojiMap[name] || '🔥' }
function getFoodImage(name) { return imageMap[name] || imageMap['羊肉串'] }

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
  try {
    orders.value = JSON.parse(localStorage.getItem('shaokao_orders') || '[]')
  } catch { orders.value = [] }
}

const actions = [
  { name: '修改密码', value: 'password' },
  { name: '退出登录', value: 'logout', color: '#ee0a24' },
]
function onAction(action) {
  showActions.value = false
  if (action.value === 'logout') {
    localStorage.removeItem('token')
    localStorage.removeItem('role')
    router.replace('/login')
  }
}

onMounted(async () => {
  const [catRes, prodRes] = await Promise.all([getCategories(), getProducts()])
  categories.value = catRes.data
  products.value = prodRes.data.list
  if (categories.value.length > 0) activeCategory.value = categories.value[0].id
  loadOrders()
})
</script>

<style scoped>
.user-home {
  min-height: 100vh;
  background: var(--bg);
  padding-bottom: 60px;
}

.tab-content {
  width: min(100%, 430px);
  margin: 0 auto;
  padding: 12px 16px 20px;
}

/* Chips */
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
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.2s, background 0.2s;
  backdrop-filter: blur(8px);
}
.chip.active {
  background: linear-gradient(135deg, var(--accent), #de7743);
  color: #fffaf3;
  transform: translateY(-1px);
  box-shadow: 0 10px 18px rgba(201, 79, 45, 0.22);
}

/* Menu cards */
.menu-list { display: grid; gap: 14px; margin-top: 14px; }

.card {
  display: grid;
  grid-template-columns: 88px 1fr;
  gap: 14px;
  padding: 14px;
  border-radius: var(--radius-lg);
  background: var(--surface);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--line);
  animation: rise-in 0.5s ease both;
}
.thumb {
  display: grid;
  place-items: center;
  border-radius: var(--radius-md);
  background: rgba(255, 245, 230, 0.8);
  font-size: 40px;
  box-shadow: inset 0 -4px 12px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}
.thumb-img { width: 100%; height: 100%; object-fit: cover; }
.card h3 { margin: 2px 0 6px; font-size: 17px; }
.desc { margin: 0; color: var(--muted); font-size: 12px; line-height: 1.6; }
.tags { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 10px; }
.tag {
  padding: 5px 8px; border-radius: 999px;
  background: var(--accent-soft); color: var(--accent);
  font-size: 11px; font-weight: 700;
}
.card-bottom {
  display: flex; justify-content: space-between; align-items: end;
  margin-top: 14px; gap: 12px;
}
.price { display: flex; flex-direction: column; gap: 4px; }
.price strong { font-size: 22px; line-height: 1; color: var(--accent-deep); }
.price span { font-size: 11px; color: var(--muted); }

.stepper {
  display: inline-flex; align-items: center; gap: 10px;
  padding: 6px; border-radius: 999px; background: #fff;
  box-shadow: inset 0 0 0 1px rgba(125, 73, 39, 0.08);
}
.stepper button {
  width: 32px; height: 32px; border: 0; border-radius: 50%;
  font-size: 18px; background: #f4e2d3; color: var(--accent-deep);
  cursor: pointer; display: flex; align-items: center; justify-content: center;
}
.stepper button.plus {
  background: linear-gradient(135deg, var(--accent), #e97b46);
  color: #fff; box-shadow: 0 6px 14px rgba(201, 79, 45, 0.25);
}
.stepper button:active { transform: scale(0.92); }
.stepper strong { width: 22px; text-align: center; font-size: 16px; }

/* Cart bar */
.cart {
  position: fixed; left: 50%; bottom: 60px;
  transform: translateX(-50%);
  width: min(calc(100% - 20px), 410px);
  padding: 14px; border-radius: 24px;
  background: rgba(255, 255, 255, 0.92); color: var(--text);
  box-shadow: 0 24px 48px rgba(160, 100, 40, 0.12);
  backdrop-filter: blur(16px); z-index: 5;
  border: 1px solid var(--line);
  animation: float-up 0.9s ease-out both;
}
.cart-top, .cart-bottom {
  display: flex; justify-content: space-between; align-items: center; gap: 12px;
}
.cart-top small { color: var(--muted); font-size: 12px; }
.cart-count {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 28px; height: 28px; padding: 0 10px;
  border-radius: 999px; background: var(--accent-soft); color: var(--accent); font-weight: 700;
}
.cart-bottom { margin-top: 12px; }
.total strong { display: block; font-size: 24px; }
.total span { color: var(--muted); font-size: 12px; }
.checkout {
  border: 0; padding: 14px 20px; border-radius: 16px;
  background: linear-gradient(135deg, #f0b04f, #ff8848);
  color: #35180c; font-size: 15px; font-weight: 800;
  cursor: pointer; box-shadow: 0 12px 20px rgba(255, 166, 84, 0.28);
}
.checkout:active { transform: scale(0.95); }

/* Order history */
.order-list { display: grid; gap: 12px; }
.order-card {
  background: var(--surface); border-radius: var(--radius-lg);
  overflow: hidden; box-shadow: var(--shadow-sm);
  border: 1px solid var(--line);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
}
.order-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 16px; border-bottom: 1px solid var(--line);
  font-size: 13px; color: var(--muted);
}
.order-total {
  font-size: 18px; font-weight: 800; color: var(--accent-deep);
}
.order-items { padding: 8px 16px; }
.order-item {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 0; font-size: 14px;
}
.oi-img { width: 28px; height: 28px; border-radius: 6px; object-fit: cover; }
.oi-name { flex: 1; font-weight: 600; }
.oi-qty { color: var(--muted); font-size: 13px; }
.oi-price { color: var(--accent-deep); font-weight: 700; min-width: 60px; text-align: right; }

@keyframes rise-in {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}
@keyframes float-up {
  from { opacity: 0; transform: translate(-50%, 18px); }
  to { opacity: 1; transform: translate(-50%, 0); }
}
</style>
