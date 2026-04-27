<script setup lang="ts">
import { reactive, ref, onMounted, watch } from 'vue'

const emit = defineEmits(['submitted'])
const isLoading = ref(false)
const message = ref('')
const messageType = ref('')

const defaultForm = {
  input: '', output: '', minDepth: 0, maxDepth: 14, intensity: 4.0,
  interpolationType: 'bilinear', body: 'earth', calculateNormals: false,
  json: true, debug: false, waterMask: false, outputFormat: 'flat'
}
const form = reactive({ ...defaultForm })

onMounted(() => {
  const saved = localStorage.getItem('mago_params')
  if (saved) Object.assign(form, JSON.parse(saved))
})

watch(form, (val) => localStorage.setItem('mago_params', JSON.stringify(val)), { deep: true })

const browseFolder = async (field: 'input' | 'output') => {
  try {
    const currentPath = form[field]
    const url = currentPath 
      ? `http://localhost:8080/api/v1/utils/select-folder?initialPath=${encodeURIComponent(currentPath)}`
      : 'http://localhost:8080/api/v1/utils/select-folder'
      
    const response = await fetch(url)
    const data = await response.json()
    if (data.path) form[field] = data.path
  } catch (e) {
    alert('无法打开文件夹选择器')
  }
}

const submit = async () => {
  if (!form.input || !form.output) {
    message.value = '请填写路径'; messageType.value = 'error'; return
  }
  isLoading.value = true
  message.value = '任务执行中...'; messageType.value = 'info'
  try {
    const res = await fetch('http://localhost:8080/api/v1/terrain/process', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(form)
    })
    if (res.ok) { emit('submitted') }
  } catch (e) {
    message.value = '提交失败'; messageType.value = 'error'
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <div class="submit-page">
    <div class="compact-grid">
      <!-- 路径配置 -->
      <div class="card full-width">
        <div class="card-header">1. 路径配置</div>
        <div class="field">
          <label>输入 (GeoTIFF 目录) <small>Source DEM Directory</small></label>
          <div class="input-with-btn">
            <input v-model="form.input" placeholder="请选择或输入包含 .tif 文件的文件夹" />
            <button class="btn-browse" @click="browseFolder('input')">浏览</button>
          </div>
        </div>
        <div class="field mt-05">
          <label>输出 (Terrain 目录) <small>Output Tiles Directory</small></label>
          <div class="input-with-btn">
            <input v-model="form.output" placeholder="请选择或输入切片存储目录" />
            <button class="btn-browse" @click="browseFolder('output')">浏览</button>
          </div>
        </div>
      </div>

      <!-- 参数配置 -->
      <div class="card">
        <div class="card-header">2. 核心参数</div>
        <div class="grid-2">
          <div class="field">
            <label>最小 / 最大层级</label>
            <div class="range-group">
              <input v-model.number="form.minDepth" type="number" />
              <span class="sep">至</span>
              <input v-model.number="form.maxDepth" type="number" />
            </div>
          </div>
          <div class="field">
            <label>简化强度</label>
            <input v-model.number="form.intensity" type="number" step="0.1" />
          </div>
          <div class="field">
            <label>插值方式</label>
            <select v-model="form.interpolationType">
              <option value="bilinear">双线性 (Bilinear)</option>
              <option value="nearest">最近邻 (Nearest)</option>
            </select>
          </div>
          <div class="field">
            <label>目标天体</label>
            <select v-model="form.body">
              <option value="earth">地球 (Earth)</option>
              <option value="moon">月球 (Moon)</option>
            </select>
          </div>
        </div>
      </div>

      <!-- 选项配置 -->
      <div class="card">
        <div class="card-header">3. 质量与扩展</div>
        <div class="options-grid">
          <label class="opt-box"><input type="checkbox" v-model="form.calculateNormals" /> <span>计算顶点法线</span></label>
          <label class="opt-box"><input type="checkbox" v-model="form.json" /> <span>生成 layer.json</span></label>
          <label class="opt-box"><input type="checkbox" v-model="form.waterMask" /> <span>水体掩膜</span></label>
          <label class="opt-box"><input type="checkbox" v-model="form.debug" /> <span>调试日志</span></label>
        </div>
        <div class="field mt-1">
          <label>输出格式 <small>Output Format</small></label>
          <select v-model="form.outputFormat">
            <option value="flat">散列文件 (Flat Folders)</option>
            <option value="compact">紧凑型数据库 (SQLite PAK)</option>
          </select>
        </div>
      </div>
    </div>

    <div class="footer-action">
      <div v-if="message" :class="['msg', messageType]">{{ message }}</div>
      <button class="btn-primary" :disabled="isLoading" @click="submit">
        <span v-if="isLoading" class="spinner"></span> 启动地形切片
      </button>
    </div>
  </div>
</template>

<style scoped>
/* 样式保留之前精致的定义 */
.submit-page { padding: 1.25rem; display: flex; flex-direction: column; height: 100%; box-sizing: border-box; background: #0f172a; }
.compact-grid { display: grid; grid-template-columns: 1fr 1.2fr; gap: 0.75rem; }
.full-width { grid-column: span 2; }
.card { background: #1e293b; border: 1px solid #334155; border-radius: 10px; padding: 1.25rem; }
.card-header { font-size: 0.8rem; font-weight: 800; color: #3b82f6; margin-bottom: 1rem; text-transform: uppercase; display: flex; align-items: center; gap: 8px; }
.field label { display: block; font-size: 0.75rem; color: #94a3b8; margin-bottom: 0.5rem; font-weight: 600; }
.field label small { font-weight: 400; opacity: 0.5; margin-left: 4px; }
input, select { background: #0f172a; border: 1px solid #475569; color: #f8fafc; padding: 0.6rem 0.8rem; border-radius: 6px; width: 100%; box-sizing: border-box; font-size: 0.85rem; transition: border-color 0.2s; }
input:focus { border-color: #3b82f6; outline: none; }
.input-with-btn { display: flex; gap: 8px; }
.btn-browse { background: #3b82f6; border: none; color: white; padding: 0 1.25rem; border-radius: 6px; cursor: pointer; font-size: 0.8rem; font-weight: 700; transition: background 0.2s; }
.btn-browse:hover { background: #2563eb; }
.grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
.range-group { display: flex; align-items: center; gap: 8px; }
.range-group input { text-align: center; }
.sep { color: #475569; font-size: 0.75rem; }
.options-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
.opt-box { display: flex; align-items: center; gap: 10px; font-size: 0.8rem; cursor: pointer; color: #f8fafc; background: #0f172a; padding: 0.8rem; border-radius: 8px; border: 1px solid #334155; transition: all 0.2s; }
.opt-box:hover { border-color: #3b82f6; background: rgba(59, 130, 246, 0.05); }
.opt-box input { width: 16px; height: 16px; accent-color: #3b82f6; }
.footer-action { display: flex; justify-content: flex-end; align-items: center; gap: 2rem; margin-top: auto; }
.btn-primary { background: #3b82f6; color: white; border: none; padding: 0.8rem 3rem; border-radius: 8px; font-weight: 800; cursor: pointer; transition: all 0.2s; box-shadow: 0 4px 14px rgba(59, 130, 246, 0.3); }
.btn-primary:hover { background: #2563eb; transform: translateY(-1px); }
.msg { font-size: 0.85rem; font-weight: 600; }
.msg.info { color: #3b82f6; }
.msg.error { color: #f87171; }
.spinner { width: 14px; height: 14px; border: 2px solid rgba(255,255,255,0.3); border-top-color: white; border-radius: 50%; animation: spin 1s linear infinite; display: inline-block; }
@keyframes spin { to { transform: rotate(360deg); } }
.mt-05 { margin-top: 0.5rem; }
</style>
