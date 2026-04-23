<script setup lang="ts">
defineProps<{ activeTab: string }>()
defineEmits(['update:activeTab'])
</script>

<template>
  <div class="sidebar">
    <div class="logo">
      <div class="logo-icon">M</div>
      <h2>Mago 3D</h2>
    </div>
    <nav class="nav-list">
      <div 
        :class="['nav-item', { active: activeTab === 'submit' }]" 
        @click="$emit('update:activeTab', 'submit')"
      >🚀 任务提交</div>
      <div 
        :class="['nav-item', { active: activeTab === 'history' }]" 
        @click="$emit('update:activeTab', 'history')"
      >📜 任务历史</div>
      <div 
        :class="['nav-item', { active: activeTab === 'viewer' }]" 
        @click="$emit('update:activeTab', 'viewer')"
      >🌍 地形预览</div>
    </nav>
    <div class="footer-info">
      <p class="version">Version 1.12.0</p>
      <p class="powered">Gaia3D Terrainer</p>
    </div>
  </div>
</template>

<style scoped>
.sidebar {
  width: 240px;
  background-color: #0f172a;
  color: white;
  display: flex;
  flex-direction: column;
  padding: 1.5rem;
  height: 100vh;
  box-sizing: border-box;
  border-right: 1px solid #334155;
}
.logo { display: flex; align-items: center; gap: 12px; margin-bottom: 2.5rem; flex-shrink: 0; }
.logo-icon { 
  background: #3b82f6; width: 32px; height: 32px; border-radius: 6px; 
  display: flex; align-items: center; justify-content: center; font-weight: bold; 
}
.logo h2 { font-size: 1.1rem; margin: 0; letter-spacing: 0.05em; }

.nav-list { flex: 1; overflow-y: auto; }
.nav-item {
  padding: 0.7rem 1rem; border-radius: 8px; cursor: pointer;
  margin-bottom: 0.5rem; transition: all 0.2s; font-weight: 500; font-size: 0.9rem;
}
.nav-item:hover { background: rgba(255,255,255,0.05); }
.nav-item.active { background: #3b82f6; color: white; box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3); }

.footer-info { 
  margin-top: auto; padding-top: 1rem; border-top: 1px solid #1e293b; 
  flex-shrink: 0; 
}
.version { font-size: 0.75rem; color: #94a3b8; margin: 0; font-weight: 600; }
.powered { font-size: 0.7rem; color: #64748b; margin: 4px 0 0 0; }
</style>
