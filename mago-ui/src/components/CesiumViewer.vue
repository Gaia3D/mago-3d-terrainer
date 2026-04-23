<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import * as Cesium from 'cesium'
import 'cesium/Build/Cesium/Widgets/widgets.css'

const props = defineProps<{ initialUrl?: string }>()
const TK = '5f3c44adaee9ddfe141db0137043199b'

// 视图模式: TDT (天地图), CESIUM (ArcGIS), DEBUG (红网格)
const viewMode = ref('CESIUM')
const mouseInfo = ref({ lon: '-', lat: '-', height: '-' })
let viewer: Cesium.Viewer | null = null

const initCesium = async () => {
  if (viewer || !document.getElementById('cesiumContainer')) return
  
  // 1. 初始化 Viewer (最稳健的基础配置)
  viewer = new Cesium.Viewer('cesiumContainer', {
    terrain: null,
    baseLayerPicker: false,
    geocoder: false,
    homeButton: true,
    animation: false,
    timeline: false,
    skyAtmosphere: false,
    infoBox: true,
    fullscreenButton: false,
    selectionIndicator: false
  })

  // 2. 视觉基准设置 (光照开启，地形感更强)
  viewer.scene.globe.enableLighting = true
  viewer.scene.globe.depthTestAgainstTerrain = true
  viewer.scene.fog.enabled = false

  // 3. 初始加载天地图 (WMTS 墨卡托)
  updateImageryLayers()

  // 4. 核心：使用 postRender 每一帧强制将网格设为红色
  viewer.scene.postRender.addEventListener(updateWireframeStyle)

  // 5. 鼠标移动监听
  const handler = new Cesium.ScreenSpaceEventHandler(viewer.scene.canvas)
  handler.setInputAction((movement: any) => {
    const ray = viewer!.camera.getPickRay(movement.endPosition)
    if (!ray) return
    const cartesian = viewer!.scene.globe.pick(ray, viewer!.scene)
    if (cartesian) {
      const carto = Cesium.Cartographic.fromCartesian(cartesian)
      mouseInfo.value = {
        lon: Cesium.Math.toDegrees(carto.longitude).toFixed(6),
        lat: Cesium.Math.toDegrees(carto.latitude).toFixed(6),
        height: carto.height.toFixed(2)
      }
    }
  }, Cesium.ScreenSpaceEventType.MOUSE_MOVE)

  if (props.initialUrl) loadTerrain(props.initialUrl)
}

// 切换图层逻辑
const updateImageryLayers = async () => {
  if (!viewer) return
  const layers = viewer.imageryLayers
  layers.removeAll()

  if (viewMode.value === 'TDT') {
    // 加载天地图影像
    layers.addImageryProvider(new Cesium.WebMapTileServiceImageryProvider({
      url: `https://t0.tianditu.gov.cn/img_w/wmts?tk=${TK}`,
      layer: 'img', style: 'default', format: 'tiles', tileMatrixSetID: 'w',
      tilingScheme: new Cesium.WebMercatorTilingScheme()
    }))
    // 加载天地图注记
    layers.addImageryProvider(new Cesium.WebMapTileServiceImageryProvider({
      url: `https://t0.tianditu.gov.cn/cia_w/wmts?tk=${TK}`,
      layer: 'cia', style: 'default', format: 'tiles', tileMatrixSetID: 'w',
      tilingScheme: new Cesium.WebMercatorTilingScheme()
    }))
  } else if (viewMode.value === 'CESIUM') {
    // 加载 ArcGIS 卫星图
    try {
      const provider = await Cesium.ArcGisMapServerImageryProvider.fromUrl(
        'https://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer'
      )
      layers.addImageryProvider(provider)
    } catch (e) { console.warn('ArcGIS 加载失败') }
  } else if (viewMode.value === 'DEBUG') {
    // 调试模式下：不加载任何地图，露出球体底色
  }
}

// 每一帧强制渲染红色网格
const updateWireframeStyle = () => {
  if (!viewer) return
  const globe = viewer.scene.globe
  const tileProvider = (globe as any)._surface._tileProvider
  if (tileProvider) {
    const isDebug = viewMode.value === 'DEBUG'
    tileProvider._debug.wireframe = isDebug
    if (isDebug) {
      tileProvider._debug.wireframeColor = Cesium.Color.RED
    }
  }
}

const loadTerrain = async (url: string) => {
  if (!viewer || !url) return
  try {
    const provider = await Cesium.CesiumTerrainProvider.fromUrl(url)
    viewer.scene.setTerrain(new Cesium.Terrain(provider))
    
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

watch(() => props.initialUrl, (newUrl) => { if (newUrl) loadTerrain(newUrl) })
watch(viewMode, () => updateImageryLayers())

onMounted(() => initCesium())
onUnmounted(() => { 
  if (viewer) {
    viewer.scene.postRender.removeEventListener(updateWireframeStyle)
    viewer.destroy()
  }
  viewer = null 
})
</script>

<template>
  <div class="viewer-wrapper">
    <div class="viewer-tools">
      <div class="btn-group">
        <button :class="{ active: viewMode === 'TDT' }" @click="viewMode = 'TDT'">天地图</button>
        <button :class="{ active: viewMode === 'CESIUM' }" @click="viewMode = 'CESIUM'">Cesium</button>
        <button :class="{ active: viewMode === 'DEBUG' }" @click="viewMode = 'DEBUG'">网格调试</button>
      </div>
    </div>

    <div class="coords-info">
      <div class="info-item"><span>经度:</span> <strong>{{ mouseInfo.lon }}</strong></div>
      <div class="info-item"><span>纬度:</span> <strong>{{ mouseInfo.lat }}</strong></div>
      <div class="info-item"><span>高程:</span> <strong class="height-text">{{ mouseInfo.height }} m</strong></div>
    </div>

    <div id="cesiumContainer"></div>
  </div>
</template>

<style scoped>
.viewer-wrapper { width: 100%; height: 100%; position: relative; background: #000; }
#cesiumContainer { width: 100%; height: 100%; }

.viewer-tools {
  position: absolute; top: 15px; left: 15px; z-index: 10;
  background: rgba(255,255,255,0.9); padding: 8px; border-radius: 8px;
  box-shadow: 0 4px 15px rgba(0,0,0,0.2);
}

.btn-group { display: flex; gap: 4px; }
.btn-group button {
  border: none; background: #f1f5f9; padding: 8px 16px; 
  font-size: 0.85rem; font-weight: 600; color: #475569;
  cursor: pointer; border-radius: 6px; transition: all 0.2s;
}
.btn-group button.active { background: #2563eb; color: white; }

.coords-info {
  position: absolute; bottom: 30px; right: 15px; z-index: 10;
  background: rgba(15, 23, 42, 0.85); color: white; padding: 10px 15px;
  border-radius: 8px; display: flex; gap: 15px; font-family: monospace;
  backdrop-filter: blur(4px); font-size: 0.8rem; pointer-events: none;
}
.height-text { color: #38bdf8; }
</style>
