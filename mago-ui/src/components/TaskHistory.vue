<script setup lang="ts">
import { ref, onMounted } from 'vue'

const tasks = ref<any[]>([])
const emit = defineEmits(['preview'])

const fetchTasks = async () => {
  const res = await fetch('http://localhost:8080/api/v1/terrain/tasks')
  const data = await res.json()
  tasks.value = data.reverse()
}

onMounted(() => {
  fetchTasks()
  setInterval(fetchTasks, 3000)
})

const handlePreview = (task: any) => {
  // 基于后端代理生成的 Web URL，Cesium 可完美识别
  const proxyUrl = `http://localhost:8080/api/v1/terrain/data/${task.id}/`
  console.log('预览地形 ID:', task.id, '代理 URL:', proxyUrl)
  emit('preview', proxyUrl)
}
</script>

<template>
  <div class="history-container">
    <table class="task-table">
      <thead>
        <tr><th>ID</th><th>状态</th><th>输出路径</th><th>操作</th></tr>
      </thead>
      <tbody>
        <tr v-for="task in tasks" :key="task.id">
          <td>#{{ task.id }}</td>
          <td><span :class="['badge', task.status]">{{ task.status }}</span></td>
          <td class="path-cell">{{ task.outputPath }}</td>
          <td>
            <button v-if="task.status === 'COMPLETED'" class="btn-sm" @click="handlePreview(task)">预览地形</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.history-container { padding: 2rem; }
.task-table { width: 100%; border-collapse: collapse; background: white; border-radius: 12px; overflow: hidden; border: 1px solid #e2e8f0; }
.task-table th, td { padding: 1rem; text-align: left; border-bottom: 1px solid #e2e8f0; font-size: 0.85rem; }
.badge { padding: 0.2rem 0.6rem; border-radius: 20px; font-size: 0.7rem; font-weight: bold; }
.badge.COMPLETED { background: #dcfce7; color: #16a34a; }
.badge.PROCESSING { background: #dbeafe; color: #2563eb; }
.path-cell { color: #64748b; font-family: monospace; max-width: 500px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.btn-sm { background: #2563eb; color: white; border: none; padding: 0.4rem 0.8rem; border-radius: 4px; cursor: pointer; }
</style>
