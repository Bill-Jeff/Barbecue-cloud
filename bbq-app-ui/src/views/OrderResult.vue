<template>
  <div class="result-page">
    <div class="ember-bg">
      <div class="ember-orb ember-orb--1"></div>
      <div class="ember-orb ember-orb--2"></div>
    </div>

    <div class="page">
      <div class="success-anim">
        <div class="success-ring"></div>
        <div class="success-circle">
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
        </div>
      </div>
      <h2 class="result-title">下单成功</h2>
      <p class="result-desc">您的订单已提交<br>后厨正在备餐中</p>
      <div class="result-emoji">
        <img class="result-food-img" src="https://images.unsplash.com/photo-1555939594-58d7cb561ad1?w=120&h=120&fit=crop" style="animation-delay: 0.3s" />
        <img class="result-food-img" src="https://images.unsplash.com/photo-1544025162-d76694265947?w=120&h=120&fit=crop" style="animation-delay: 0.5s" />
        <img class="result-food-img" src="https://images.unsplash.com/photo-1527477396000-e27163b4bbed?w=120&h=120&fit=crop" style="animation-delay: 0.7s" />
      </div>
      <button class="back-btn" @click="router.replace('/')">
        <span>继续点单</span>
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>
      </button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
</script>

<style scoped>
.result-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.ember-bg {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}
.ember-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
}
.ember-orb--1 {
  width: 250px;
  height: 250px;
  background: rgba(76, 175, 80, 0.08);
  top: 20%;
  left: 30%;
  animation: pulse-slow 6s ease-in-out infinite;
}
.ember-orb--2 {
  width: 150px;
  height: 150px;
  background: rgba(232, 98, 44, 0.06);
  bottom: 30%;
  right: 10%;
  animation: pulse-slow 8s ease-in-out infinite reverse;
}
@keyframes pulse-slow {
  0%, 100% { transform: scale(1); opacity: 0.6; }
  50% { transform: scale(1.2); opacity: 1; }
}

.page {
  position: relative;
  z-index: 1;
  width: min(100%, 380px);
  margin: 0 auto;
  padding: 40px 24px;
  text-align: center;
}

.success-anim {
  position: relative;
  width: 100px;
  height: 100px;
  margin: 0 auto 32px;
}
.success-ring {
  position: absolute;
  inset: -8px;
  border-radius: 50%;
  border: 2px solid rgba(76, 175, 80, 0.15);
  animation: ring-expand 1s ease-out both;
}
@keyframes ring-expand {
  from { transform: scale(0.5); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}
.success-circle {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: linear-gradient(135deg, #2e7d32, #66bb6a);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  animation: pop 0.6s cubic-bezier(0.175, 0.885, 0.32, 1.275) 0.1s both;
  box-shadow:
    0 16px 40px rgba(76, 175, 80, 0.3),
    0 0 0 4px rgba(76, 175, 80, 0.1);
  color: #fff;
}
@keyframes pop {
  0% { transform: scale(0); opacity: 0; }
  60% { transform: scale(1.1); }
  100% { transform: scale(1); opacity: 1; }
}

.result-title {
  font-size: 28px;
  font-weight: 900;
  color: var(--text-bright);
  margin: 0 0 10px;
  letter-spacing: 0.05em;
}
.result-desc {
  font-size: 14px;
  color: var(--muted);
  margin: 0 0 28px;
  line-height: 1.8;
}
.result-emoji {
  margin-bottom: 40px;
  display: flex;
  justify-content: center;
  gap: 12px;
}
.result-food-img {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  object-fit: cover;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  animation: bounce-in 0.5s ease-out both;
}
@keyframes bounce-in {
  0% { transform: translateY(20px); opacity: 0; }
  60% { transform: translateY(-4px); }
  100% { transform: translateY(0); opacity: 1; }
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: linear-gradient(135deg, var(--accent), var(--accent-bright));
  color: #fff;
  border: none;
  padding: 14px 36px;
  border-radius: 999px;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 12px 32px rgba(232, 98, 44, 0.3);
  transition: transform 0.15s;
}
.back-btn:active { transform: scale(0.95); }
</style>
