import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/**
 * 主题仓库。
 *
 * 切换主题只做一件事：改 <html> 上的 data-theme 属性。
 * 所有颜色都由 CSS 变量驱动，因此不需要重新渲染任何组件，
 * 也绝不能用「JS 遍历 DOM 改样式」的写法。
 */

/** 主题清单，供主题选择页展示 */
export const THEME_LIST = [
  {
    key: 'sakura',
    name: '樱の和风',
    en: 'Sakura',
    light: true,
    desc: '延续原有视觉的粉白基调，樱粉主色配薰衣草紫。日间使用最舒服，樱花飘落动效与之最搭。'
  },
  {
    key: 'starry',
    name: '星海夜航',
    en: 'Starry',
    light: false,
    desc: '深靛蓝底色配星辉蓝主色，整体压暗、文字提亮。晚上翻看老照片与聊天记录时氛围最好。'
  },
  {
    key: 'neon',
    name: '紫夜霓虹',
    en: 'Neon',
    light: false,
    desc: '暗紫底加霓虹紫与青色双主色，对比度拉满的赛博感。年轻张扬，截图分享最抓眼球。'
  },
  {
    key: 'mint',
    name: '薄荷森屿',
    en: 'Mint',
    light: true,
    desc: '薄荷绿配青碧色，明亮清爽。长时间阅读留言墙和相册时眼睛负担最小。'
  },
  {
    key: 'sunset',
    name: '暮色橘颂',
    en: 'Sunset',
    light: true,
    desc: '暖橘主色配胭脂红点缀，整体偏暖。很契合毕业季夕阳的情绪，用于纪念册场景最合适。'
  }
]

const STORAGE_KEY = 'schoolmate-theme'
const DEFAULT_THEME = 'sakura'

export const useThemeStore = defineStore('theme', () => {
  const current = ref(localStorage.getItem(STORAGE_KEY) || DEFAULT_THEME)

  const currentTheme = computed(
    () => THEME_LIST.find((t) => t.key === current.value) || THEME_LIST[0]
  )

  /** 应用主题：写入 html 属性 + 持久化 */
  function apply(key) {
    const target = THEME_LIST.some((t) => t.key === key) ? key : DEFAULT_THEME
    current.value = target
    document.documentElement.setAttribute('data-theme', target)
    localStorage.setItem(STORAGE_KEY, target)
  }

  /**
   * 初始化。
   * 首屏防闪烁其实靠 index.html 里的同步内联脚本完成，
   * 这里只是把状态与 DOM 对齐（例如浏览器回退、存储被清空等情况）。
   */
  function init() {
    apply(current.value)
  }

  return { current, currentTheme, apply, init }
})
