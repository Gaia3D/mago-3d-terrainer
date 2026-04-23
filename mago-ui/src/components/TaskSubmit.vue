<script setup lang="ts">
import { reactive, ref, onMounted, watch } from 'vue'

const emit = defineEmits(['submitted'])
const isLoading = ref(false)
const message = ref('')
const messageType = ref('')

// 初始状态与默认值
const defaultForm = {
  input: '',
  output: '',
  log: '',
  temp: '',
  geoid: 'Ellipsoid',
  minDepth: 0,
  maxDepth: 14,
  intensity: 4.0,
  interpolationType: 'bilinear',
  priorityType: 'resolution',
  nodataValue: -9999,
  calculateNormals: false,
  mosaicSize: 16,
  rasterMaxSize: 8192,
  body: 'earth',
  metadata: false,
  waterMask: false,
  json: true,
  continueProcess: false,
  debug: false,
  leaveTemp: false
}

const form = reactive({ ...defaultForm })

onMounted(() => {
  const saved = localStorage.getItem('mago_params')
  if (saved) {
    try {
      Object.assign(form, JSON.parse(saved))
    } catch (e) {}
  }
})

watch(form, (newVal) => {
  localStorage.setItem('mago_params', JSON.stringify(newVal))
}, { deep: true })

const browseFolder = async (field: 'input' | 'output') => {
  try {
    const response = await fetch('http://localhost:8080/api/v1/utils/select-folder')
    const result = await response.json()
    if (result.path) form[field] = result.path
  } catch (e) {
    alert('无法打开文件夹选择器')
  }
}

