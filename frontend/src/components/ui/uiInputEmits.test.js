import assert from 'node:assert/strict'
import test from 'node:test'

import { uiInputEmits } from './uiInputEmits.js'

test('UiInput declares keydown event it emits', () => {
  assert.ok(uiInputEmits.includes('update:modelValue'))
  assert.ok(uiInputEmits.includes('keydown'))
})
