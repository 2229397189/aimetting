<template>
  <div class="code-editor" :class="{ 'is-disabled': disabled }">
    <!-- 行号 -->
    <div class="code-editor__gutter" ref="gutterEl">
      <div class="code-editor__lines">
        <div v-for="n in lineCount" :key="n" class="code-editor__ln">{{ n }}</div>
      </div>
    </div>

    <!-- 编辑区：高亮层 + 透明输入框叠加 -->
    <div class="code-editor__area">
      <pre class="code-editor__pre" ref="preEl" aria-hidden="true"><code v-html="rendered"></code></pre>
      <textarea
        ref="taEl"
        class="code-editor__ta"
        :value="modelValue"
        :placeholder="placeholder"
        :disabled="disabled"
        :maxlength="maxlength"
        wrap="off"
        spellcheck="false"
        autocapitalize="off"
        autocomplete="off"
        @input="onInput"
        @scroll="syncScroll"
        @keydown.tab.prevent="onTab"
        @keydown="onKeydown"
      ></textarea>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'

/** 支持的高亮语言 */
export type EditorLang = 'java' | 'python' | 'javascript'

const props = withDefaults(
  defineProps<{
    modelValue: string
    language?: EditorLang
    placeholder?: string
    disabled?: boolean
    maxlength?: number
  }>(),
  {
    language: 'java',
    placeholder: '在此输入代码…',
    disabled: false,
    maxlength: undefined,
  },
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'input', value: string): void
}>()

const taEl = ref<HTMLTextAreaElement | null>(null)
const preEl = ref<HTMLPreElement | null>(null)
const gutterEl = ref<HTMLDivElement | null>(null)

/* ------------------------------ 行号 ------------------------------ */

const lineCount = computed<number>(() => {
  const text = props.modelValue || ''
  if (text === '') return 1
  // 末尾换行也占一行，保证行号与高亮对齐
  return text.split('\n').length
})

/* ------------------------------ 语法高亮 ------------------------------ */

const KEYWORDS: Record<EditorLang, string[]> = {
  java: [
    'abstract', 'assert', 'boolean', 'break', 'byte', 'case', 'catch', 'char', 'class',
    'const', 'continue', 'default', 'do', 'double', 'else', 'enum', 'extends', 'final',
    'finally', 'float', 'for', 'goto', 'if', 'implements', 'import', 'instanceof', 'int',
    'interface', 'long', 'native', 'new', 'package', 'private', 'protected', 'public',
    'return', 'short', 'static', 'strictfp', 'super', 'switch', 'synchronized', 'this',
    'throw', 'throws', 'transient', 'try', 'void', 'volatile', 'while', 'true', 'false', 'null',
  ],
  python: [
    'and', 'as', 'assert', 'async', 'await', 'break', 'class', 'continue', 'def', 'del',
    'elif', 'else', 'except', 'finally', 'for', 'from', 'global', 'if', 'import', 'in',
    'is', 'lambda', 'nonlocal', 'not', 'or', 'pass', 'raise', 'return', 'try', 'while',
    'with', 'yield', 'True', 'False', 'None', 'self',
  ],
  javascript: [
    'break', 'case', 'catch', 'class', 'const', 'continue', 'debugger', 'default', 'delete',
    'do', 'else', 'export', 'extends', 'finally', 'for', 'function', 'if', 'import', 'in',
    'instanceof', 'new', 'return', 'super', 'switch', 'this', 'throw', 'try', 'typeof',
    'var', 'void', 'while', 'with', 'yield', 'let', 'await', 'async', 'of', 'true', 'false',
    'null', 'undefined', 'NaN',
  ],
}

