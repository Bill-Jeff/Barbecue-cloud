<template>
  <div class="admin">
    <header class="admin-header">
      <div class="admin-header__inner">
        <h1>后台管理</h1>
        <a href="/" class="back-link">← 返回前台</a>
      </div>
      <nav class="admin-tabs">
        <button :class="{ active: tab === 'products' }" @click="tab = 'products'">菜品管理</button>
        <button :class="{ active: tab === 'categories' }" @click="tab = 'categories'">分类管理</button>
      </nav>
    </header>

    <!-- ========== 菜品管理 ========== -->
    <main v-show="tab === 'products'" class="admin-main">
      <div class="toolbar">
        <h2>菜品列表</h2>
        <button class="btn-primary" @click="openProductForm()">+ 新增菜品</button>
      </div>

      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>图片</th>
              <th>名称</th>
              <th>分类</th>
              <th>价格</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in products" :key="p.id">
              <td>
                <div class="thumb-cell">
                  <img v-if="p.image" :src="p.image" class="thumb-img" />
                  <span v-else class="thumb-placeholder">{{ getEmoji(p.name) }}</span>
                </div>
              </td>
              <td>
                <div class="name-cell">
                  <strong>{{ p.name }}</strong>
                  <small>{{ p.description }}</small>
                </div>
              </td>
              <td>{{ getCategoryName(p.categoryId) }}</td>
              <td class="price-cell">¥{{ Number(p.price).toFixed(2) }}</td>
              <td>
                <span class="status-badge" :class="p.status === 1 ? 'on' : 'off'">
                  {{ p.status === 1 ? '上架' : '下架' }}
                </span>
              </td>
              <td>
                <div class="action-btns">
                  <button class="btn-sm" @click="openProductForm(p)">编辑</button>
                  <button class="btn-sm toggle" @click="toggleStatus(p)">
                    {{ p.status === 1 ? '下架' : '上架' }}
                  </button>
                  <button class="btn-sm danger" @click="confirmDeleteProduct(p)">删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </main>

    <!-- ========== 分类管理 ========== -->
    <main v-show="tab === 'categories'" class="admin-main">
      <div class="toolbar">
        <h2>分类列表</h2>
        <button class="btn-primary" @click="openCategoryForm()">+ 新增分类</button>
      </div>

      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>分类名称</th>
              <th>排序</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in categories" :key="c.id">
              <td>{{ c.id }}</td>
              <td><strong>{{ c.name }}</strong></td>
              <td>{{ c.sortOrder }}</td>
              <td>
                <div class="action-btns">
                  <button class="btn-sm" @click="openCategoryForm(c)">编辑</button>
                  <button class="btn-sm danger" @click="confirmDeleteCategory(c)">删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </main>

    <!-- ========== 菜品编辑弹窗 ========== -->
    <div class="modal-mask" v-if="showProductModal" @click.self="showProductModal = false">
      <div class="modal">
        <h3>{{ productForm.id ? '编辑菜品' : '新增菜品' }}</h3>
        <div class="form-grid">
          <label>
            <span>菜品名称</span>
            <input v-model="productForm.name" placeholder="如：招牌羊肉串" />
          </label>
          <label>
            <span>所属分类</span>
            <select v-model="productForm.categoryId">
              <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
          </label>
          <label>
            <span>单价 (元)</span>
            <input v-model.number="productForm.price" type="number" step="0.01" />
          </label>
          <label>
            <span>描述</span>
            <input v-model="productForm.description" placeholder="简短描述" />
          </label>
          <label>
            <span>排序</span>
            <input v-model.number="productForm.sortOrder" type="number" />
          </label>
          <label class="full">
            <span>菜品图片</span>
            <div class="upload-area">
              <img v-if="productForm.image" :src="productForm.image" class="preview-img" />
              <div v-else class="upload-placeholder">点击上传图片</div>
              <input type="file" accept="image/*" @change="handleUpload" class="file-input" />
            </div>
          </label>
        </div>
        <div class="modal-actions">
          <button class="btn-cancel" @click="showProductModal = false">取消</button>
          <button class="btn-primary" @click="saveProduct" :disabled="saving">
            {{ saving ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>

    <!-- ========== 分类编辑弹窗 ========== -->
    <div class="modal-mask" v-if="showCategoryModal" @click.self="showCategoryModal = false">
      <div class="modal modal-sm">
        <h3>{{ categoryForm.id ? '编辑分类' : '新增分类' }}</h3>
        <div class="form-grid">
          <label>
            <span>分类名称</span>
            <input v-model="categoryForm.name" placeholder="如：烤串" />
          </label>
          <label>
            <span>排序</span>
            <input v-model.number="categoryForm.sortOrder" type="number" />
          </label>
        </div>
        <div class="modal-actions">
          <button class="btn-cancel" @click="showCategoryModal = false">取消</button>
          <button class="btn-primary" @click="saveCategory">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import {
  adminListProducts, adminSaveProduct, adminDeleteProduct,
  adminListCategories, adminSaveCategory, adminDeleteCategory,
  adminUpload,
} from '../api'

const tab = ref('products')
const products = ref([])
const categories = ref([])
const saving = ref(false)

const emojiMap = {
  '羊肉串': '🍢', '牛肉串': '🥩', '鸡翅': '🍗', '五花肉': '🥓',
  '烤腰子': '🫘', '烤生蚝': '🦪', '烤鱿鱼': '🦑', '烤大虾': '🦐',
  '烤馒头片': '🍞', '烤韭菜': '🥬', '烤茄子': '🍆',
  '啤酒': '🍺', '王老吉': '🧃', '酸梅汤': '🥤',
}

function getEmoji(name) { return emojiMap[name] || '🔥' }
function getCategoryName(id) {
  const c = categories.value.find(c => c.id === id)
  return c ? c.name : '-'
}

async function loadAll() {
  const [pRes, cRes] = await Promise.all([adminListProducts(), adminListCategories()])
  products.value = pRes.data
  categories.value = cRes.data
}

onMounted(loadAll)

// ========== 菜品 CRUD ==========
const showProductModal = ref(false)
const productForm = reactive({
  id: null, name: '', categoryId: null, price: 0, image: '', description: '', sortOrder: 0, status: 1,
})

function openProductForm(p) {
  if (p) {
    Object.assign(productForm, { ...p })
  } else {
    Object.assign(productForm, {
      id: null, name: '', categoryId: categories.value[0]?.id, price: 0,
      image: '', description: '', sortOrder: 0, status: 1,
    })
  }
  showProductModal.value = true
}

async function saveProduct() {
  saving.value = true
  try {
    await adminSaveProduct({ ...productForm })
    showProductModal.value = false
    await loadAll()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(p) {
  await adminSaveProduct({ ...p, status: p.status === 1 ? 0 : 1 })
  await loadAll()
}

async function confirmDeleteProduct(p) {
  if (confirm(`确定删除「${p.name}」吗？`)) {
    await adminDeleteProduct(p.id)
    await loadAll()
  }
}

async function handleUpload(e) {
  const file = e.target.files[0]
  if (!file) return
  const res = await adminUpload(file)
  productForm.image = res.data.url
}

// ========== 分类 CRUD ==========
const showCategoryModal = ref(false)
const categoryForm = reactive({ id: null, name: '', sortOrder: 0 })

function openCategoryForm(c) {
  if (c) {
    Object.assign(categoryForm, { ...c })
  } else {
    Object.assign(categoryForm, { id: null, name: '', sortOrder: 0 })
  }
  showCategoryModal.value = true
}

async function saveCategory() {
  await adminSaveCategory({ ...categoryForm })
  showCategoryModal.value = false
  await loadAll()
}

async function confirmDeleteCategory(c) {
  if (confirm(`确定删除分类「${c.name}」吗？`)) {
    await adminDeleteCategory(c.id)
    await loadAll()
  }
}
</script>

<style scoped>
.admin {
  min-height: 100vh;
  background: #f0f2f5;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  color: #333;
}

/* Header */
.admin-header {
  background: #fff;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
  position: sticky;
  top: 0;
  z-index: 50;
}

.admin-header__inner {
  max-width: 1100px;
  margin: 0 auto;
  padding: 16px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.admin-header__inner h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
}

.back-link {
  color: #666;
  text-decoration: none;
  font-size: 14px;
}

.back-link:hover { color: #c94f2d; }

.admin-tabs {
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  gap: 0;
  border-bottom: 1px solid #eee;
}

.admin-tabs button {
  padding: 10px 20px;
  border: none;
  background: none;
  font-size: 14px;
  font-weight: 500;
  color: #666;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  transition: all 0.2s;
}

.admin-tabs button.active {
  color: #c94f2d;
  border-bottom-color: #c94f2d;
  font-weight: 600;
}

/* Main */
.admin-main {
  max-width: 1100px;
  margin: 0 auto;
  padding: 24px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.toolbar h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
}

/* Table */
.table-wrap {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

thead {
  background: #fafafa;
}

th {
  padding: 12px 16px;
  text-align: left;
  font-size: 13px;
  font-weight: 600;
  color: #999;
  border-bottom: 1px solid #f0f0f0;
  white-space: nowrap;
}

td {
  padding: 12px 16px;
  border-bottom: 1px solid #f5f5f5;
  font-size: 14px;
  vertical-align: middle;
}

tr:last-child td { border-bottom: none; }

tr:hover { background: #fafafa; }

/* Cells */
.thumb-cell {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #7b2d18, #492015);
}

.thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumb-placeholder {
  font-size: 24px;
}

.name-cell strong {
  display: block;
  font-size: 14px;
}

.name-cell small {
  color: #999;
  font-size: 12px;
}

.price-cell {
  font-weight: 700;
  color: #c94f2d;
}

.status-badge {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.status-badge.on {
  background: #e6f7e9;
  color: #389e0d;
}

.status-badge.off {
  background: #fff2e8;
  color: #d46b08;
}

/* Buttons */
.action-btns {
  display: flex;
  gap: 8px;
}

.btn-sm {
  padding: 4px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  background: #fff;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-sm:hover { border-color: #c94f2d; color: #c94f2d; }

.btn-sm.toggle {
  border-color: #faad14;
  color: #d48806;
}

.btn-sm.toggle:hover {
  background: #faad14;
  color: #fff;
}

.btn-sm.danger {
  border-color: #ff4d4f;
  color: #ff4d4f;
}

.btn-sm.danger:hover {
  background: #ff4d4f;
  color: #fff;
}

.btn-primary {
  padding: 8px 20px;
  background: linear-gradient(135deg, #c94f2d, #de7743);
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(201, 79, 45, 0.25);
  transition: transform 0.15s;
}

.btn-primary:active { transform: scale(0.97); }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }

.btn-cancel {
  padding: 8px 20px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
}

/* Modal */
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  padding: 20px;
}

.modal {
  background: #fff;
  border-radius: 12px;
  padding: 28px;
  width: 100%;
  max-width: 520px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.2);
}

.modal-sm { max-width: 380px; }

.modal h3 {
  margin: 0 0 20px;
  font-size: 18px;
  font-weight: 700;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.form-grid label {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-grid label.full {
  grid-column: 1 / -1;
}

.form-grid span {
  font-size: 13px;
  font-weight: 600;
  color: #666;
}

.form-grid input,
.form-grid select {
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

.form-grid input:focus,
.form-grid select:focus {
  border-color: #c94f2d;
}

.upload-area {
  position: relative;
  width: 120px;
  height: 120px;
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.2s;
}

.upload-area:hover { border-color: #c94f2d; }

.file-input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #bbb;
  font-size: 13px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

@media (max-width: 768px) {
  .admin-main { padding: 16px; }
  .form-grid { grid-template-columns: 1fr; }
  .action-btns { flex-direction: column; gap: 4px; }
}
</style>
