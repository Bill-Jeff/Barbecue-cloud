<template>
  <div class="cart-page">
    <div class="page">
      <div class="cart-header">
        <button class="back-btn" @click="router.back()">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="15 18 9 12 15 6"/></svg>
        </button>
        <h2>购物车</h2>
        <span class="item-count">{{ cart.totalCount }} 件</span>
      </div>

      <div v-if="cart.items.length === 0" class="empty-cart">
        <div class="empty-icon">
          <svg width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" opacity="0.25"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/></svg>
        </div>
        <p>购物车空空如也</p>
        <button class="go-shop-btn" @click="router.replace('/')">去逛逛</button>
      </div>

      <div v-else>
        <div class="cart-list">
          <div class="cart-item" v-for="item in cart.items" :key="item.productId">
            <div class="cart-item__thumb"><img :src="getFoodImage(item.name)" class="cart-thumb-img" /></div>
            <div class="cart-item__info">
              <div class="cart-item__name">{{ item.name }}</div>
              <div class="cart-item__price">¥{{ item.price.toFixed(0) }}</div>
            </div>
            <div class="stepper">
              <button type="button" class="stepper-btn" @click="cart.removeItem(item.productId)">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><line x1="5" y1="12" x2="19" y2="12"/></svg>
              </button>
              <strong>{{ item.quantity }}</strong>
              <button type="button" class="stepper-btn stepper-btn--plus" @click="cart.addItem({ id: item.productId, name: item.name, price: item.price })">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
              </button>
            </div>
            <button class="del-btn" @click="removeAll(item.productId)">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          </div>
        </div>

        <div class="table-input">
          <div class="table-label">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="9" y1="21" x2="9" y2="9"/></svg>
            桌号
          </div>
          <input v-model="tableNo" type="number" placeholder="请输入桌号" class="table-field" />
        </div>

        <div class="order-summary">
          <div class="summary-row">
            <span class="summary-count">共 {{ cart.totalCount }} 件</span>
            <span class="summary-total">¥<em>{{ cart.totalPrice.toFixed(0) }}</em></span>
          </div>
          <button class="submit-btn" :class="{ loading: submitting }" @click="submitOrder">
            {{ submitting ? '下单中...' : '确认下单' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../stores/cart'
import { createOrder } from '../api'

const router = useRouter()
const cart = useCartStore()
const tableNo = ref('')
const submitting = ref(false)

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

function getEmoji(name) { return emojiMap[name] || '🔥' }
function getFoodImage(name) { return imageMap[name] || imageMap['羊肉串'] }
function removeAll(productId) { cart.items = cart.items.filter(i => i.productId !== productId) }

async function submitOrder() {
  if (!tableNo.value) { alert('请输入桌号'); return }
  submitting.value = true
  try {
    const orderData = {
      tableNo: tableNo.value,
      items: cart.items.map(i => ({
        productId: i.productId, productName: i.name, price: i.price, quantity: i.quantity,
      })),
    }
    const res = await createOrder(orderData)
    const localOrder = {
      id: res.data.id || Date.now(),
      tableNo: orderData.tableNo,
      items: orderData.items,
      total: cart.totalPrice,
      time: new Date().toLocaleString('zh-CN'),
    }
    const orders = JSON.parse(localStorage.getItem('shaokao_orders') || '[]')
    orders.unshift(localOrder)
    localStorage.setItem('shaokao_orders', JSON.stringify(orders.slice(0, 50)))
    cart.clear()
    router.replace('/order-result')
  } catch {
    alert('下单失败，请重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.cart-page {
  min-height: 100vh;
  background: var(--bg);
}

.page {
  position: relative;
  z-index: 1;
  width: min(100%, 430px);
  margin: 0 auto;
  padding: 0 16px 160px;
}

.cart-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 44px 0 24px;
}
.cart-header h2 {
  margin: 0;
  font-size: 20px;
  flex: 1;
  font-weight: 900;
  color: var(--text-bright);
}
.back-btn {
  background: var(--surface);
  border: 1px solid var(--line);
  width: 38px;
  height: 38px;
  border-radius: 50%;
  cursor: pointer;
  color: var(--text);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}
.back-btn:active { transform: scale(0.9); background: var(--surface-raised); }
.item-count {
  font-size: 13px;
  color: var(--muted-soft);
}

/* Empty */
.empty-cart {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 0;
}
.empty-icon { margin-bottom: 20px; color: var(--muted-soft); }
.empty-cart p { color: var(--muted-soft); font-size: 14px; }
.go-shop-btn {
  margin-top: 24px;
  background: linear-gradient(135deg, var(--accent), var(--accent-bright));
  color: #fff;
  border: none;
  padding: 12px 40px;
  border-radius: 999px;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 10px 28px rgba(232, 98, 44, 0.3);
  transition: transform 0.15s;
}
.go-shop-btn:active { transform: scale(0.95); }

/* Cart list */
.cart-list {
  background: var(--surface);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-radius: var(--radius-lg);
  overflow: hidden;
  border: 1px solid var(--line);
  box-shadow: var(--shadow-sm);
}
.cart-item {
  display: flex;
  align-items: center;
  padding: 14px;
  border-bottom: 1px solid var(--line);
  gap: 12px;
}
.cart-item:last-child { border-bottom: none; }

.cart-item__thumb {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-sm);
  background: rgba(255, 245, 230, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}
.cart-thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: var(--radius-sm);
}
.cart-item__info { flex: 1; }
.cart-item__name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-bright);
}
.cart-item__price {
  font-size: 16px;
  color: var(--accent-bright);
  font-weight: 800;
  margin-top: 4px;
}