const submit = async () => {
  if (!form.input || !form.output) {
    message.value = '请输入输入和输出目录。'
    messageType.value = 'error'
    return
  }

  isLoading.value = true
  message.value = '任务提交中...'
  messageType.value = 'info'

  try {
    const response = await fetch('http://localhost:8080/api/v1/terrain/process', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(form)
    })

    if (response.ok) {
      message.value = '任务已提交，正在后台处理。'
      messageType.value = 'success'
      setTimeout(() => emit('submitted'), 1000)
    } else {
      throw new Error('提交失败')
    }
  } catch (error) {
    message.value = '提交出错: ' + (error as Error).message
    messageType.value = 'error'
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <div class="form-scroll-container">
    <div class="form-content">
      <!-- 卡片 1: 目录 -->
      <div class="section-card">
        <div class="section-header">
          <span class="step-num">1</span>
          <h3>工作空间目录</h3>
        </div>
        <div class="form-row">
          <div class="field">
            <label>输入 GeoTIFF 路径 <small>(包含原始 DEM 文件的文件夹)</small></label>
            <div class="input-with-btn">
              <input v-model="form.input" type="text" placeholder="例如：E:\Data\Source_DEM" />
              <button class="btn-browse" @click="browseFolder('input')">浏览...</button>
            </div>
          </div>
        </div>
        <div class="form-row">
          <div class="field">
            <label>输出 Terrain 路径 <small>(切片数据的存储位置)</small></label>
            <div class="input-with-btn">
              <input v-model="form.output" type="text" placeholder="例如：E:\Data\Cesium_Terrain" />
              <button class="btn-browse" @click="browseFolder('output')">浏览...</button>
            </div>
          </div>
        </div>
      </div>

      <!-- 卡片 2: 参数 -->
      <div class="section-card">
        <div class="section-header">
          <span class="step-num">2</span>
          <h3>切片与简化参数</h3>
        </div>
        <div class="grid-3">
          <div class="field">
            <label>最小层级</label>
            <input v-model.number="form.minDepth" type="number" min="0" max="22" />
            <p class="help">通常为 0</p>
          </div>
          <div class="field">
            <label>最大层级</label>
            <input v-model.number="form.maxDepth" type="number" min="0" max="22" />
            <p class="help">建议：14-18</p>
          </div>
          <div class="field">
            <label>简化强度 (Intensity)</label>
            <input v-model.number="form.intensity" type="number" step="0.1" />
            <p class="help">默认 4.0 (RTIN 因子)</p>
          </div>
        </div>
        <div class="grid-2 mt-1">
          <div class="field">
            <label>插值算法</label>
            <select v-model="form.interpolationType">
              <option value="bilinear">双线性插值 (推荐)</option>
              <option value="nearest">最近邻插值 (快速)</option>
            </select>
          </div>
          <div class="field">
            <label>目标天体</label>
            <select v-model="form.body">
              <option value="earth">地球 (WGS84)</option>
              <option value="moon">月球</option>
            </select>
          </div>
        </div>
      </div>

      <!-- 卡片 3: 选项 -->
      <div class="section-card">
        <div class="section-header">
          <span class="step-num">3</span>
          <h3>扩展功能与质量</h3>
        </div>
        <div class="toggle-list">
          <label class="toggle-item">
            <input type="checkbox" v-model="form.calculateNormals" />
            <div class="toggle-content">
              <span class="title">生成顶点法线</span>
              <span class="desc">在 Cesium 中开启光照效果</span>
            </div>
          </label>
          <label class="toggle-item">
            <input type="checkbox" v-model="form.json" />
            <div class="toggle-content">
              <span class="title">生成 layer.json</span>
              <span class="desc">Cesium 自动加载所需元数据</span>
            </div>
          </label>
          <label class="toggle-item">
            <input type="checkbox" v-model="form.waterMask" />
            <div class="toggle-content">
              <span class="title">水体掩膜</span>
              <span class="desc">识别水面效果 (实验性)</span>
            </div>
          </label>
          <label class="toggle-item">
            <input type="checkbox" v-model="form.debug" />
            <div class="toggle-content">
              <span class="title">调试模式</span>
              <span class="desc">记录详细的处理日志</span>
            </div>
          </label>
        </div>
      </div>

      <!-- 提交栏 -->
      <div class="action-footer-inline">
        <div v-if="message" :class="['status-message', messageType]">
          {{ message }}
        </div>
        <button class="btn-primary" :disabled="isLoading" @click="submit">
          <span v-if="isLoading" class="spinner"></span>
          {{ isLoading ? '正在提交...' : '提交切片任务' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.form-scroll-container {
  height: 100%;
  overflow-y: auto;
  padding: 2rem;
}

.form-content {
  max-width: 900px;
  margin: 0 auto;
}

.section-card {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 1.5rem;
  margin-bottom: 1.5rem;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 1.2rem;
}

.step-num {
  width: 24px;
  height: 24px;
  background: #dbeafe;
  color: #2563eb;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 0.8rem;
}

.section-card h3 {
  margin: 0;
  font-size: 1.05rem;
  color: #334155;
}

.field label {
  display: block;
  font-size: 0.85rem;
  font-weight: 600;
  margin-bottom: 0.5rem;
  color: #475569;
}

.field label small {
  font-weight: 400;
  color: #64748b;
  margin-left: 4px;
}

.input-with-btn {
  display: flex;
  gap: 8px;
}

.input-with-btn input {
  flex: 1;
}

input, select {
  padding: 0.65rem 0.8rem;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 0.9rem;
  width: 100%;
  box-sizing: border-box;
}

input:focus, select:focus {
  outline: none;
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.btn-browse {
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  padding: 0 1rem;
  border-radius: 8px;
  cursor: pointer;
  white-space: nowrap;
  font-size: 0.85rem;
  transition: background 0.2s;
}

.btn-browse:hover {
  background: #e2e8f0;
}

.grid-3 {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1.2rem;
}

.grid-2 {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.2rem;
}

.help {
  font-size: 0.75rem;
  color: #64748b;
  margin-top: 4px;
}

.toggle-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.toggle-item {
  display: flex;
  gap: 12px;
  padding: 1rem;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s;
}

.toggle-item:hover {
  background: #f8fafc;
}

.toggle-item input {
  width: 18px;
  height: 18px;
  accent-color: #2563eb;
  margin: 0;
}

.toggle-content {
  display: flex;
  flex-direction: column;
}

.toggle-content .title {
  font-size: 0.9rem;
  font-weight: 600;
}

.toggle-content .desc {
  font-size: 0.75rem;
  color: #64748b;
}

.action-footer-inline {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  background: white;
  border: 1px dashed #2563eb;
  border-radius: 12px;
  margin-bottom: 2rem;
}

.btn-primary {
  background: #2563eb;
  color: white;
  border: none;
  padding: 0.8rem 2rem;
  border-radius: 8px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: background 0.2s;
}

.btn-primary:hover {
  background: #1d4ed8;
}

.status-message {
  font-weight: 500;
  font-size: 0.9rem;
}

.status-message.info { color: #2563eb; }
.status-message.success { color: #16a34a; }
.status-message.error { color: #dc2626; }

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }
.mt-1 { margin-top: 1rem; }
</style>
