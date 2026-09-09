<template>
  <div ref="chartRef" class="radar-chart" :style="{ height: height + 'px' }"></div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { DIMENSION_LABELS, normalizeDimensions } from '@/utils/dict'
import { useChartTheme } from '@/composables/useChartTheme'

/**
 * 五维能力雷达图（ECharts）
 * 维度：专业技能 / 表达沟通 / 逻辑思维 / 项目深度 / 潜力
 */
const props = withDefaults(
  defineProps<{
    /** 五维分数，key 为维度枚举，value 为 0-100 */
    dimensions?: Record<string, number> | null
    /** 图表高度（px） */
    height?: number
    /** 系列名称 */
    seriesName?: string
  }>(),
  {
    dimensions: null,
    height: 320,
    seriesName: '本次得分',
  },
)

const chartRef = ref<HTMLDivElement | null>(null)
let chart: echarts.ECharts | null = null
let resizeHandler: (() => void) | null = null

const { isDark, palette } = useChartTheme()

/** 构造 ECharts option */
function buildOption(): echarts.EChartsOption {
  const data = normalizeDimensions(props.dimensions, 60)
  const keys = Object.keys(DIMENSION_LABELS)
  const indicator = keys.map((key) => ({
    name: DIMENSION_LABELS[key],
    max: 100,
  }))
  const values = keys.map((key) => Number(data[key] ?? 0))

  return {
    tooltip: {
      trigger: 'item',
      textStyle: { color: palette.value.text },
      formatter: () => {
        return keys
          .map((key, i) => `${DIMENSION_LABELS[key]}：${values[i]} 分`)
          .join('<br/>')
      },
    },
    radar: {
      center: ['50%', '54%'],
      radius: '66%',
      indicator,
      axisName: {
        color: palette.value.text,
        fontSize: 12,
      },
      splitArea: {
        areaStyle: {
          color: palette.value.areaBg,
        },
      },
      axisLine: {
        lineStyle: { color: palette.value.axisLine },
      },
      splitLine: {
        lineStyle: { color: palette.value.splitLine },
      },
    },
    series: [
      {
        type: 'radar',
        name: props.seriesName,
        data: [
          {
            value: values,
            name: props.seriesName,
            areaStyle: {
              color: 'rgba(79, 70, 229, 0.18)',
            },
            lineStyle: {
              color: '#4f46e5',
              width: 2,
            },
            itemStyle: {
              color: '#4f46e5',
            },
            label: {
              show: true,
              fontSize: 11,
              color: '#4f46e5',
              formatter: (p: any) => String(p.value),
            },
          },
        ],
      },
    ],
  }
}

/** 渲染 / 更新图表 */
function render(): void {
  if (!chartRef.value) return
  if (!chart) {
    chart = echarts.init(chartRef.value)
  }
  chart.setOption(buildOption(), true)
}

onMounted(() => {
  render()
  // 窗口尺寸变化时自适应
  resizeHandler = () => {
    if (chart) chart.resize()
  }
  window.addEventListener('resize', resizeHandler)
})

watch(
  () => props.dimensions,
  () => {
    render()
  },
  { deep: true },
)

// 主题切换时重绘，使轴线 / 文字配色跟随暗色
watch(isDark, () => {
  render()
})

onBeforeUnmount(() => {
  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler)
    resizeHandler = null
  }
  if (chart) {
    chart.dispose()
    chart = null
  }
})
</script>

<style scoped lang="scss">
.radar-chart {
  width: 100%;
  min-height: 260px;
}
</style>
