import assert from 'node:assert/strict'
import test from 'node:test'

import { bindingDependenciesForTarget, deleteTargetConfirmMessage } from './configDeletionPlan.js'

test('finds binding dependencies for an mcp target', () => {
  const bindings = [
    { configId: 'config_client_a_mcp_wecom', configType: 'mcp', refId: 'mcp_wecom', content: 'client_a' },
    { configId: 'config_client_b_mcp_wecom', secret: 'mcp', refId: 'mcp_wecom', content: 'client_b' },
    { configId: 'config_client_a_prompt', configType: 'prompt', refId: 'prompt_system', content: 'client_a' },
    { configId: 'config_client_a_mcp_other', configType: 'mcp', refId: 'mcp_other', content: 'client_a' }
  ]

  assert.deepEqual(
    bindingDependenciesForTarget('mcp', { configId: 'mcp_wecom' }, bindings).map(record => record.configId),
    ['config_client_a_mcp_wecom', 'config_client_b_mcp_wecom']
  )
})

test('builds delete confirmation message with dependent bindings', () => {
  assert.equal(
    deleteTargetConfirmMessage('mcp', { configId: 'mcp_wecom' }, [{ configId: 'binding_1' }, { configId: 'binding_2' }]),
    'MCP mcp_wecom 仍被 2 条 Binding 引用。确认先删除这些 Binding，再删除 MCP？'
  )
})
