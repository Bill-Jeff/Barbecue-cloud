import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import { addPv } from '../api'

const CART_KEY = 'bbq_cart'

function loadCart() {
  try {
    const raw = localStorage.getItem(CART_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

export const useCartStore = defineStore('cart', () => {
  const items = ref(loadCart())

  const totalCount = computed(() =>
    items.value.reduce((sum, item) => sum + item.quantity, 0)
  )

  const totalPrice = computed(() =>
    items.value.reduce((sum, item) => sum + item.price * item.quantity, 0)
  )

  watch(items, val => {
    localStorage.setItem(CART_KEY, JSON.stringify(val))
  }, { deep: true })

  function addItem(product) {
    const existing = items.value.find(i => i.productId === product.id)
    if (existing) {
      existing.quantity++
      if (product.price < existing.price) {
        existing.price = product.price
      }
    } else {
      items.value.push({
        productId: product.id,
        name: product.name,
        price: product.price,
        quantity: 1,
      })
    }
    addPv(product.id).catch(() => {})
  }

  function removeItem(productId) {
    const existing = items.value.find(i => i.productId === productId)
    if (existing) {
      existing.quantity--
      if (existing.quantity <= 0) {
        items.value = items.value.filter(i => i.productId !== productId)
      }
    }
  }

  function clear() {
    items.value = []
  }

  return { items, totalCount, totalPrice, addItem, removeItem, clear }
})
