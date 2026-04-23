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
    <nav>
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
    <div class="nav-help">
      <p>Version 1.12.0</p>
      <p>Local Cesium SDK</p>
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
}
.logo { display: flex; align-items: center; gap: 12px; margin-bottom: 3rem; }
.logo-icon { 
  background: #2563eb; width: 36px; height: 36px; border-radius: 8px; 
  display: flex; align-items: center; justify-content: center; font-weight: bold; 
}
.nav-item {
  padding: 0.8rem 1rem; border-radius: 8px; cursor: pointer;
  margin-bottom: 0.5rem; transition: background 0.2s; font-weight: 500;
}
.nav-item:hover { background: rgba(255,255,255,0.05); }
.nav-item.active { background: #2563eb; }
.nav-help { margin-top: auto; font-size: 0.8rem; color: #64748b; }
</style>
