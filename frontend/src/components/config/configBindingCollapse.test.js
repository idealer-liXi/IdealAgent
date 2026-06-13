import assert from 'node:assert/strict'
import test from 'node:test'

import { isBindingGroupCollapsed, toggleBindingGroup } from './configBindingCollapse.js'

test('binding groups are expanded by default', () => {
  assert.equal(isBindingGroupCollapsed(new Set(), 'client_a'), false)
})

test('toggle binding group collapse state', () => {
  const collapsed = toggleBindingGroup(new Set(), 'client_a')
  assert.equal(isBindingGroupCollapsed(collapsed, 'client_a'), true)
  assert.deepEqual([...toggleBindingGroup(collapsed, 'client_a')], [])
})
