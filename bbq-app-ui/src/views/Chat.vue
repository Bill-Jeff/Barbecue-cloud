<template>
  <div class="chat-page">
    <!-- Header -->
    <header class="chat-header">
      <button class="chat-back" @click="router.back()">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="15 18 9 12 15 6"/>
        </svg>
      </button>
      <div class="chat-header-info">
        <div class="chat-avatar">🐨</div>
        <div class="chat-header-text">
          <span class="chat-header-name">小考</span>
          <span class="chat-header-sub">AI 美食助手</span>
        </div>
      </div>
      <button class="chat-clear" @click="clearHistory">清空</button>
    </header>

    <!-- Messages -->
    <div class="chat-body" ref="chatBodyRef">
      <div v-if="messages.length === 0 && !sending" class="chat-welcome">
        <div class="welcome-icon">🐨</div>
        <p class="welcome-title">你好，我是小考！</p>
        <p class="welcome-desc">不知道吃什么？告诉我你的口味偏好，我来帮你推荐~</p>
        <div class="welcome-chips">
          <button
            v-for="q in quickQuestions"
            :key="q"
            class="welcome-chip"
            @click="send(q)"
          >{{ q }}</button>
        </div>
      </div>

      <div class="chat-messages" v-if="messages.length > 0">
        <div
          v-for="(msg, i) in messages"
          :key="i"
          class="msg-row"
          :class="msg.role === 'user' ? 'msg-row--user' : 'msg-row--bot'"
        >
          <div class="msg-bubble" :class="msg.role === 'user' ? 'bubble--user' : 'bubble--bot'">
            <span class="bubble-text">{{ msg.content }}</span>
          </div>
        </div>

        <div v-if="typing" class="msg-row msg-row--bot">
          <div class="msg-bubble bubble--bot bubble--typing">
            <span class="typing-dots"><i></i><i></i><i></i></span>
          </div>
        </div>
      </div>
    </div>

    <!-- Input bar -->
    <div class="chat-input-bar">
      <div class="input-wrap">
        <input
          ref="inputRef"
          v-model="inputText"
          class="chat-input"
          placeholder="输入你想吃的..."
          @keydown.enter="send(inputText)"
        />
        <button
          class="send-btn"
          :class="{ 'send-btn--active': inputText.trim() }"
          :disabled="!inputText.trim() || sending"
          @click="send(inputText)"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/>
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { sendChatMsg, getProducts } from '../api'
import { useCartStore } from '../stores/cart'
import { showToast } from 'vant'

const router = useRouter()
const cart = useCartStore()

const CHAT_KEY = 'bbq_chat_history'

const messages = ref([])
const inputText = ref('')
const sending = ref(false)
const typing = ref(false)
const chatBodyRef = ref(null)
const inputRef = ref(null)
const products = ref([])

const quickQuestions = [
  '今晚有什么推荐的？',
  '想吃辣的，有什么选择？',
  '两人份的烤肉推荐一下',
  '有没有适合宵夜的？',
]

function loadHistory() {
  try {
    const raw = localStorage.getItem(CHAT_KEY)
    if (raw) messages.value = JSON.parse(raw)
  } catch { messages.value = [] }
}

function saveHistory() {
  localStorage.setItem(CHAT_KEY, JSON.stringify(messages.value))
}

function clearHistory() {
  messages.value = []
  localStorage.removeItem(CHAT_KEY)
}

function parseCartTag(content) {
  const match = content.match(/\[CART:([^\]]+)\]/)
  if (!match) return { text: content, cartNames: [] }
  const cartNames = match[1].split(',').map(s => s.trim()).filter(Boolean)
  return {
    text: content.replace(/\[CART:[^\]]+\]\n?/g, '').trim(),
    cartNames,
  }
}

function addToCartByNames(names) {
  if (products.value.length === 0) return 0
  let added = 0
  for (const name of names) {
    const p = products.value.find(
      item => item.name === name || item.name.includes(name) || name.includes(item.name)
    )
    if (p) {
      cart.addItem({ id: p.id, name: p.name, price: p.price })
      added++
    }
  }
  return added
}

async function send(text) {
  const msg = (typeof text === 'string' ? text : inputText.value).trim()
  if (!msg || sending.value) return

  messages.value.push({ role: 'user', content: msg })
  inputText.value = ''
  sending.value = true
  typing.value = true
  saveHistory()
  await scrollToBottom()

  try {
    const res = await sendChatMsg(msg)
    typing.value = false
    const rawContent = res.data || '抱歉，我没能理解您的问题~'
    const { text: displayText, cartNames } = parseCartTag(rawContent)

    messages.value.push({ role: 'bot', content: displayText })
    saveHistory()

    if (cartNames.length > 0) {
      const n = addToCartByNames(cartNames)
      if (n > 0) {
        showToast({ message: `已加入购物车`, icon: 'success', className: 'toast-success', duration: 1500 })
      }
    }
  } catch {
    typing.value = false
    messages.value.push({ role: 'bot', content: '抱歉，网络不太好，请稍后再试~' })
    saveHistory()
  } finally {
    sending.value = false
    await scrollToBottom()
  }
}

