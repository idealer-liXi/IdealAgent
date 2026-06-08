import assert from 'node:assert/strict'
import test from 'node:test'

import {
  activeParameterForAgent,
  buildProcessCards,
  canSendWork,
  isParameterEditable,
  userInputCard
} from './workUiState.js'

test('only the selected strategy parameter remains editable', () => {
  assert.equal(activeParameterForAgent('step'), 'retry')
  assert.equal(isParameterEditable('step', 'retry'), true)
  assert.equal(isParameterEditable('step', 'round'), false)
  assert.equal(isParameterEditable('step', 'pace'), false)

  assert.equal(activeParameterForAgent('loop'), 'round')
  assert.equal(isParameterEditable('loop', 'retry'), false)
  assert.equal(isParameterEditable('loop', 'round'), true)
  assert.equal(isParameterEditable('loop', 'pace'), false)

  assert.equal(activeParameterForAgent('react'), 'pace')
  assert.equal(isParameterEditable('react', 'retry'), false)
  assert.equal(isParameterEditable('react', 'round'), false)
  assert.equal(isParameterEditable('react', 'pace'), true)
})

test('work cannot be sent while a run is loading', () => {
  assert.equal(canSendWork({ agentId: 'agent_step', content: '查新闻', loading: false }), true)
  assert.equal(canSendWork({ agentId: 'agent_step', content: '查新闻', loading: true }), false)
  assert.equal(canSendWork({ agentId: 'agent_step', content: '   ', loading: false }), false)
  assert.equal(canSendWork({ agentId: '', content: '查新闻', loading: false }), false)
})

test('process cards keep user input before execution events', () => {
  const userCard = userInputCard('查询新闻并发邮件')
  assert.equal(userCard.clientType, 'user')
  assert.equal(userCard.sectionType, '用户输入')
  assert.equal(userCard.sectionContent, '查询新闻并发邮件')

  const cards = buildProcessCards([
    { role: 'user', content: '查询新闻并发邮件' },
    { role: 'event', content: '{"clientType":"inspector","sectionType":"inspector_task","sectionContent":"分析任务"}' },
    { role: 'assistant', content: '完成' }
  ], JSON.parse)

  assert.equal(cards.length, 2)
  assert.equal(cards[0].clientType, 'user')
  assert.equal(cards[0].sectionContent, '查询新闻并发邮件')
  assert.equal(cards[1].clientType, 'inspector')
})
