export function isBindingGroupCollapsed(collapsedGroups, clientId) {
  return collapsedGroups.has(clientId)
}

export function toggleBindingGroup(collapsedGroups, clientId) {
  const next = new Set(collapsedGroups)
  if (next.has(clientId)) {
    next.delete(clientId)
  } else {
    next.add(clientId)
  }
  return next
}
