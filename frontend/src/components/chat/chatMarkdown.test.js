import assert from 'node:assert/strict'
import test from 'node:test'

import { renderCodeBlock } from './chatMarkdown.js'

test('renders code block with undefined text without crashing', () => {
  const html = renderCodeBlock({ text: undefined, lang: 'js' })

  assert.match(html, /<pre class="hljs">/)
  assert.match(html, /language-javascript|language-js|language-plaintext/)
})

test('renders unknown language as plaintext', () => {
  const html = renderCodeBlock({ text: 'hello', lang: 'not-a-language' })

  assert.match(html, /language-plaintext/)
  assert.match(html, /hello/)
})
