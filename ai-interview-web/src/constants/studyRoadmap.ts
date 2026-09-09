/**
 * 学习路线映射表：维度 → 提升建议
 * 供「报告详情页 · 学习路线」区块使用（见 views/ReportDetail.vue）
 * 基于五维分数中最弱的维度，给出可执行、可勾选的提升路径。
 */

import type { DimensionKey } from '@/types'

/** 单个维度的学习建议 */
export interface DimensionStudy {
  /** 提升目标 */
  goal: string
  /** 建议周期，如「2 周」 */
  cycle: string
  /** 具体动作（2-4 条，每条配一个可勾选进度） */
  actions: string[]
}

/**
 * 维度 → 学习建议映射
 * key 与后端 DimensionKey 枚举一致，缺失维度时由调用方兜底。
 */
export const STUDY_ROADMAP: Record<DimensionKey, DimensionStudy> = {
  PROFESSIONAL: {
    goal: '夯实岗位核心知识，形成可复述、可迁移的知识体系',
    cycle: '3 周',
    actions: [
      '按方向刷八股：Java 方向重点 JVM / 并发 / 集合 / 框架，每日 3 题并口述',
      '每题复盘到「原理 → 源码 → 场景」三层，沉淀成笔记',
      '用费曼学习法对外讲解一个知识点，验证是否真正理解',
    ],
  },
  EXPRESSION: {
    goal: '让回答结构清晰、重点突出、时长可控',
    cycle: '2 周',
    actions: [
      '答题先给结论再展开，单题控制在 90 秒内',
      '用「是什么 → 为什么 → 怎么做」三句式刻意练习',
      '录音回听，删掉口头禅与冗余铺垫',
    ],
  },
  LOGIC: {
    goal: '提升分析的条理性与论证闭环度',
    cycle: '2 周',
    actions: [
      '用「总-分-总」结构，先框架后细节',
      '复杂问题先画思维导图再作答，避免跳步与遗漏',
      '每轮回答结尾做一句总结，呼应开头结论',
    ],
  },
  PROJECT_DEPTH: {
    goal: '把项目讲出业务价值与个人真实贡献',
    cycle: '2 周',
    actions: [
      '用 STAR 法则重写项目经历，补上量化结果与踩坑复盘',
      '准备 2 个「难点 + 你的方案 + 业务收益」的深入故事',
      '梳理技术选型对比，能说清为什么不用其他方案',
    ],
  },
  POTENTIAL: {
    goal: '展现持续学习与解决未知问题的能力',
    cycle: '4 周',
    actions: [
      '补充对新技术的理解与思考，展示清晰的学习路径',
      '关注一个技术方向的前沿动态，每周输出 1 篇短评',
      '在 GitHub 上做一个小项目，体现动手与迭代意识',
    ],
  },
}

/** 维度缺失时的兜底建议（保证任何维度都有内容） */
export const STUDY_ROADMAP_FALLBACK: DimensionStudy = {
  goal: '针对该维度继续打磨，查漏补缺',
  cycle: '2 周',
  actions: [
    '梳理该维度的薄弱点，制定专项练习计划',
    '结合真题与项目实践反复巩固',
    '定期回顾并口述核心要点',
  ],
}