function escapeHtml(s: string): string {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

function buildRegex(lang: EditorLang): RegExp {
  // 单行注释：python 用 #，java/js 用 //
  const lineComment = lang === 'python' ? '#[^\\n]*' : '\\/\\/[^\\n]*'
  // 块注释：仅 java/js 支持 /* */
  const blockComment = lang === 'python' ? '' : '|\\/\\*[\\s\\S]*?\\*\\/'
  // 字符串：单引号 / 双引号 / 模板串
  const str = "'(?:[^'\\\\]|\\\\.)*'|\"(?:[^\"\\\\]|\\\\.)*\"|`(?:[^`\\\\]|\\\\.)*`"
  const kw = `\\b(?:${KEYWORDS[lang].join('|')})\\b`
  const num = '\\b\\d+(?:\\.\\d+)?\\b'
  const pattern = `(${lineComment}${blockComment})|(${str})|(${kw})|(${num})`
  return new RegExp(pattern, 'g')
}

const rendered = computed<string>(() => {
  const code = props.modelValue || ''
  if (!code) return ''
  const safe = escapeHtml(code)
  const re = buildRegex(props.language)
  const html = safe.replace(re, (match, g1: string, g2: string, g3: string, g4: string) => {
    if (g1) return `<span class="tk-comment">${g1}</span>`
    if (g2) return `<span class="tk-string">${g2}</span>`
    if (g3) return `<span class="tk-keyword">${g3}</span>`
    if (g4) return `<span class="tk-number">${g4}</span>`
    return match
  })
  // 末尾补一个换行，保证最后一行也能渲染出高度
  return html + '\n'
})

/* ------------------------------ 事件 ------------------------------ */

function onInput(e: Event): void {
  const value = (e.target as HTMLTextAreaElement).value
  emit('update:modelValue', value)
  emit('input', value)
}

/** Tab 缩进：插入两个空格，并保持光标位置 */
function onTab(e: KeyboardEvent): void {
  const ta = taEl.value
  if (!ta || props.disabled) return
  const start = ta.selectionStart
  const end = ta.selectionEnd
  const value = props.modelValue
  const next = value.slice(0, start) + '  ' + value.slice(end)
  emit('update:modelValue', next)
  nextTick(() => {
    if (taEl.value) {
      taEl.value.selectionStart = taEl.value.selectionEnd = start + 2
      taEl.value.focus()
    }
  })
}

/** Shift+Tab / 其他按键透传给父级，这里仅占位以方便扩展 */
function onKeydown(): void {
  /* 预留：后续可加入括号补全、缩进智能处理等 */
}

/** 输入框滚动时，同步高亮层与行号的滚动位置 */
function syncScroll(e: Event): void {
  const ta = e.target as HTMLTextAreaElement
  if (preEl.value) {
    preEl.value.scrollTop = ta.scrollTop
    preEl.value.scrollLeft = ta.scrollLeft
  }
  if (gutterEl.value) {
    gutterEl.value.scrollTop = ta.scrollTop
  }
}
</script>

<style scoped lang="scss">
.code-editor {
  display: flex;
  border: 1px solid var(--border-color, #dcdfe6);
  border-radius: 6px;
  overflow: hidden;
  background: #1e1e2e;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, 'Courier New', monospace;
  min-height: 320px;
}

.code-editor.is-disabled {
  opacity: 0.7;
  pointer-events: none;
}

.code-editor__gutter {
  flex: 0 0 auto;
  width: 44px;
  padding: 12px 0;
  overflow: hidden;
  background: #181825;
  border-right: 1px solid #313244;
  color: #6c7086;
  font-size: 13px;
  line-height: 1.6;
  text-align: right;
  user-select: none;
}

.code-editor__lines {
  display: flex;
  flex-direction: column;
}

.code-editor__ln {
  padding: 0 8px 0 0;
  height: calc(13px * 1.6);
  box-sizing: border-box;
}

.code-editor__area {
  position: relative;
  flex: 1 1 auto;
  min-width: 0;
  min-height: 320px;
  overflow: hidden;
}

.code-editor__pre,
.code-editor__ta {
  margin: 0;
  padding: 12px 14px;
  border: 0;
  width: 100%;
  height: 100%;
  min-height: 0;
  box-sizing: border-box;
  font-family: inherit;
  font-size: 13px;
  line-height: 1.6;
  tab-size: 2;
  white-space: pre;
  overflow: auto;
  word-break: normal;
}

.code-editor__pre {
  position: absolute;
  inset: 0;
  z-index: 1;
  color: #cdd6f4;
  pointer-events: none;
  background: transparent;
}

.code-editor__pre code {
  font: inherit;
  white-space: pre;
  word-break: normal;
}

.code-editor__ta {
  position: absolute;
  inset: 0;
  z-index: 2;
  resize: vertical;
  color: transparent;
  background: transparent;
  caret-color: #f5e0dc;
  outline: none;
}

.code-editor__ta::placeholder {
  color: #585b70;
}

.code-editor__ta:focus {
  border-color: var(--color-primary, #4f46e5);
}

/* 语法高亮配色（基于 Catppuccin Mocha 思路） */
.tk-comment {
  color: #6c7086;
  font-style: italic;
}
.tk-string {
  color: #a6e3a1;
}
.tk-keyword {
  color: #cba6f7;
  font-weight: 600;
}
.tk-number {
  color: #fab387;
}
</style>
