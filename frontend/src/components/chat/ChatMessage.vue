<script setup>
import { computed } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { escapeHtml, renderCodeBlock } from './chatMarkdown'
import 'highlight.js/styles/github.css'

const props = defineProps({
  role: { type: String, required: true },
  content: { type: String, required: true },
  userName: { type: String, default: '' },
})

const isUser = computed(() => props.role === 'user')

marked.use({
  renderer: {
    code: renderCodeBlock
  }
})

const renderedHtml = computed(() => {
  if (!props.content) return ''
  if (isUser.value) {
    return escapeHtml(props.content).replace(/\n/g, '<br>')
  }
  const rawHtml = marked.parse(props.content, { breaks: true, gfm: true })
  return DOMPurify.sanitize(rawHtml)
})
</script>

<template>
  <div
    class="message-appear flex items-start gap-3"
    :class="isUser ? 'flex-row-reverse' : 'flex-row'"
  >
    <!-- Avatar -->
    <div
      class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full select-none overflow-hidden text-xs font-bold"
      :class="isUser
        ? 'bg-accent text-white'
        : 'bg-surface border border-border-default text-text-tertiary'
      "
    >
      <span v-if="isUser">me</span>
      <span v-else>IA</span>
    </div>

    <!-- Bubble Column -->
    <div class="flex flex-col max-w-[75%]" :class="isUser ? 'items-end' : 'items-start'">
      <!-- Name Tag -->
      <span class="text-[11px] text-text-tertiary mb-1 px-0.5">
        {{ isUser ? (userName || '我') : 'IdealAgent' }}
      </span>

      <!-- Message Bubble -->
      <div
        class="relative px-4 py-2.5 text-sm leading-6 break-words"
        :class="isUser
          ? 'bg-accent text-white rounded-t-2xl rounded-l-2xl rounded-br-sm'
          : 'bg-surface border border-border-default text-text-primary rounded-t-2xl rounded-r-2xl rounded-bl-sm'
        "
      >
        <template v-if="isUser">
          <span v-html="renderedHtml"></span>
        </template>
        <template v-else>
          <div class="markdown-body" v-html="renderedHtml"></div>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Markdown styles for assistant messages */
.markdown-body :deep(*) {
  max-width: 100%;
}

.markdown-body :deep(p) {
  margin: 0 0 0.6em 0;
}
.markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4),
.markdown-body :deep(h5),
.markdown-body :deep(h6) {
  margin: 0.8em 0 0.4em 0;
  font-weight: 600;
  line-height: 1.3;
}
.markdown-body :deep(h1) { font-size: 1.35em; }
.markdown-body :deep(h2) { font-size: 1.2em; }
.markdown-body :deep(h3) { font-size: 1.1em; }
.markdown-body :deep(h4),
.markdown-body :deep(h5),
.markdown-body :deep(h6) { font-size: 1em; }

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  margin: 0.4em 0;
  padding-left: 1.5em;
}
.markdown-body :deep(li) {
  margin: 0.2em 0;
}

.markdown-body :deep(pre) {
  margin: 0.6em 0;
  padding: 0.75em 1em;
  border-radius: 8px;
  background-color: #f6f8fa;
  overflow-x: auto;
  font-size: 0.85em;
  line-height: 1.5;
}
.markdown-body :deep(pre code) {
  background: none;
  padding: 0;
  font-size: inherit;
  border-radius: 0;
}

.markdown-body :deep(code) {
  background-color: rgba(175, 184, 193, 0.2);
  padding: 0.15em 0.35em;
  border-radius: 4px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 0.9em;
}

.markdown-body :deep(blockquote) {
  margin: 0.6em 0;
  padding-left: 0.8em;
  border-left: 3px solid var(--color-border-default, #e5e7eb);
  color: var(--color-text-secondary, #6b7280);
}

.markdown-body :deep(table) {
  margin: 0.6em 0;
  border-collapse: collapse;
  width: 100%;
  font-size: 0.9em;
}
.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid var(--color-border-default, #e5e7eb);
  padding: 0.4em 0.6em;
  text-align: left;
}
.markdown-body :deep(th) {
  background-color: var(--color-bg-surface, #f8f9fa);
  font-weight: 600;
}

.markdown-body :deep(a) {
  color: var(--color-accent, #2563eb);
  text-decoration: none;
}
.markdown-body :deep(a:hover) {
  text-decoration: underline;
}

.markdown-body :deep(hr) {
  margin: 0.8em 0;
  border: 0;
  border-top: 1px solid var(--color-border-default, #e5e7eb);
}

.markdown-body :deep(img) {
  max-width: 100%;
  border-radius: 6px;
  margin: 0.4em 0;
}
</style>
