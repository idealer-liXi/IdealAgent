const strategyParameters = {
  step: 'retry',
  loop: 'round',
  react: 'pace'
}

export function activeParameterForAgent(agentType) {
  return strategyParameters[String(agentType || '').toLowerCase()] || ''
}

export function isParameterEditable(agentType, parameter) {
  return activeParameterForAgent(agentType) === parameter
}

export function canSendWork({ agentId, content, loading }) {
  return Boolean(agentId && String(content || '').trim() && !loading)
}

export function userInputCard(content) {
  return {
    clientType: 'user',
    sectionType: '用户输入',
    sectionContent: content || ''
  }
}

export function buildProcessCards(history, parseEvent) {
  return history
    .map(item => {
      if (item.role === 'user') {
        return userInputCard(item.content)
      }
      if (item.role === 'event') {
        return parseEvent(item.content)
      }
      return null
    })
    .filter(Boolean)
}
