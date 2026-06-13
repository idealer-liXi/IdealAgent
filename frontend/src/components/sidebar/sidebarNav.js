const primaryItems = [
  { path: '/welcome', label: 'Welcome', description: '仪表盘' },
  { path: '/chat', label: 'Chat', description: '对话会话' },
  { path: '/work', label: 'Work', description: '智能体任务' },
  { path: '/workshop', label: 'Workshop', description: '创建智能体' }
]

const configGroup = {
  group: true,
  label: '配置管理',
  description: '管理入口',
  children: [
    { path: '/agents', label: 'Agent 管理', description: '编排管理', activePrefix: '/agents' },
    { path: '/config', label: 'Config', description: '配置管理' }
  ]
}

const toolGroup = {
  group: true,
  label: '工具展示',
  description: '展示入口',
  children: [
    { path: '/agent-display', label: 'Agent 展示', description: '只读观看' },
    { path: '/mcp-display', label: 'MCP 展示', description: '工具目录' }
  ]
}

export function buildSidebarItems(admin) {
  return admin ? [...primaryItems, configGroup, toolGroup] : [...primaryItems, toolGroup]
}

export function itemIsActive(item, path) {
  return item.path === path || Boolean(item.activePrefix && path.startsWith(`${item.activePrefix}/`))
}

export function groupIsActive(group, path) {
  return Boolean(group.children?.some(item => itemIsActive(item, path)))
}
