import assert from 'node:assert/strict'
import test from 'node:test'

import { groupBindingRecordsByClient } from './configRecordGroups.js'

test('groups binding records by client id and includes client name', () => {
  const groups = groupBindingRecordsByClient([
    { configId: 'config_client_a_prompt_prompt_a', content: 'client_a', configType: 'prompt', refId: 'prompt_a', status: 1 },
    { configId: 'config_client_b_mcp_mcp_a', content: 'client_b', configType: 'mcp', refId: 'mcp_a', status: 1 },
    { configId: 'config_client_a_advisor_advisor_a', content: 'client_a', configType: 'advisor', refId: 'advisor_a', status: 1 }
  ], [
    { configId: 'client_a', name: 'Planner Client' },
    { configId: 'client_b', name: 'Runner Client' }
  ])

  assert.equal(groups.length, 2)
  assert.equal(groups[0].clientId, 'client_a')
  assert.equal(groups[0].clientName, 'Planner Client')
  assert.deepEqual(groups[0].records.map(record => record.configType), ['prompt', 'advisor'])
  assert.equal(groups[1].clientId, 'client_b')
  assert.equal(groups[1].clientName, 'Runner Client')
})

test('falls back to unknown client label when client config is missing', () => {
  const groups = groupBindingRecordsByClient([
    { configId: 'config_missing_prompt_prompt_a', content: 'client_missing', configType: 'prompt', refId: 'prompt_a', status: 1 }
  ], [])

  assert.equal(groups[0].clientId, 'client_missing')
  assert.equal(groups[0].clientName, '未知 Client')
})
