<template>
  <div class="flash-sale-page">
    <van-nav-bar title="限时秒杀" left-arrow @click-left="router.back()" placeholder fixed>
      <template #right>
        <span class="nav-tip">手慢无</span>
      </template>
    </van-nav-bar>

    <div class="page-body">
      <div v-if="sales.length === 0" class="empty-state">
        <div class="empty-icon">⏳</div>
        <p>暂无秒杀活动</p>
        <span class="empty-hint">下次早点来哦</span>
      </div>

      <div v-else class="sale-list">
        <div class="sale-card" v-for="sale in sales" :key="sale.id">
          <div class="sale-visual">
            <img v-if="sale.productImage" :src="sale.productImage" class="sale-img" />
            <span v-else class="sale-emoji">🔥</span>
            <div class="sale-badge">秒杀</div>
          </div>
          <div class="sale-body">
            <div class="sale-header">
              <h3>{{ sale.productName }}</h3>
              <div class="price-row">
                <span class="flash-price">¥{{ Number(sale.price).toFixed(0) }}</span>
                <span class="orig-price">¥{{ Number(sale.originalPrice).toFixed(0) }}</span>
              </div>
            </div>
            <div class="sale-meta">
              <span class="meta-stock">剩余 {{ sale.count }} 件</span>
              <van-button
                type="danger"
                size="small"
                round
                :disabled="sale.count <= 0"
                @click="openBuyDialog(sale)"
              >
                {{ sale.count > 0 ? '立即抢购' : '已抢光' }}
              </van-button>
            </div>
            <div class="countdown-row">
              <div class="countdown">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                <span class="countdown-text" :class="{ urgent: countdowns[sale.id]?.urgent }">
                  {{ countdowns[sale.id]?.text || '已结束' }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 下单弹窗 -->
    <van-dialog
      v-model:show="buyDialog.visible"
      title="确认下单"
      show-cancel-button
      :confirm-button-loading="buyDialog.loading"
      @confirm="submitFlashOrder"
    >
      <div class="buy-dialog-body">
        <div class="buy-dialog-info">
          <span class="buy-dialog-name">{{ buyDialog.sale?.productName }}</span>
          <span class="buy-dialog-price">¥{{ Number(buyDialog.sale?.price || 0).toFixed(0) }} / 份</span>
        </div>
        <van-stepper v-model="buyDialog.quantity" :min="1" :max="buyDialog.sale?.count || 1" theme="round" />
        <van-field
          v-model="buyDialog.tableNo"
          label="桌号"
          placeholder="请输入桌号"
          type="number"
          input-align="right"
        />
      </div>
    </van-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getFlashSales, createFlashSaleOrder } from '../api'

const router = useRouter()
const sales = ref([])
const countdowns = ref({})
let timer = null

const buyDialog = reactive({
  visible: false,
  loading: false,
  sale: null,
  quantity: 1,
  tableNo: '',
})

function openBuyDialog(sale) {
  buyDialog.sale = sale
  buyDialog.quantity = 1
  buyDialog.visible = true
}

async function submitFlashOrder() {
  if (!buyDialog.tableNo) {
    showToast('请输入桌号')
    return
  }
  buyDialog.loading = true
  try {
    await createFlashSaleOrder({
      flashSaleId: buyDialog.sale.id,
      tableNo: buyDialog.tableNo,
      quantity: buyDialog.quantity,
    })
    showToast('抢购成功，订单处理中')
    buyDialog.visible = false
    // 刷新列表更新库存
    const res = await getFlashSales()
    sales.value = res.data
  } catch (e) {
    showToast(e.response?.data?.message || e.message || '下单失败')
  } finally {
    buyDialog.loading = false
  }
}

function updateCountdowns() {
  const now = Date.now()
  sales.value.forEach(sale => {
    const end = new Date(sale.saleTime).getTime()
    const diff = end - now
    if (diff <= 0) {
      countdowns.value[sale.id] = { text: '即将售罄', urgent: true }
      return
    }
    const h = Math.floor(diff / 3600000)
    const m = Math.floor((diff % 3600000) / 60000)
    const s = Math.floor((diff % 60000) / 1000)
    const urgent = diff < 3600000
    countdowns.value[sale.id] = {
      text: `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`,
      urgent,
    }
  })
}

onMounted(async () => {
  const res = await getFlashSales()
  sales.value = res.data
  updateCountdowns()
  timer = setInterval(updateCountdowns, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.flash-sale-page {
  min-height: 100vh;
  background: var(--bg);
  padding-bottom: 40px;
}

.nav-tip {
  font-size: 12px;
  color: var(--ember);
  font-weight: 600;
}

.page-body {
  padding: 14px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120px 20px;
}
.empty-icon { font-size: 48px; margin-bottom: 16px; }
.empty-state p { font-size: 15px; font-weight: 600; color: var(--text); margin: 0 0 6px; }
.empty-hint { font-size: 12px; color: var(--muted-soft); }

.sale-list {
  display: grid;
  gap: 12px;
}

.sale-card {
  display: flex;
  gap: 14px;
  padding: 16px;
  border-radius: var(--radius-lg);
  background: var(--surface);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid var(--line);
  box-shadow: var(--shadow-sm);
  animation: rise-in 0.4s ease both;
}

.sale-visual {
  position: relative;
  width: 90px;
  height: 90px;
  border-radius: var(--radius-md);
  overflow: hidden;
  flex-shrink: 0;
  background: linear-gradient(145deg, #f8f0e8, #f0e8de);
  display: flex;
  align-items: center;
  justify-content: center;
}
.sale-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.sale-emoji {
  font-size: 36px;
}
.sale-badge {
  position: absolute;
  top: 0;
  left: 0;
  padding: 3px 10px;
  background: linear-gradient(135deg, #ff4e20, #ff7a3d);
  color: #fff;
  font-size: 10px;
  font-weight: 800;
  border-radius: 0 0 8px 0;
  letter-spacing: 0.05em;
}

.sale-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-width: 0;
}
.sale-header h3 {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 700;
  color: var(--text-bright);
}
.price-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
}
.flash-price {
  font-size: 24px;
  font-weight: 900;
  color: var(--ember);
  line-height: 1;
}
.orig-price {
  font-size: 13px;
  color: var(--muted-soft);
  text-decoration: line-through;
}

.sale-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 4px;
}
.countdown-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 4px;
}
.meta-stock {
  font-size: 12px;
  color: var(--muted);
  padding: 3px 10px;
  border-radius: 999px;
  background: var(--surface-raised);
  border: 1px solid var(--line);
}
.countdown {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--accent-bright);
}
.countdown-text {
  font-size: 15px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}
.countdown-text.urgent {
  color: var(--ember);
  animation: pulse 1s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* 下单弹窗 */
.buy-dialog-body {
  padding: 12px 16px 8px;
}
.buy-dialog-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.buy-dialog-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-bright);
}
.buy-dialog-price {
  font-size: 15px;
  font-weight: 800;
  color: var(--ember);
}
</style>
