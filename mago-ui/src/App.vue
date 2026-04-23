<script setup lang="ts">
import { ref } from 'vue'
import Sidebar from './components/Sidebar.vue'
import TaskSubmit from './components/TaskSubmit.vue'
import TaskHistory from './components/TaskHistory.vue'
import CesiumViewer from './components/CesiumViewer.vue'

const activeTab = ref('submit')
const currentPreviewUrl = ref('')

const handlePreview = (url: string) => {
  currentPreviewUrl.value = url
  activeTab.value = 'viewer'
}

const handleSubmitted = () => {
  activeTab.value = 'history'
}
</script>

<template>
  <div class="app-layout">
    <Sidebar v-model:activeTab="activeTab" />
    
    <main class="main-body">
      <header class="header">
        <h1>Mago 3D Terrainer <small>地形切片管理系统</small></h1>
      </header>

      <div class="content">
        <TaskSubmit v-if="activeTab === 'submit'" @submitted="handleSubmitted" />
        <TaskHistory v-if="activeTab === 'history'" @preview="handlePreview" />
        <CesiumViewer v-if="activeTab === 'viewer'" :initialUrl="currentPreviewUrl" />
      </div>
    </main>
  </div>
</template>

<style>
:root {
  --bg-darker: #0f172a;
  --bg-dark: #1e293b;
  --bg-card: #334155;
  --text-primary: #f8fafc;
  --text-secondary: #94a3b8;
  --primary: #3b82f6;
  --border: #475569;
}

body { margin: 0; padding: 0; background-color: var(--bg-darker); color: var(--text-primary); font-family: 'Inter', system-ui, sans-serif; }
.app-layout { display: flex; height: 100vh; overflow: hidden; }
.main-body { flex: 1; display: flex; flex-direction: column; overflow: hidden; background-color: var(--bg-darker); }

.header { 
  background: var(--bg-dark); padding: 0 2rem; border-bottom: 1px solid var(--border); 
  display: flex; align-items: center; height: 60px; box-sizing: border-box;
}
.header h1 { font-size: 1.1rem; margin: 0; font-weight: 700; color: var(--text-primary); }
.header h1 small { font-weight: 400; font-size: 0.8rem; color: var(--text-secondary); margin-left: 10px; }

.content { flex: 1; overflow: hidden; position: relative; }
</style>
