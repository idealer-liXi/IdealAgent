const targetLabels = {
  prompt: 'Prompt',
  advisor: 'Advisor',
  mcp: 'MCP'
}

export function bindingDependenciesForTarget(kind, record, bindings = []) {
  const targetId = record?.configId
  if (!['prompt', 'advisor', 'mcp'].includes(kind) || !targetId) {
    return []
  }
  return bindings.filter(binding => bindingType(binding) === kind && binding.refId === targetId)
}

export function deleteTargetConfirmMessage(kind, record, dependencies = []) {
  const label = targetLabels[kind] || '配置'
  if (!dependencies.length) {
    return `确认删除 ${record.configId}？`
  }
  return `${label} ${record.configId} 仍被 ${dependencies.length} 条 Binding 引用。确认先删除这些 Binding，再删除 ${label}？`
}

function bindingType(binding) {
  return String(binding?.configType || binding?.secret || '').toLowerCase()
}
