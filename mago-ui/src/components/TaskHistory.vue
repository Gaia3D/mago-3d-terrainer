<script setup lang="ts">
import { ref, onMounted } from 'vue'

const tasks = ref<any[]>([])
const emit = defineEmits(['preview'])

const fetchTasks = async () => {
  try {
    const res = await fetch('http://localhost:8080/api/v1/terrain/tasks')
    const data = await res.json()
    tasks.value = data.reverse()
  } catch (e) {
    console.error('Fetch error', e)
  }
}

onMounted(() => {
  fetchTasks()
  setInterval(fetchTasks, 5000)
})

const handlePreview = (task: any) => {
  let path = task.outputPath.replace(/\\/g, '/')
  path = path.replace(':', '')
  if (!path.startsWith('/')) path = '/' + path
  if (!path.endsWith('/')) path += '/'
  const finalUrl = `http://localhost:8080/api/v1/terrain/data/${task.id}/`
  emit('preview', finalUrl)
}
</script>

<template>
  <div class="history-page">
    <div class="table-card">
      <table class="task-table">
        <thead>
          <tr>
            <th>#</th>
            <th>状态</th>
            <th>层级 / 强度</th>
            <th>天体 / 插值</th>
            <th>输入路径</th>
            <th>输出路径</th>
            <th>耗时</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="task in tasks" :key="task.id">
            <td class="id-cell">{{ task.id }}</td>
            <td><span :class="['badge', task.status]">{{ task.status }}</span></td>
            <td>
              <div class="param-val">L{{ task.minDepth }} - {{ task.maxDepth }}</div>
              <div class="param-label">Int: {{ task.intensity }}</div>
            </td>
            <td>
              <div class="param-val">{{ task.body }}</div>
              <div class="param-label">{{ task.interpolationType }}</div>
            </td>
            <td class="path-cell" :title="task.inputPath"><code>{{ task.inputPath }}</code></td>
            <td class="path-cell" :title="task.outputPath"><code>{{ task.outputPath }}</code></td>
            <td class="time-cell">
              <div class="val">{{ task.durationSeconds ? task.durationSeconds + 's' : '-' }}</div>
              <div class="label">{{ new Date(task.startTime).toLocaleTimeString() }}</div>
            </td>
            <td>
              <button 
                v-if="task.status === 'COMPLETED'" 
                class="btn-view" 
                @click="handlePreview(task)"
              >查看预览</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="tasks.length === 0" class="empty-state">暂无执行任务</div>
    </div>
  </div>
</template>

<style scoped>
.history-page { padding: 1.25rem; height: 100%; box-sizing: border-box; background: #0f172a; overflow-y: auto; }
.table-card { background: #1e293b; border: 1px solid #334155; border-radius: 10px; overflow: hidden; }

.task-table { width: 100%; border-collapse: collapse; }
.task-table th { background: #0f172a; padding: 1rem; font-size: 0.7rem; color: #94a3b8; text-align: left; text-transform: uppercase; border-bottom: 1px solid #334155; }
.task-table td { padding: 0.75rem 1rem; font-size: 0.8rem; border-bottom: 1px solid #334155; color: #f8fafc; }

.id-cell { font-family: monospace; font-weight: bold; color: #3b82f6; }
.badge { padding: 0.2rem 0.5rem; border-radius: 4px; font-size: 0.65rem; font-weight: 900; }
.badge.COMPLETED { background: #065f46; color: #34d399; }
.badge.PROCESSING { background: #1e3a8a; color: #93c5fd; animation: pulse 2s infinite; }
.badge.FAILED { background: #7f1d1d; color: #f87171; }

.param-val { font-weight: 700; color: #f1f5f9; }
.param-label { font-size: 0.7rem; color: #64748b; margin-top: 2px; }

.path-cell { max-width: 180px; }
.path-cell code { 
  display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; 
  background: #0f172a; padding: 0.2rem 0.4rem; border-radius: 4px;
  font-family: 'JetBrains Mono', 'Consolas', monospace; font-size: 0.75rem; color: #34d399;
}

.time-cell .val { font-weight: bold; color: #3b82f6; }
.time-cell .label { font-size: 0.65rem; color: #64748b; }

.btn-view { background: #3b82f6; color: white; border: none; padding: 0.4rem 0.8rem; border-radius: 4px; cursor: pointer; font-weight: bold; font-size: 0.75rem; }
.btn-view:hover { background: #2563eb; }

.empty-state { padding: 4rem; text-align: center; color: #475569; font-size: 0.85rem; }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.6; } }
</style>
