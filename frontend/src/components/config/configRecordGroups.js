export function groupBindingRecordsByClient(records, clients) {
  const clientById = new Map((clients || []).map(client => [client.configId, client]))
  const groups = new Map()
  ;(records || []).forEach(record => {
    const clientId = record.content || String(record.ownerId || '')
    if (!groups.has(clientId)) {
      const client = clientById.get(clientId)
      groups.set(clientId, {
        clientId,
        clientName: client?.name || '未知 Client',
        records: []
      })
    }
    groups.get(clientId).records.push(record)
  })
  return [...groups.values()]
}
