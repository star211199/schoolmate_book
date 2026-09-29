<template>
  <div class="theme-page">
    <div class="glass-card head-card">
      <div>
        <h2 class="anime-title">主题装扮</h2>
        <p class="text-muted">共 {{ themes.length }} 款二次元主题，点击立即切换，刷新后保持</p>
      </div>
      <div class="current-tip">
        当前：<b>{{ themeStore.currentTheme.name }}</b>
      </div>
    </div>

    <div class="theme-grid">
      <div
        v-for="t in themes"
        :key="t.key"
        class="theme-card"
        :class="{ active: themeStore.current === t.key }"
        @click="onApply(t.key)"
      >
        <div class="preview" :style="previewStyle(t.key)">
          <div class="pv-sidebar">
            <div class="pv-dot" :style="{ background: colors(t.key).brand }"></div>
            <div class="pv-line"></div>
            <div class="pv-line short"></div>
          </div>
          <div class="pv-main">
            <div class="pv-bar" :style="{ background: colors(t.key).brand }"></div>
            <div
              class="pv-bubble"
              :style="{ background: colors(t.key).other, color: colors(t.key).text }"
            ></div>
            <div
              class="pv-bubble me"
              :style="{ background: colors(t.key).brand, color: colors(t.key).onBrand }"
            ></div>
            <div class="pv-bubble" :style="{ background: colors(t.key).other }"></div>
          </div>
        </div>

        <div class="info">
          <div class="name-row">
            <span class="name">{{ t.name }}</span>
            <span class="en">{{ t.en }}</span>
            <span class="mode">{{ t.light ? '浅色' : '深色' }}</span>
          </div>
          <p class="desc">{{ t.desc }}</p>
        </div>

        <div v-if="themeStore.current === t.key" class="active-badge">使用中</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useThemeStore, THEME_LIST } from '@/stores/theme'

const themeStore = useThemeStore()
const themes = THEME_LIST

/** 预览用的色值，仅用于展示，与 themes.css 中的语义变量一一对应 */
const PREVIEW = {
  sakura: { brand: '#ff8fab', other: '#ffffff', text: '#4a4e69', onBrand: '#ffffff', bg: '#fbf8fa' },
  starry: { brand: '#6c8cff', other: '#2a2b4d', text: '#e8eaff', onBrand: '#ffffff', bg: '#14152c' },
  neon: { brand: '#a855f7', other: '#2b1f3d', text: '#e9dcff', onBrand: '#ffffff', bg: '#17111f' },
  mint: { brand: '#1d9e75', other: '#eaf7f0', text: '#1f3330', onBrand: '#ffffff', bg: '#f4fbf8' },
  sunset: { brand: '#e07a3f', other: '#f9ede2', text: '#3a2a22', onBrand: '#ffffff', bg: '#fdf7f2' }
}

function colors(key) {
  return PREVIEW[key] || PREVIEW.sakura
}

function previewStyle(key) {
  const c = colors(key)
  return { background: c.bg }
}

function onApply(key) {
  if (themeStore.current === key) return
  // 短暂开启过渡，让换肤过程平滑；过渡只作用于背景与文字，避免拖慢渲染
  document.documentElement.classList.add('theme-transition')
  themeStore.apply(key)
  setTimeout(() => document.documentElement.classList.remove('theme-transition'), 320)
  ElMessage.success(`已切换到「${themes.find((t) => t.key === key).name}」`)
}

onMounted(() => {
  themeStore.init()
})
</script>

<style scoped>
.theme-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.head-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}
.head-card h2 {
  margin: 0 0 4px;
  font-size: 20px;
}
.head-card p {
  margin: 0;
}
.current-tip {
  font-size: 13px;
  color: var(--color-text-muted);
}
.current-tip b {
  color: var(--color-brand-dark);
}

.theme-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.theme-card {
  position: relative;
  border-radius: var(--card-radius);
  overflow: hidden;
  cursor: pointer;
  background: var(--color-glass);
  backdrop-filter: blur(12px);
  border: 1px solid var(--color-glass-border);
  box-shadow: 0 8px 26px var(--color-shadow);
  transition: transform 0.18s, border-color 0.18s;
}
.theme-card:hover {
  transform: translateY(-3px);
}
.theme-card.active {
  border-color: var(--color-brand);
  box-shadow: 0 0 0 2px var(--color-brand-light), 0 10px 30px var(--color-shadow);
}

/* 迷你界面预览 */
.preview {
  display: flex;
  height: 116px;
  gap: 8px;
  padding: 12px;
}
.pv-sidebar {
  width: 34px;
  border-radius: 9px;
  background: rgba(128, 128, 128, 0.14);
  padding: 8px 6px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.pv-dot {
  width: 14px;
  height: 14px;
  border-radius: 5px;
}
.pv-line {
  height: 4px;
  border-radius: 2px;
  background: rgba(128, 128, 128, 0.32);
}
.pv-line.short {
  width: 60%;
}
.pv-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  justify-content: center;
}
.pv-bar {
  height: 8px;
  width: 42%;
  border-radius: 4px;
}
.pv-bubble {
  height: 15px;
  width: 62%;
  border-radius: 8px;
  opacity: 0.92;
}
.pv-bubble.me {
  align-self: flex-end;
}

.info {
  padding: 12px 15px 15px;
}
.name-row {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-bottom: 6px;
}
.name {
  font-size: 14.5px;
  font-weight: 600;
  color: var(--color-text);
}
.en {
  font-size: 11.5px;
  color: var(--color-text-subtle);
}
.mode {
  font-size: 11px;
  padding: 1px 7px;
  border-radius: 6px;
  background: var(--color-surface-2);
  color: var(--color-text-muted);
  margin-left: auto;
}
.desc {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.65;
  color: var(--color-text-muted);
}

.active-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  font-size: 11px;
  padding: 2px 9px;
  border-radius: 999px;
  background: var(--color-brand);
  color: #fff;
}
</style>