.stepper {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.stepper-btn {
  width: 26px;
  height: 26px;
  border: 0;
  border-radius: 50%;
  background: var(--surface-glass);
  color: var(--muted);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--line-bright);
  transition: all 0.15s;
}
.stepper-btn--plus {
  background: linear-gradient(135deg, var(--accent), var(--accent-bright));
  color: #fff;
  border: none;
  box-shadow: 0 4px 12px rgba(232, 98, 44, 0.3);
}
.stepper-btn:active { transform: scale(0.85); }
.stepper strong {
  width: 18px;
  text-align: center;
  font-size: 14px;
  color: var(--text-bright);
}

.del-btn {
  background: none;
  border: none;
  color: var(--muted-soft);
  cursor: pointer;
  padding: 4px;
  transition: color 0.15s;
}
.del-btn:active { color: var(--ember); }

/* Table input */
.table-input {
  background: var(--surface);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-radius: var(--radius-lg);
  margin-top: 14px;
  padding: 16px 18px;
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px solid var(--line);
}
.table-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  white-space: nowrap;
}
.table-field {
  flex: 1;
  border: none;
  outline: none;
  font-size: 15px;
  text-align: right;
  background: transparent;
  color: var(--text-bright);
}
.table-field::placeholder { color: var(--muted-soft); }

/* Order summary */
.order-summary {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: min(100%, 430px);
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  padding: 16px 18px;
  padding-bottom: max(16px, env(safe-area-inset-bottom));
  border-top: 1px solid var(--line);
  box-shadow: 0 -8px 30px rgba(160, 100, 40, 0.08);
}
.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}
.summary-count {
  font-size: 13px;
  color: var(--muted);
}
.summary-total {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
}
.summary-total em {
  font-style: normal;
  font-size: 28px;
  font-weight: 900;
  color: var(--accent);
  margin-left: 2px;
}
.submit-btn {
  width: 100%;
  height: 50px;
  background: linear-gradient(135deg, var(--accent), var(--gold));
  color: #fff;
  border: none;
  border-radius: 16px;
  font-size: 16px;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 12px 32px rgba(232, 98, 44, 0.3);
  transition: transform 0.15s;
  letter-spacing: 0.05em;
}
.submit-btn:active { transform: scale(0.97); }
.submit-btn.loading { opacity: 0.7; }
</style>
