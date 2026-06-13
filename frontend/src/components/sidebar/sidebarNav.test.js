import assert from 'node:assert/strict'
import test from 'node:test'

import { buildSidebarItems } from './sidebarNav.js'

test('normal users see workshop and tool display group only', () => {
  const items = buildSidebarItems(false)

  assert.equal(items[3].path, '/workshop')
  assert.equal(items.some(item => item.label === '配置管理'), false)
  assert.deepEqual(items.find(item => item.label === '工具展示').children.map(item => item.label), ['Agent 展示', 'MCP 展示'])
})

test('admins see collapsible config management group', () => {
  const items = buildSidebarItems(true)
  const configGroup = items.find(item => item.label === '配置管理')

  assert.ok(configGroup)
  assert.equal(configGroup.group, true)
  assert.deepEqual(configGroup.children.map(item => item.label), ['Agent 管理', 'Config'])
})
