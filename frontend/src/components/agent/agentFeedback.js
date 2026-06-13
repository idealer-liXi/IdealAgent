export function requireSuccessResult(result, fallback = 'Agent 创建失败') {
  if (!result || result.code !== '0000') {
    throw new Error(result?.message || fallback)
  }
  return result.data
}

export function agentCreateSuccessMessage(agent) {
  const id = agent?.agentId || ''
  const name = agent?.agentName || id
  if (name && id && name !== id) {
    return `Agent 创建成功：${name}（${id}）`
  }
  return `Agent 创建成功：${name || '已创建'}`
}

export function agentSaveSuccessMessage(agent, editing) {
  if (!editing) {
    return agentCreateSuccessMessage(agent)
  }
  return `Agent 更新成功：${agent?.agentName || agent?.agentId || '已更新'}`
}

export function agentCreateFailureMessage(error) {
  const message = error?.response?.data?.message || error?.message || ''
  return message ? `Agent 创建失败：${message}` : 'Agent 创建失败'
}

export function agentSaveFailureMessage(error, editing) {
  if (!editing) {
    return agentCreateFailureMessage(error)
  }
  const message = error?.response?.data?.message || error?.message || ''
  return message ? `Agent 更新失败：${message}` : 'Agent 更新失败'
}
