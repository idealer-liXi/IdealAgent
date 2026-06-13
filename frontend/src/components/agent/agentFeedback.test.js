import assert from 'node:assert/strict'
import test from 'node:test'

import {
  agentCreateFailureMessage,
  agentCreateSuccessMessage,
  agentSaveFailureMessage,
  agentSaveSuccessMessage,
  requireSuccessResult
} from './agentFeedback.js'

test('builds explicit agent creation success message', () => {
  assert.equal(agentCreateSuccessMessage({ agentName: '新闻查询', agentId: 'agent_news' }), 'Agent 创建成功：新闻查询（agent_news）')
  assert.equal(agentCreateSuccessMessage({ agentId: 'agent_news' }), 'Agent 创建成功：agent_news')
})

test('throws backend message when result code is not success', () => {
  assert.throws(
    () => requireSuccessResult({ code: '0001', message: '模型不存在或未启用', data: null }),
    /模型不存在或未启用/
  )
})

test('extracts useful agent creation failure message', () => {
  assert.equal(agentCreateFailureMessage({ response: { data: { message: 'Agent 类型不支持' } } }), 'Agent 创建失败：Agent 类型不支持')
  assert.equal(agentCreateFailureMessage(new Error('网络异常')), 'Agent 创建失败：网络异常')
  assert.equal(agentCreateFailureMessage({}), 'Agent 创建失败')
})

test('builds admin agent save feedback for create and update', () => {
  assert.equal(agentSaveSuccessMessage({ agentName: '邮件助手', agentId: 'agent_mail' }, false), 'Agent 创建成功：邮件助手（agent_mail）')
  assert.equal(agentSaveSuccessMessage({ agentName: '邮件助手', agentId: 'agent_mail' }, true), 'Agent 更新成功：邮件助手')
  assert.equal(agentSaveFailureMessage(new Error('模型不能为空'), false), 'Agent 创建失败：模型不能为空')
  assert.equal(agentSaveFailureMessage(new Error('Agent 不存在'), true), 'Agent 更新失败：Agent 不存在')
})