async function scrollToBottom() {
  await nextTick()
  if (chatBodyRef.value) {
    chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
  }
}

watch(messages, () => saveHistory(), { deep: true })

onMounted(async () => {
  inputRef.value?.focus()
  loadHistory()
  try {
    const res = await getProducts()
    products.value = res.data?.list || res.data || []
  } catch { /* ignore */ }
})
</script>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--bg);
}

/* ===== Header ===== */
.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--surface);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid var(--line);
  position: sticky;
  top: 0;
  z-index: 10;
}
.chat-back {
  width: 36px;
  height: 36px;
  border: 0;
  border-radius: 50%;
  background: var(--surface-glass);
  color: var(--text);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.15s;
  flex-shrink: 0;
}
.chat-back:active { transform: scale(0.9); }
.chat-header-info {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}
.chat-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ffecd2, #fcb69f);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}
.chat-header-text {
  display: flex;
  flex-direction: column;
}
.chat-header-name {
  font-size: 17px;
  font-weight: 700;
  color: var(--text-bright);
}
.chat-header-sub {
  font-size: 11px;
  color: var(--muted-soft);
}
.chat-clear {
  border: 0;
  background: none;
  color: var(--muted-soft);
  font-size: 12px;
  cursor: pointer;
  padding: 4px 8px;
}
.chat-clear:active { color: var(--ember); }

/* ===== Body ===== */
.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.chat-welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 60px;
}
.welcome-icon {
  font-size: 64px;
  margin-bottom: 16px;
}
.welcome-title {
  font-size: 20px;
  font-weight: 800;
  color: var(--text-bright);
  margin: 0 0 8px;
}
.welcome-desc {
  font-size: 13px;
  color: var(--muted);
  margin: 0 0 24px;
}
.welcome-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
}
.welcome-chip {
  padding: 10px 18px;
  border-radius: 999px;
  border: 1px solid var(--line-bright);
  background: var(--surface);
  color: var(--text);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.welcome-chip:active {
  background: var(--accent-soft);
  border-color: var(--accent-bright);
  color: var(--accent);
}

/* Messages */
.chat-messages {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.msg-row {
  display: flex;
}
.msg-row--user {
  justify-content: flex-end;
}
.msg-row--bot {
  justify-content: flex-start;
}

.msg-bubble {
  max-width: 82%;
  padding: 12px 16px;
  border-radius: 18px;
  line-height: 1.6;
  word-break: break-word;
}
.bubble--user {
  background: linear-gradient(135deg, var(--accent), var(--accent-bright));
  color: #fff;
  border-bottom-right-radius: 6px;
}
.bubble--bot {
  background: var(--surface);
  color: var(--text);
  border: 1px solid var(--line);
  border-bottom-left-radius: 6px;
}
.bubble-text {
  font-size: 14px;
  white-space: pre-wrap;
}

/* Typing */
.bubble--typing {
  padding: 14px 20px;
}
.typing-dots {
  display: flex;
  gap: 5px;
  align-items: center;
}
.typing-dots i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--muted-soft);
  animation: typing-bounce 1.2s ease-in-out infinite;
}
.typing-dots i:nth-child(1) { animation-delay: 0s; }
.typing-dots i:nth-child(2) { animation-delay: 0.15s; }
.typing-dots i:nth-child(3) { animation-delay: 0.3s; }

@keyframes typing-bounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.3; }
  30% { transform: translateY(-6px); opacity: 1; }
}

/* ===== Input bar ===== */
.chat-input-bar {
  padding: 10px 14px 18px;
  background: var(--surface);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-top: 1px solid var(--line);
}
.input-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--bg);
  border-radius: 24px;
  padding: 6px 6px 6px 18px;
  border: 1px solid var(--line);
  transition: border-color 0.2s;
}
.input-wrap:focus-within {
  border-color: var(--accent-bright);
}
.chat-input {
  flex: 1;
  border: 0;
  background: transparent;
  font-size: 14px;
  outline: none;
  color: var(--text);
  padding: 6px 0;
}
.chat-input::placeholder {
  color: var(--muted-soft);
}
.send-btn {
  width: 38px;
  height: 38px;
  border: 0;
  border-radius: 50%;
  background: var(--line-bright);
  color: var(--muted-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: default;
  transition: all 0.2s;
  flex-shrink: 0;
}
.send-btn--active {
  background: linear-gradient(135deg, var(--accent), var(--accent-bright));
  color: #fff;
  box-shadow: 0 4px 12px rgba(232, 98, 44, 0.35);
  cursor: pointer;
}
.send-btn--active:active {
  transform: scale(0.9);
}
</style>
