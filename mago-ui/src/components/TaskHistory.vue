<script setup lang="ts">
import { ref, onMounted, onUnmounted, reactive } from 'vue'

const tasks = ref<any[]>([])
const pagination = reactive({ page: 0, total: 0, size: 10, totalPages: 0 })
const filterStatus = ref('') 
const emit = defineEmits(['preview'])

const showLogModal = ref(false)
const currentLogTaskId = ref<number | null>(null)
const taskLogs = reactive<Record<number, string[]>>({})

let ws: WebSocket | null = null

const fetchTasks = async (page = 0) => {
  try {
    const res = await fetch(`http://localhost:8080/api/v1/terrain/tasks?page=${page}&size=${pagination.size}`)
    const data = await res.json()
    tasks.value = data.content
    pagination.page = data.number
    pagination.total = data.totalElements
    pagination.totalPages = data.totalPages
  } catch (e) { console.error(e) }
}

const initWebSocket = () => {
  ws = new WebSocket('ws://localhost:8080/ws/tasks')
  ws.onmessage = (event) => {
    const raw = event.data
    if (raw.startsWith('LOG:')) {
      const parts = raw.split(':')
      const id = parseInt(parts[1]), msg = parts.slice(2).join(':')
      if (!taskLogs[id]) taskLogs[id] = []
      taskLogs[id].push(msg)
      if (taskLogs[id].length > 1000) taskLogs[id].shift()
    } else if (raw.startsWith('PROGRESS:')) {
      const parts = raw.split(':')
      const id = parseInt(parts[1]), val = parseInt(parts[2])
      const task = tasks.value.find(t => t.id === id)
      if (task) task.progress = val
    } else if (raw.startsWith('STATUS:')) {
      const parts = raw.split(':')
      const id = parseInt(parts[1]), status = parts[2]
      const task = tasks.value.find(t => t.id === id)
      if (task) {
        task.status = status
        if (status === 'COMPLETED' || status === 'FAILED') fetchTasks(pagination.page)
      }
    }
  }
  ws.onclose = () => setTimeout(initWebSocket, 3000)
}

const deleteTask = async (id: number) => {
  if (!confirm('确定删除此任务记录吗？')) return
  await fetch(`http://localhost:8080/api/v1/terrain/tasks/${id}`, { method: 'DELETE' })
  fetchTasks(pagination.page)
}

const openLogs = (id: number) => {
  currentLogTaskId.value = id
  if (!taskLogs[id]) taskLogs[id] = ["等待日志流转..."]
  showLogModal.value = true
}

const getFilteredTasks = () => {
  if (!filterStatus.value) return tasks.value
  return tasks.value.filter(t => t.status === filterStatus.value)
}

onMounted(() => { fetchTasks(); initWebSocket() })
onUnmounted(() => ws?.close())

const getPreviewUrl = (task: any) => `http://localhost:8080/api/v1/terrain/data/${task.id}/`
</script>

