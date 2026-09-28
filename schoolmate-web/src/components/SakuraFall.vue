<template>
  <canvas ref="canvasRef" class="sakura-canvas" />
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'

/**
 * 樱花飘落特效：Canvas 绘制花瓣，包含飘落 / 摇摆 / 旋转动画。
 * props: density 花瓣数量上限（默认 24）
 */
const props = defineProps({
  density: { type: Number, default: 24 }
})

const canvasRef = ref(null)
let ctx = null
let animationId = null
let petals = []

class Petal {
  constructor(width, height) {
    this.reset(width, height, true)
  }

  reset(width, height, randomY = false) {
    this.x = Math.random() * width
    this.y = randomY ? Math.random() * height : -20
    this.size = 6 + Math.random() * 8
    this.speedY = 0.6 + Math.random() * 1.2
    this.speedX = (Math.random() - 0.5) * 0.8
    this.angle = Math.random() * Math.PI * 2
    this.angleSpeed = (Math.random() - 0.5) * 0.03
    this.swing = Math.random() * Math.PI * 2
    this.swingSpeed = 0.01 + Math.random() * 0.02
    this.opacity = 0.5 + Math.random() * 0.5
    // 樱花色系
    this.color = Math.random() > 0.3 ? '#ffb7c5' : '#ffc2d4'
  }

  update(width, height) {
    this.swing += this.swingSpeed
    this.x += this.speedX + Math.sin(this.swing) * 0.5
    this.y += this.speedY
    this.angle += this.angleSpeed
    if (this.y > height + 20 || this.x < -30 || this.x > width + 30) {
      this.reset(width, height)
    }
  }

  draw(ctx) {
    ctx.save()
    ctx.translate(this.x, this.y)
    ctx.rotate(this.angle)
    ctx.globalAlpha = this.opacity
    ctx.fillStyle = this.color
    // 花瓣：两瓣椭圆组合
    ctx.beginPath()
    ctx.ellipse(0, 0, this.size / 2, this.size, 0, 0, Math.PI * 2)
    ctx.fill()
    ctx.beginPath()
    ctx.ellipse(this.size / 3, -this.size / 3, this.size / 2.6, this.size / 1.8, 0.6, 0, Math.PI * 2)
    ctx.fill()
    ctx.restore()
  }
}

function resize(canvas) {
  canvas.width = canvas.offsetWidth
  canvas.height = canvas.offsetHeight
}

function animate(canvas) {
  if (!ctx) return
  ctx.clearRect(0, 0, canvas.width, canvas.height)
  for (const petal of petals) {
    petal.update(canvas.width, canvas.height)
    petal.draw(ctx)
  }
  animationId = requestAnimationFrame(() => animate(canvas))
}

onMounted(() => {
  const canvas = canvasRef.value
  if (!canvas) return
  ctx = canvas.getContext('2d')
  resize(canvas)
  petals = Array.from({ length: props.density }, () => new Petal(canvas.width, canvas.height))
  animate(canvas)
  window.addEventListener('resize', () => resize(canvas))
})

onBeforeUnmount(() => {
  if (animationId) cancelAnimationFrame(animationId)
})
</script>

<style scoped>
.sakura-canvas {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 1;
}
</style>
