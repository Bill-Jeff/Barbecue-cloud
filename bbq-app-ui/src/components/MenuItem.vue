<template>
  <div class="menu-card">
    <div class="menu-card__img" :style="{ background: bgGradient }">
      <span class="menu-card__emoji">{{ emoji }}</span>
    </div>
    <div class="menu-card__info">
      <div class="menu-card__name">{{ product.name }}</div>
      <div class="menu-card__desc">{{ product.description }}</div>
      <div class="menu-card__bottom">
        <span class="menu-card__price">¥{{ product.price.toFixed(2) }}</span>
        <div class="menu-card__stepper">
          <button v-if="quantity > 0" class="step-btn minus" @click="cart.removeItem(product.id)">−</button>
          <span v-if="quantity > 0" class="step-count">{{ quantity }}</span>
          <button class="step-btn plus" @click="cart.addItem(product)">+</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useCartStore } from '../stores/cart'

const props = defineProps({ product: Object })
const cart = useCartStore()

const quantity = computed(() => {
  const item = cart.items.find(i => i.productId === props.product.id)
  return item ? item.quantity : 0
})

const emojiMap = {
  '羊肉串': '🍢', '牛肉串': '🥩', '鸡翅': '🍗', '五花肉': '🥓',
  '烤腰子': '🫘', '烤生蚝': '🦪', '烤鱿鱼': '🦑', '烤大虾': '🦐',
  '烤馒头片': '🍞', '烤韭菜': '🥬', '烤茄子': '🍆',
  '啤酒': '🍺', '王老吉': '🧃', '酸梅汤': '🥤',
}

const gradients = [
  'linear-gradient(135deg, #ff9a56 0%, #ff6b35 100%)',
  'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
  'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
  'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
  'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
  'linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%)',
]

const emoji = computed(() => emojiMap[props.product.name] || '🔥')
const bgGradient = computed(() => gradients[props.product.id % gradients.length])
</script>

<style scoped>
.menu-card {
  display: flex;
  background: #fff;
  border-radius: 12px;
  margin-bottom: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  transition: transform 0.2s;
}

.menu-card:active {
  transform: scale(0.98);
}

.menu-card__img {
  width: 100px;
  min-height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.menu-card__emoji {
  font-size: 40px;
  filter: drop-shadow(0 2px 4px rgba(0,0,0,0.2));
}

.menu-card__info {
  flex: 1;
  padding: 12px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.menu-card__name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.menu-card__desc {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.menu-card__bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.menu-card__price {
  font-size: 18px;
  font-weight: 700;
  color: #ff4444;
}

.menu-card__stepper {
  display: flex;
  align-items: center;
  gap: 8px;
}

.step-btn {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  font-size: 18px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s;
}

.step-btn.plus {
  background: #ff6b35;
  color: #fff;
  box-shadow: 0 2px 8px rgba(255,107,53,0.4);
}

.step-btn.plus:active {
  transform: scale(0.9);
  background: #e55a25;
}

.step-btn.minus {
  background: #f0f0f0;
  color: #666;
}

.step-btn.minus:active {
  background: #ddd;
}

.step-count {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  min-width: 20px;
  text-align: center;
}
</style>
