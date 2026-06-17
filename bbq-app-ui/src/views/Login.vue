<template>
  <div class="login-page">
    <div class="ember-bg">
      <div class="ember-orb ember-orb--1"></div>
      <div class="ember-orb ember-orb--2"></div>
      <div class="ember-orb ember-orb--3"></div>
    </div>

    <div class="login-card">
      <div class="login-header">
        <div class="login-icon">🔥</div>
        <h2>账号登录</h2>
        <p>请输入您的账号密码</p>
      </div>

      <div class="login-form">
        <div class="input-group">
          <label>账号</label>
          <input
            v-model="username"
            type="text"
            placeholder="请输入账号"
            @keyup.enter="handleLogin"
          />
        </div>
        <div class="input-group">
          <label>密码</label>
          <input
            v-model="password"
            type="password"
            placeholder="请输入密码"
            @keyup.enter="handleLogin"
          />
        </div>

        <div class="error-msg" v-if="errorMsg">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
          {{ errorMsg }}
        </div>

        <button
          class="login-btn"
          :class="{ loading: logging }"
          :disabled="logging"
          @click="handleLogin"
        >
          {{ logging ? '登录中...' : '登 录' }}
        </button>
      </div>

      <a href="/" class="back-link">← 返回前台</a>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { adminLogin } from '../api'

const router = useRouter()
const username = ref('')
const password = ref('')
const errorMsg = ref('')
const logging = ref(false)

async function handleLogin() {
  if (!username.value || !password.value) {
    errorMsg.value = '请输入账号和密码'
    return
  }
  errorMsg.value = ''
  logging.value = true
  try {
    const res = await adminLogin(username.value, password.value)
    const d = res.data
    localStorage.setItem('token', d.accessToken)
    localStorage.setItem('refreshToken', d.refreshToken)
    localStorage.setItem('role', d.role || 'user')
    localStorage.setItem('nickname', d.nickname || '用户')
    const redirect = router.currentRoute.value.query.redirect || '/'
    location.replace(redirect)
  } catch {
    errorMsg.value = '登录失败，账号或密码错误'
  } finally {
    logging.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  position: relative;
  overflow: hidden;
}

/* Floating ember orbs */
.ember-bg {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}
.ember-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.4;
}
.ember-orb--1 {
  width: 200px;
  height: 200px;
  background: rgba(232, 98, 44, 0.12);
  top: 10%;
  right: -30%;
  animation: float-1 8s ease-in-out infinite;
}
.ember-orb--2 {
  width: 150px;
  height: 150px;
  background: rgba(240, 168, 48, 0.1);
  bottom: 20%;
  left: -20%;
  animation: float-2 10s ease-in-out infinite;
}
.ember-orb--3 {
  width: 100px;
  height: 100px;
  background: rgba(255, 78, 32, 0.08);
  top: 60%;
  right: 10%;
  animation: float-1 12s ease-in-out infinite reverse;
}

@keyframes float-1 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(-20px, 30px); }
}
@keyframes float-2 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(30px, -20px); }
}

.login-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 380px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(24px);
  border-radius: 28px;
  padding: 40px 28px;
  border: 1px solid var(--line);
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.06);
  text-align: center;
  animation: rise-in 0.6s ease-out both;
}

.login-header {
  margin-bottom: 32px;
}
.login-icon {
  font-size: 48px;
  margin-bottom: 16px;
  filter: drop-shadow(0 4px 12px rgba(232, 98, 44, 0.4));
}
.login-header h2 {
  margin: 0 0 6px;
  font-size: 24px;
  font-weight: 900;
  color: var(--text-bright);
}
.login-header p {
  margin: 0;
  font-size: 13px;
  color: var(--muted-soft);
}

.login-form {
  text-align: left;
}
.input-group {
  margin-bottom: 20px;
}
.input-group label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--muted);
  margin-bottom: 8px;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}
.input-group input {
  width: 100%;
  padding: 14px 16px;
  border: 1px solid var(--line-bright);
  border-radius: 14px;
  font-size: 15px;
  background: #faf8f6;
  color: var(--text);
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
}
.input-group input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px rgba(232, 98, 44, 0.1);
  background: #fff;
}
.input-group input::placeholder {
  color: var(--muted-soft);
}

.error-msg {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  margin-bottom: 18px;
  background: rgba(255, 78, 32, 0.06);
  border: 1px solid rgba(255, 78, 32, 0.12);
  border-radius: 12px;
  color: var(--ember);
  font-size: 13px;
}

.login-btn {
  width: 100%;
  padding: 15px;
  border: none;
  border-radius: 16px;
  background: linear-gradient(135deg, var(--accent), var(--accent-bright));
  color: #fff;
  font-size: 16px;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 12px 32px rgba(232, 98, 44, 0.3);
  transition: transform 0.15s, opacity 0.15s;
  letter-spacing: 0.1em;
}
.login-btn:active { transform: scale(0.97); }
.login-btn.loading { opacity: 0.6; }

.back-link {
  display: inline-block;
  margin-top: 24px;
  font-size: 13px;
  color: var(--muted-soft);
  text-decoration: none;
  transition: color 0.2s;
}
.back-link:hover { color: var(--text); }

@keyframes rise-in {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
