<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import * as Cesium from 'cesium'
import 'cesium/Build/Cesium/Widgets/widgets.css'

const props = defineProps<{ initialUrl?: string }>()
const showWireframe = ref(false)
let viewer: Cesium.Viewer | null = null

const initCesium = () => {
  if (viewer) return
  viewer = new Cesium.Viewer('cesiumContainer', {
    terrain: null,
    imageryProvider: new Cesium.ArcGisMapServerImageryProvider({
      url: 'https://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer'
    }),
    baseLayerPicker: false, timeline: false, animation: false,
    geocoder: false, infoBox: false, selectionIndicator: false
  })
  viewer.scene.globe.depthTestAgainstTerrain = true
  
  if (props.initialUrl) loadTerrain(props.initialUrl)
}

const loadTerrain = async (url: string) => {
  if (!viewer || !url) return
  try {
    const provider = await Cesium.CesiumTerrainProvider.fromUrl(url)
    viewer.scene.setTerrain(new Cesium.Terrain(provider))
    
    // 定位到 layer.json 范围
    const res = await fetch(`${url}layer.json`)
    const json = await res.json()
    if (json.bounds) {
      viewer.camera.flyTo({
        destination: Cesium.Rectangle.fromDegrees(...json.bounds),
        duration: 2
      })
    }
  } catch (e) {
    console.error('地形加载失败', e)
  }
}

watch(() => props.initialUrl, (newUrl) => {
  if (newUrl) loadTerrain(newUrl)
})

watch(showWireframe, (val) => {
  if (viewer) (viewer.scene.globe as any)._surface._tileProvider._debug.wireframe = val
})

onMounted(() => initCesium())
onUnmounted(() => { viewer?.destroy(); viewer = null })
</script>

<template>
  <div class="viewer-wrapper">
    <div class="viewer-tools">
      <label class="toggle-wire"><input type="checkbox" v-model="showWireframe" /> 显示三角形网格</label>
    </div>
    <div id="cesiumContainer"></div>
  </div>
</template>

<style scoped>
.viewer-wrapper { width: 100%; height: 100%; position: relative; }
#cesiumContainer { width: 100%; height: 100%; }
.viewer-tools {
  position: absolute; top: 10px; left: 10px; z-index: 10;
  background: rgba(255,255,255,0.85); padding: 8px 12px; border-radius: 6px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
}
.toggle-wire { font-size: 0.8rem; font-weight: bold; color: #334155; display: flex; align-items: center; gap: 6px; cursor: pointer; }
</style>
