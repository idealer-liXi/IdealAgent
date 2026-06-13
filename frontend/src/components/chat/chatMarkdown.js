import hljs from 'highlight.js'

export function renderCodeBlock({ text = '', lang = '' } = {}) {
  const code = typeof text === 'string' ? text : String(text ?? '')
  const language = hljs.getLanguage(lang) ? lang : 'plaintext'
  const highlighted = hljs.highlight(code, { language, ignoreIllegals: true }).value
  return `<pre class="hljs"><code class="hljs language-${language}">${highlighted}</code></pre>`
}

export function escapeHtml(text = '') {
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}