<template>
  <div class="history-page">
    <div class="filter-bar">
      <div class="filter-group">
        <button :class="{ active: filterStatus === '' }" @click="filterStatus = ''">全部</button>
        <button :class="{ active: filterStatus === 'PROCESSING' }" @click="filterStatus = 'PROCESSING'">进行中</button>
        <button :class="{ active: filterStatus === 'COMPLETED' }" @click="filterStatus = 'COMPLETED'">已完成</button>
        <button :class="{ active: filterStatus === 'FAILED' }" @click="filterStatus = 'FAILED'">失败</button>
      </div>
      <div class="refresh-info">共 {{ pagination.total }} 条任务</div>
    </div>

    <div class="table-card">
      <div class="table-scroll-area">
        <table class="task-table">
          <thead>
            <tr>
              <th style="width: 50px">#</th>
              <th style="width: 140px">状态/进度</th>
              <th style="width: 100px">存储格式</th>
              <th style="width: 120px">配置</th>
              <th>输出路径</th>
              <th style="width: 100px">耗时</th>
              <th style="width: 130px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="task in getFilteredTasks()" :key="task.id">
              <td class="id-cell">{{ task.id }}</td>
              <td>
                <span :class="['badge', task.status]">{{ task.status }}</span>
                <div v-if="task.status === 'PROCESSING'" class="mini-progress">
                  <div class="bar" :style="{ width: (task.progress || 0) + '%' }"></div>
                  <span class="pct">{{ task.progress || 0 }}%</span>
                </div>
              </td>
              <td>
                <span :class="['fmt-tag', task.outputFormat]">{{ task.outputFormat === 'compact' ? 'PAK 紧凑' : 'FLAT 散列' }}</span>
              </td>
              <td>
                <div class="lvl">L{{ task.minDepth }}-{{ task.maxDepth }}</div>
                <div class="sub-info">Int: {{ task.intensity }}</div>
              </td>
              <td class="path-col">
                <div class="path-box" :title="task.outputPath"><code>{{ task.outputPath }}</code></div>
              </td>
              <td class="time-col">
                <div class="duration">{{ task.durationSeconds ? task.durationSeconds + 's' : '-' }}</div>
                <div class="start-time">{{ new Date(task.startTime).toLocaleTimeString() }}</div>
              </td>
              <td>
                <div class="btn-group-row">
                  <button class="action-btn log" title="日志" @click="openLogs(task.id)">📋</button>
                  <button v-if="task.status === 'COMPLETED'" class="action-btn view" @click="emit('preview', getPreviewUrl(task))">🌍</button>
                  <button class="action-btn del" @click="deleteTask(task.id)">🗑️</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination">
        <button class="nav-btn" :disabled="pagination.page === 0" @click="fetchTasks(pagination.page - 1)">上一页</button>
        <div class="page-numbers">
           <span v-for="p in pagination.totalPages" :key="p" 
                 :class="['page-num', { active: p - 1 === pagination.page }]"
                 @click="fetchTasks(p - 1)">{{ p }}</span>
        </div>
        <button class="nav-btn" :disabled="pagination.page >= pagination.totalPages - 1" @click="fetchTasks(pagination.page + 1)">下一页</button>
      </div>
    </div>

    <!-- 日志弹窗保持原样 -->
    <div v-if="showLogModal" class="modal-mask" @click.self="showLogModal = false">
      <div class="log-modal">
        <div class="modal-header"><h3>任务 #{{ currentLogTaskId }} 实时日志</h3><button class="close-btn" @click="showLogModal = false">×</button></div>
        <div class="log-body"><div v-for="(line, i) in taskLogs[currentLogTaskId!]" :key="i" class="log-line">{{ line }}</div></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.history-page { padding: 1.5rem; height: 100%; box-sizing: border-box; background: #0f172a; display: flex; flex-direction: column; gap: 1rem; overflow: hidden; }
.filter-bar { flex-shrink: 0; display: flex; justify-content: space-between; align-items: center; background: #1e293b; padding: 0.75rem 1.25rem; border-radius: 10px; border: 1px solid #334155; }
.filter-group button { background: #0f172a; border: 1px solid #334155; color: #94a3b8; padding: 0.4rem 1rem; border-radius: 6px; cursor: pointer; font-size: 0.8rem; }
.filter-group button.active { background: #3b82f6; color: white; }

.table-card { background: #1e293b; border: 1px solid #334155; border-radius: 12px; overflow: hidden; flex: 1; display: flex; flex-direction: column; }
.table-scroll-area { flex: 1; overflow-y: auto; }
.task-table { width: 100%; border-collapse: collapse; table-layout: fixed; }
.task-table thead th { position: sticky; top: 0; z-index: 10; background: #0f172a; padding: 1rem; font-size: 0.7rem; color: #94a3b8; text-align: left; }
.task-table td { padding: 0.8rem 1rem; font-size: 0.8rem; border-bottom: 1px solid #334155; color: #f8fafc; vertical-align: middle; }

.badge { padding: 0.2rem 0.5rem; border-radius: 4px; font-size: 0.65rem; font-weight: 900; text-transform: uppercase; }
.badge.COMPLETED { background: #065f46; color: #34d399; }
.badge.PROCESSING { background: #1e3a8a; color: #93c5fd; }
.badge.FAILED { background: #7f1d1d; color: #f87171; }

.fmt-tag { font-size: 0.65rem; font-weight: bold; padding: 2px 6px; border-radius: 4px; }
.fmt-tag.compact { background: #4338ca; color: #e0e7ff; }
.fmt-tag.flat { background: #374151; color: #d1d5db; }

.path-box code { display: block; background: #0f172a; padding: 0.3rem 0.5rem; border-radius: 4px; color: #34d399; font-family: monospace; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.btn-group-row { display: flex; gap: 8px; }
.action-btn { background: #334155; border: 1px solid #475569; color: white; width: 32px; height: 32px; border-radius: 8px; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: 0.2s; }
.action-btn:hover { background: #475569; border-color: #3b82f6; transform: translateY(-2px); }

.mini-progress { margin-top: 6px; height: 12px; background: #0f172a; border-radius: 4px; position: relative; overflow: hidden; }
.mini-progress .bar { height: 100%; background: #3b82f6; }
.mini-progress .pct { position: absolute; width: 100%; text-align: center; font-size: 0.6rem; top: 0; line-height: 12px; font-weight: bold; }

.pagination { flex-shrink: 0; padding: 0.75rem; display: flex; justify-content: center; align-items: center; gap: 1rem; background: #0f172a; }
.nav-btn { background: #1e293b; border: 1px solid #334155; color: white; padding: 0.4rem 1rem; border-radius: 6px; cursor: pointer; }
.page-num { padding: 0.3rem 0.6rem; border-radius: 4px; cursor: pointer; font-size: 0.75rem; color: #94a3b8; }
.page-num.active { background: #3b82f6; color: white; }

.modal-mask { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.8); display: flex; align-items: center; justify-content: center; z-index: 2000; }
.log-modal { width: 900px; height: 70vh; background: #1e293b; border-radius: 16px; display: flex; flex-direction: column; overflow: hidden; }
.modal-header { padding: 1.2rem; display: flex; justify-content: space-between; align-items: center; background: #0f172a; }
.log-body { flex: 1; overflow-y: auto; padding: 1.5rem; background: #050505; color: #34d399; font-family: monospace; font-size: 0.7rem; }
</style>
