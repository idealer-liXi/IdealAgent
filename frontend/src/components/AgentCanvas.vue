<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { VueFlow, Handle, MarkerType, Position, useVueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import { MiniMap } from '@vue-flow/minimap'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/controls/dist/style.css'
import '@vue-flow/minimap/dist/style.css'
import request from '../request/request'
import Sidebar from './Sidebar.vue'
import Footer from './Footer.vue'
import UiButton from './ui/UiButton.vue'

const route = useRoute()
const router = useRouter()
const { fitView } = useVueFlow()

const roleMap = {
  step: ['inspector', 'planner', 'runner', 'replier'],
  loop: ['analyzer', 'performer', 'supervisor', 'summarizer'],
  react: ['observer', 'reasoner', 'actor', 'evaluator']
}
const nodeTypes = ['client', 'model', 'api', 'prompt', 'advisor', 'mcp']

const agentId = computed(() => String(route.query.agentId || ''))
const graph = ref(null)
const nodes = ref([])
const edges = ref([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const createPickerVisible = ref(false)
const modalVisible = ref(false)
const modalType = ref('')
const editingNode = ref(null)
const connectionSourceNodeId = ref('')
const pending = reactive({ draftId: '', sourceNodeId: '' })
const lastClick = reactive({ id: '', ts: 0 })
const form = reactive({})
const flowForm = reactive({ originAgentId: '', originClientId: '', clientRole: '', userPrompt: '', flowSeq: 1 })
const newConfig = reactive({ prompt: '', advisor: '', mcp: '' })
const ragTags = ref([])
const ragAdvisorForm = reactive({ ragTag: '', topK: 4 })
const contextMenu = reactive({ visible: false, x: 0, y: 0, nodeId: '' })
const pendingChanges = reactive({ nodes: [], relations: [], deletedRelations: [] })

const agent = computed(() => graph.value?.agent || null)
const roleOrder = computed(() => roleMap[agent.value?.agentType || 'step'] || roleMap.step)
const clients = computed(() => new Map((graph.value?.clients || []).map(item => [item.configId, item])))
const models = computed(() => new Map((graph.value?.models || []).map(item => [item.configId, item])))
const apis = computed(() => new Map((graph.value?.apis || []).map(item => [item.configId, item])))
const contextNode = computed(() => nodes.value.find(item => item.id === contextMenu.nodeId) || null)
const hasPendingChanges = computed(() => pendingChanges.nodes.length > 0 || pendingChanges.relations.length > 0 || pendingChanges.deletedRelations.length > 0)
const availableNodeTypes = computed(() => {
  if (!connectionSourceNodeId.value) return nodeTypes
  const source = nodes.value.find(item => item.id === connectionSourceNodeId.value)
  if (!source) return []
  return nodeTypes.filter(type => isAllowed(source.type, type))
})
const configsByClient = computed(() => {
  const map = new Map()
  ;(graph.value?.configs || []).forEach(item => {
    const clientId = item.content || item.ownerId
    if (!clientId) return
    if (!map.has(clientId)) map.set(clientId, [])
    map.get(clientId).push(item)
  })
  return map
})

onMounted(async () => {
  await Promise.all([fetchData(), loadRagTags()])
  window.addEventListener('keydown', handleShortcut)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleShortcut)
})

watch(ragAdvisorForm, () => {
  if (modalType.value === 'advisor' && normalizeAdvisorType(form.type) === 'Rag') {
    applyRagAdvisorContent()
  }
})

async function loadRagTags() {
  try {
    const response = await request.get('/ai/rag/tags')
    ragTags.value = response.data.data || []
  } catch (e) {
    ragTags.value = []
  }
}

async function fetchData() {
  if (!agentId.value) {
    error.value = '缺少 agentId'
    return
  }
  loading.value = true
  error.value = ''
  try {
    const response = await request.get(`/ai/admin/canvas/${agentId.value}`)
    graph.value = response.data.data
    buildGraph()
    clearPendingChanges()
    await nextTick()
    fitView({ padding: 0.2 })
  } catch (e) {
    error.value = e.response?.data?.message || 'Canvas 加载失败'
  } finally {
    loading.value = false
  }
}

function buildGraph() {
  const builtNodes = []
  const builtEdges = []
  const builtNodeIds = new Set()
  const builtEdgeIds = new Set()
  if (!agent.value) {
    nodes.value = []
    edges.value = []
    return
  }

  const flows = [...(graph.value.flows || [])].sort((a, b) => (a.flowSeq || 0) - (b.flowSeq || 0))
  const agentNodeId = `agent:${agent.value.agentId}`
  pushNode(builtNodes, builtNodeIds, { id: agentNodeId, type: 'agent', position: { x: 0, y: -220 }, data: { id: agent.value.agentId, title: agent.value.agentName || agent.value.agentId, subtitle: agent.value.agentType, desc: agent.value.agentDesc } })

  const flowBySeq = new Map(flows.map(flow => [flow.flowSeq, flow]))
  roleOrder.value.forEach((role, index) => {
    const flow = flowBySeq.get(index + 1)
    const x = index * 300
    if (!flow) {
      const slotNodeId = `slot:${role}`
      pushNode(builtNodes, builtNodeIds, { id: slotNodeId, type: 'slot', position: { x, y: 0 }, data: { id: role, title: role, subtitle: `Seq ${index + 1}`, desc: '待绑定 Work Client' } })
      pushEdge(builtEdges, builtEdgeIds, edge(index === 0 ? agentNodeId : previousStrategyNode(roleOrder.value, flowBySeq, index), slotNodeId))
      return
    }
    const client = clients.value.get(flow.clientId) || { configId: flow.clientId, name: flow.clientId, refId: '', content: flow.clientRole }
    const clientNodeId = `client:${flow.clientId}`
    pushNode(builtNodes, builtNodeIds, { id: clientNodeId, type: 'client', position: { x, y: 0 }, data: { id: flow.clientId, title: client.name || flow.clientId, subtitle: flow.clientRole, desc: flow.userPrompt, raw: client, flow } })
    pushEdge(builtEdges, builtEdgeIds, edge(index === 0 ? agentNodeId : previousStrategyNode(roleOrder.value, flowBySeq, index), clientNodeId, flowRelation(flow, index, flowBySeq)))

    const model = client.refId ? models.value.get(client.refId) : null
    if (model) {
      const modelNodeId = `model:${model.configId}`
      pushNode(builtNodes, builtNodeIds, { id: modelNodeId, type: 'model', position: { x, y: 220 }, data: { id: model.configId, title: model.name || model.configId, subtitle: model.type, desc: model.refId, raw: model } })
      pushEdge(builtEdges, builtEdgeIds, edge(clientNodeId, modelNodeId, relation('client', 'model', flow.clientId, model.configId, 'model')))
      const api = model.refId ? apis.value.get(model.refId) : null
      if (api) {
        const apiNodeId = `api:${api.configId}`
        pushNode(builtNodes, builtNodeIds, { id: apiNodeId, type: 'api', position: { x, y: 420 }, data: { id: api.configId, title: api.name || api.configId, subtitle: api.type, desc: api.content, raw: api } })
        pushEdge(builtEdges, builtEdgeIds, edge(modelNodeId, apiNodeId, relation('model', 'api', model.configId, api.configId, 'api')))
      }
    }

    const depX = x + 180
    ;(configsByClient.value.get(flow.clientId) || []).forEach((config, configIndex) => {
      const type = config.configType || config.secret
      const targetId = config.refId
      if (!targetId) return
      const target = collection(type).get(targetId)
      const nodeId = `${type}:${targetId}`
      pushNode(builtNodes, builtNodeIds, { id: nodeId, type, position: { x: depX + configIndex * 180, y: 220 }, data: { id: targetId, title: target?.name || targetId, subtitle: target?.type || 'missing', desc: target ? target.content : '绑定目标配置不存在', raw: target || {}, missing: !target } })
      pushEdge(builtEdges, builtEdgeIds, edge(clientNodeId, nodeId, relation('client', type, flow.clientId, targetId, type)))
    })
  })

  nodes.value = builtNodes
  edges.value = builtEdges
}

function pushNode(targetNodes, nodeIds, node) {
  if (nodeIds.has(node.id)) return
  nodeIds.add(node.id)
  targetNodes.push(node)
}

function pushEdge(targetEdges, edgeIds, nextEdge) {
  if (edgeIds.has(nextEdge.id)) return
  edgeIds.add(nextEdge.id)
  targetEdges.push(nextEdge)
}

function collection(type) {
  const records = graph.value?.[`${type}s`] || []
  return new Map(records.map(item => [item.configId, item]))
}

function edge(source, target, data = null) {
  return { id: `edge:${source}->${target}`, source, target, data, markerEnd: MarkerType.ArrowClosed, style: { stroke: '#94a3b8', strokeWidth: 1.5 } }
}

function relation(sourceType, targetType, sourceId, targetId, configType = null) {
  return { sourceType, targetType, sourceId, targetId, agentId: agentId.value, configType }
}

function flowRelation(flow, index, flowBySeq) {
  const previousFlow = flowBySeq.get(index)
  return {
    sourceType: index === 0 ? 'agent' : 'client',
    targetType: 'client',
    sourceId: index === 0 ? agentId.value : previousFlow?.clientId || agentId.value,
    targetId: flow.clientId,
    agentId: agentId.value,
    clientRole: flow.clientRole,
    userPrompt: flow.userPrompt,
    flowSeq: flow.flowSeq
  }
}

function previousStrategyNode(roles, flowBySeq, index) {
  const previousFlow = flowBySeq.get(index)
  return previousFlow ? `client:${previousFlow.clientId}` : `slot:${roles[index - 1]}`
}

function createDraftNode(type) {
  createPickerVisible.value = false
  const sourceNode = nodes.value.find(item => item.id === connectionSourceNodeId.value)
  const draftId = `draft:${type}:${Date.now()}`
  const position = sourceNode ? { x: sourceNode.position.x + 260, y: sourceNode.position.y + 160 } : { x: 120, y: 260 }
  const draftNode = { id: draftId, type, position, data: { id: 'NEW', title: `New ${type}`, draft: true } }
  nodes.value = [...nodes.value, draftNode]
  if (sourceNode) {
    pending.draftId = draftId
    pending.sourceNodeId = sourceNode.id
    connectionSourceNodeId.value = ''
    openNodeModal(draftNode)
    return
  }
  openNodeModal(draftNode)
}

function cancelCreatePicker() {
  createPickerVisible.value = false
  connectionSourceNodeId.value = ''
}

function handleNodeClick(event) {
  closeContextMenu()
  const node = event?.node || event
  if (!node?.id) return
  const now = Date.now()
  if (lastClick.id === node.id && now - lastClick.ts < 320) {
    lastClick.id = ''
    openNodeModal(node)
    return
  }
  lastClick.id = node.id
  lastClick.ts = now
}

function openNodeContextMenu(event, nodeId) {
  event.preventDefault()
  event.stopPropagation()
  contextMenu.visible = true
  contextMenu.x = event.clientX
  contextMenu.y = event.clientY
  contextMenu.nodeId = nodeId
}

function openPaneContextMenu(event) {
  event.preventDefault()
  contextMenu.visible = true
  contextMenu.x = event.clientX
  contextMenu.y = event.clientY
  contextMenu.nodeId = ''
}

function closeContextMenu() {
  contextMenu.visible = false
}

function canConnectFrom(node) {
  return Boolean(node && nodeTypes.some(type => isAllowed(node.type, type)))
}

function showCreatePicker(sourceNodeId = '') {
  connectionSourceNodeId.value = sourceNodeId
  createPickerVisible.value = true
  closeContextMenu()
}

function deleteContextNode() {
  const node = contextNode.value
  if (!node) return
  closeContextMenu()
  if (node.data?.draft) {
    nodes.value = nodes.value.filter(item => item.id !== node.id)
    return
  }
  if (['agent', 'slot'].includes(node.type)) {
    error.value = '该节点不能删除'
    return
  }
  const confirmed = typeof window === 'undefined' || window.confirm(`删除当前画布中的 ${node.type} 节点 ${node.data?.id}？`)
  if (!confirmed) return
  error.value = ''
  deleteLocalNode(node)
}

function deleteLocalNode(node) {
  const incidentEdges = edges.value.filter(item => item.source === node.id || item.target === node.id)
  incidentEdges.forEach(item => addPendingDeletedRelation(item.data))
  removePendingNode(node.data?.id)
  nodes.value = nodes.value.filter(item => item.id !== node.id)
  edges.value = edges.value.filter(item => item.source !== node.id && item.target !== node.id)
}

function handleConnect({ source, target }) {
  const sourceNode = nodes.value.find(item => item.id === source)
  const targetNode = nodes.value.find(item => item.id === target)
  if (!sourceNode || !targetNode) return
  if (!targetNode.data?.draft) {
    saveExistingRelation(sourceNode, targetNode)
    return
  }
  if (!isAllowed(sourceNode.type, targetNode.type)) {
    error.value = `连接规则不允许：${sourceNode.type} -> ${targetNode.type}`
    return
  }
  pending.draftId = targetNode.id
  pending.sourceNodeId = sourceNode.id
  openNodeModal(targetNode)
}

function saveExistingRelation(sourceNode, targetNode) {
  if (!isAllowed(sourceNode.type, targetNode.type)) {
    error.value = `连接规则不允许：${sourceNode.type} -> ${targetNode.type}`
    return
  }
  if (targetNode.type === 'client') {
    error.value = '请新增 Client 节点或双击 Client 编辑 Flow 配置'
    return
  }
  const payload = relationFromNodes(sourceNode, targetNode)
  if (!payload) return
  const validationError = sourceNode.type === 'client'
    ? invalidChatClientRuntimeBinding(sourceNode.data?.raw, targetNode.type, payload.targetId)
    : ''
  if (validationError) {
    error.value = validationError
    return
  }
  error.value = ''
  addPendingRelation(payload)
  addLocalEdge(sourceNode.id, targetNode.id, payload)
}

function relationFromNodes(sourceNode, targetNode) {
  const sourceId = sourceNode.data?.id
  const targetId = targetNode.data?.id
  if (!sourceId || !targetId) {
    error.value = '连接节点缺少 ID'
    return null
  }
  return relation(sourceNode.type, targetNode.type, sourceId, targetId, targetNode.type)
}

function isAllowed(sourceType, targetType) {
  return new Set(['agent->client', 'client->client', 'client->model', 'model->api', 'client->prompt', 'client->advisor', 'client->mcp']).has(`${sourceType}->${targetType}`)
}

function openNodeModal(node) {
  if (node.type === 'agent') return
  if (node.data?.missing) {
    error.value = `${node.type} 配置 ${node.data.id} 不存在，请删除这条绑定或先创建对应配置`
    return
  }
  modalType.value = node.type
  editingNode.value = node
  Object.keys(form).forEach(key => delete form[key])
  Object.assign(form, formFromNode(node))
  if (node.type === 'advisor') syncRagAdvisorForm(form.content)
  resetFlowForm(node)
  newConfig.prompt = ''
  newConfig.advisor = ''
  newConfig.mcp = ''
  modalVisible.value = true
}

function formFromNode(node) {
  const raw = node.data?.raw || {}
  const id = node.data?.draft ? '' : node.data?.id
  if (node.type === 'client') return { configId: id || '', name: raw.name || '', type: raw.type || 'work', content: raw.content || node.data?.subtitle || '', refId: raw.refId || '', secret: raw.secret || '', status: raw.status ?? 1 }
  if (node.type === 'model') return { configId: id || '', name: raw.name || '', type: raw.type || 'model', refId: raw.refId || '', status: raw.status ?? 1 }
  if (node.type === 'api') return { configId: id || '', name: raw.name || '', type: raw.type || 'openai', content: raw.content || '', secret: raw.secret || '', status: raw.status ?? 1 }
  if (node.type === 'prompt') return { configId: id || '', name: raw.name || '', type: raw.type || 'system', content: raw.content || '', status: raw.status ?? 1 }
  if (node.type === 'advisor') return { configId: id || '', name: raw.name || '', type: normalizeAdvisorType(raw.type), content: raw.content || '', status: raw.status ?? 1 }
  return { configId: id || '', name: raw.name || '', type: raw.type || 'stdio', content: raw.content || '', secret: raw.secret || '', status: raw.status ?? 1 }
}

function normalizeAdvisorType(type) {
  if ((type || '').toLowerCase() === 'rag') return 'Rag'
  if ((type || '').toLowerCase() === 'memory') return 'Memory'
  return type || 'Memory'
}

function onAdvisorTypeChange() {
  if (form.type === 'Memory') {
    form.content = '{"maxMessages":20}'
    return
  }
  if (form.type === 'Rag') {
    syncRagAdvisorForm(form.content)
    applyRagAdvisorContent()
  }
}

function syncRagAdvisorForm(content) {
  const parsed = parseJsonObject(content)
  ragAdvisorForm.ragTag = knowledgeTag(parsed.filterExpression || '')
  ragAdvisorForm.topK = positiveNumberOrDefault(parsed.topK, 4)
}

function applyRagAdvisorContent() {
  const ragTag = trimmed(ragAdvisorForm.ragTag)
  const topK = positiveNumberOrDefault(ragAdvisorForm.topK, 4)
  form.content = JSON.stringify({ topK, filterExpression: ragTag ? `knowledge == '${ragTag}'` : '' })
}

function parseJsonObject(value) {
  try {
    const parsed = JSON.parse(value || '{}')
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  } catch (e) {
    return {}
  }
}

function knowledgeTag(filterExpression) {
  const match = String(filterExpression || '').match(/knowledge\s*==\s*['"]([^'"]+)['"]/i)
  return match ? match[1] : ''
}

function positiveNumberOrDefault(value, fallback) {
  const numberValue = Number(value)
  return Number.isFinite(numberValue) && numberValue > 0 ? Math.floor(numberValue) : fallback
}

function trimmed(value) {
  return typeof value === 'string' ? value.trim() : value
}

function resetFlowForm(node) {
  const flow = node.type === 'client' ? node.data?.flow : null
  const nextSeq = Math.min((graph.value?.flows || []).length + 1, roleOrder.value.length)
  const seq = flow?.flowSeq || nextSeq
  flowForm.originAgentId = flow?.agentId || ''
  flowForm.originClientId = flow?.clientId || ''
  flowForm.flowSeq = seq
  flowForm.clientRole = flow?.clientRole || roleOrder.value[seq - 1] || ''
  flowForm.userPrompt = flow?.userPrompt || ''
}

async function saveModal() {
  if (!form.configId) {
    error.value = `${modalType.value} ID 不能为空`
    return
  }
  if (modalType.value === 'client' && !validateFlowForm()) return
  if (editingNode.value?.data?.draft) {
    saveDraftNodeLocally()
    return
  }
  saving.value = true
  error.value = ''
  try {
    await saveNodeConfig()
    if (modalType.value === 'client') await saveClientFlow()
    if (pending.draftId && ['model', 'api', 'prompt', 'advisor', 'mcp'].includes(modalType.value)) await savePendingRelation()
    modalVisible.value = false
    clearPending()
    await fetchData()
  } catch (e) {
    error.value = e.response?.data?.message || '保存失败'
  } finally {
    saving.value = false
  }
}

function saveDraftNodeLocally() {
  const draftId = editingNode.value?.id
  const nodeIndex = nodes.value.findIndex(item => item.id === draftId)
  if (nodeIndex < 0) return
  const payload = { ...form }
  const sourceNode = nodes.value.find(item => item.id === pending.sourceNodeId) || nodes.value.find(item => item.id === `agent:${agentId.value}`)
  if (pending.draftId && ['model', 'api', 'prompt', 'advisor', 'mcp'].includes(modalType.value) && sourceNode) {
    const validationError = invalidChatClientRuntimeBinding(sourceNode.data?.raw, modalType.value, form.configId)
    if (validationError) {
      error.value = validationError
      return
    }
  }
  const nodeId = `${modalType.value}:${form.configId}`
  const current = nodes.value[nodeIndex]
  const nextNode = {
    ...current,
    id: nodeId,
    data: {
      ...current.data,
      id: form.configId,
      title: form.name || form.configId,
      subtitle: form.type,
      desc: form.content || form.refId,
      raw: payload,
      draft: false
    }
  }
  nodes.value = nodes.value.map(item => item.id === draftId ? nextNode : item)
  edges.value = edges.value.map(item => ({
    ...item,
    source: item.source === draftId ? nodeId : item.source,
    target: item.target === draftId ? nodeId : item.target,
    id: `edge:${item.source === draftId ? nodeId : item.source}->${item.target === draftId ? nodeId : item.target}`
  }))
  upsertPendingNode({ nodeType: modalType.value, id: form.configId, agentId: agentId.value, payload })
  if (modalType.value === 'client') {
    const sourceType = sourceNode?.type === 'client' ? 'client' : 'agent'
    const sourceId = sourceNode?.data?.id || agentId.value
    const payloadRelation = { sourceType, targetType: 'client', sourceId, targetId: form.configId, agentId: agentId.value, clientRole: flowForm.clientRole, userPrompt: flowForm.userPrompt, flowSeq: Number(flowForm.flowSeq) }
    addPendingRelation(payloadRelation)
    if (sourceNode) addLocalEdge(sourceNode.id, nodeId, payloadRelation)
  }
  if (pending.draftId && ['model', 'api', 'prompt', 'advisor', 'mcp'].includes(modalType.value)) {
    if (sourceNode) {
      const payloadRelation = { sourceType: sourceNode.type, targetType: modalType.value, sourceId: sourceNode.data.id, targetId: form.configId, agentId: agentId.value, configType: modalType.value }
      addPendingRelation(payloadRelation)
      addLocalEdge(sourceNode.id, nodeId, payloadRelation)
    }
  }
  modalVisible.value = false
  clearPending()
}

async function saveNodeConfig() {
  const payload = { ...form }
  if (editingNode.value?.data?.draft) await request.post(`/ai/config/${modalType.value}`, payload)
  else await request.put(`/ai/config/${modalType.value}/${form.configId}`, payload)
}

function validateFlowForm() {
  if (!flowForm.userPrompt) {
    error.value = 'Flow Prompt 不能为空'
    return false
  }
  const expectedRole = roleOrder.value[Number(flowForm.flowSeq) - 1]
  if (!expectedRole || expectedRole !== flowForm.clientRole) {
    error.value = 'Flow 顺序必须匹配当前 Agent 策略角色'
    return false
  }
  return true
}

async function saveClientFlow() {
  const body = { originAgentId: flowForm.originAgentId, originClientId: flowForm.originClientId, agentId: agentId.value, clientId: form.configId, clientRole: flowForm.clientRole, userPrompt: flowForm.userPrompt, flowSeq: Number(flowForm.flowSeq) }
  if (flowForm.originAgentId && flowForm.originClientId) await request.put('/ai/admin/flows', body)
  else await request.post('/ai/admin/flows', body)
}

async function savePendingRelation() {
  const sourceNode = nodes.value.find(item => item.id === pending.sourceNodeId)
  if (!sourceNode) return
  const validationError = invalidChatClientRuntimeBinding(sourceNode.data?.raw, modalType.value, form.configId)
  if (validationError) {
    error.value = validationError
    return
  }
  await saveConfigRelation({ sourceType: sourceNode.type, targetType: modalType.value, sourceId: sourceNode.data.id, targetId: form.configId })
}

async function addClientConfig(type) {
  const refId = newConfig[type]
  if (!refId || !editingNode.value?.data?.id) return
  const validationError = invalidChatClientRuntimeBinding(editingNode.value.data?.raw, type, refId)
  if (validationError) {
    error.value = validationError
    return
  }
  saving.value = true
  error.value = ''
  try {
    await saveConfigRelation({ sourceType: 'client', targetType: type, sourceId: editingNode.value.data.id, targetId: refId })
    newConfig[type] = ''
    modalVisible.value = false
    await fetchData()
  } catch (e) {
    error.value = e.response?.data?.message || '依赖保存失败'
  } finally {
    saving.value = false
  }
}

async function deleteClientConfig(config) {
  if (!config?.refId) return
  saving.value = true
  error.value = ''
  try {
    await deleteConfigRelation({ sourceType: 'client', targetType: config.configType || config.secret, sourceId: config.content || editingNode.value?.data?.id, targetId: config.refId })
    modalVisible.value = false
    await fetchData()
  } catch (e) {
    error.value = e.response?.data?.message || '依赖删除失败'
  } finally {
    saving.value = false
  }
}

async function deleteFlow() {
  if (!editingNode.value?.data?.id || !flowForm.originAgentId || !flowForm.originClientId) return
  saving.value = true
  error.value = ''
  try {
    await request.delete('/ai/admin/flows', { params: { agentId: agentId.value, clientId: editingNode.value.data.id } })
    modalVisible.value = false
    await fetchData()
  } catch (e) {
    error.value = e.response?.data?.message || 'Flow 删除失败'
  } finally {
    saving.value = false
  }
}

function handleEdgeClick(event, clickedEdge) {
  const selectedEdge = clickedEdge || event?.edge || event
  if (!selectedEdge?.data) return
  const confirmed = typeof window === 'undefined' || window.confirm('删除这条绑定关系？')
  if (!confirmed) return
  error.value = ''
  addPendingDeletedRelation(selectedEdge.data)
  removePendingRelation(selectedEdge.data)
  edges.value = edges.value.filter(item => item.id !== selectedEdge.id)
}

async function saveConfigRelation(payload) {
  const edgeKey = `${payload.sourceType}->${payload.targetType}`
  if (edgeKey === 'client->model') {
    await updateConfigRef('client', payload.sourceId, payload.targetId)
    return
  }
  if (edgeKey === 'model->api') {
    await updateConfigRef('model', payload.sourceId, payload.targetId)
    return
  }
  if (payload.sourceType === 'client') {
    const body = bindingPayload(payload.sourceId, payload.targetType, payload.targetId)
    const existing = collection('config').get(body.configId)
    if (existing) await request.put(`/ai/config/config/${body.configId}`, body)
    else await request.post('/ai/config/config', body)
  }
}

async function deleteConfigRelation(payload) {
  const edgeKey = `${payload.sourceType}->${payload.targetType}`
  if (edgeKey === 'agent->client' || edgeKey === 'client->client') {
    await request.delete('/ai/admin/flows', { params: { agentId: agentId.value, clientId: payload.targetId } })
    return
  }
  if (edgeKey === 'client->model') {
    await updateConfigRef('client', payload.sourceId, '')
    return
  }
  if (edgeKey === 'model->api') {
    await updateConfigRef('model', payload.sourceId, '')
    return
  }
  if (payload.sourceType === 'client') {
    await request.delete(`/ai/config/config/${bindingId(payload.sourceId, payload.targetType, payload.targetId)}`)
  }
}

function addLocalEdge(source, target, data) {
  const nextEdge = edge(source, target, data)
  if (edges.value.some(item => item.id === nextEdge.id)) return
  edges.value = [...edges.value, nextEdge]
}

function upsertPendingNode(node) {
  const index = pendingChanges.nodes.findIndex(item => item.nodeType === node.nodeType && item.id === node.id)
  if (index >= 0) pendingChanges.nodes.splice(index, 1, node)
  else pendingChanges.nodes.push(node)
}

function addPendingRelation(payload) {
  if (!payload) return
  removePendingDeletedRelation(payload)
  const index = pendingChanges.relations.findIndex(item => relationKey(item) === relationKey(payload))
  if (index >= 0) pendingChanges.relations.splice(index, 1, payload)
  else pendingChanges.relations.push(payload)
}

function removePendingRelation(payload) {
  if (!payload) return
  const key = relationKey(payload)
  const index = pendingChanges.relations.findIndex(item => relationKey(item) === key)
  if (index >= 0) pendingChanges.relations.splice(index, 1)
}

function addPendingDeletedRelation(payload) {
  if (!payload) return
  removePendingRelation(payload)
  if (pendingChanges.deletedRelations.some(item => relationKey(item) === relationKey(payload))) return
  pendingChanges.deletedRelations.push(payload)
}

function removePendingDeletedRelation(payload) {
  if (!payload) return
  const key = relationKey(payload)
  const index = pendingChanges.deletedRelations.findIndex(item => relationKey(item) === key)
  if (index >= 0) pendingChanges.deletedRelations.splice(index, 1)
}

function removePendingNode(configId) {
  if (!configId) return
  const index = pendingChanges.nodes.findIndex(item => item.id === configId)
  if (index >= 0) pendingChanges.nodes.splice(index, 1)
}

function relationKey(payload) {
  return `${payload.sourceType}->${payload.targetType}:${payload.sourceId}->${payload.targetId}`
}

function clearPendingChanges() {
  pendingChanges.nodes.splice(0)
  pendingChanges.relations.splice(0)
  pendingChanges.deletedRelations.splice(0)
}

async function saveCanvasChanges() {
  if (!hasPendingChanges.value || saving.value) return
  saving.value = true
  error.value = ''
  try {
    await request.post(`/ai/admin/canvas/${agentId.value}/save`, {
      nodes: pendingChanges.nodes,
      relations: pendingChanges.relations,
      deletedRelations: pendingChanges.deletedRelations
    })
    await fetchData()
  } catch (e) {
    error.value = e.response?.data?.message || 'Canvas 保存失败，数据库已回滚，当前图修改已保留'
  } finally {
    saving.value = false
  }
}

function handleShortcut(event) {
  if ((event.ctrlKey || event.metaKey) && event.key?.toLowerCase() === 's') {
    event.preventDefault()
    saveCanvasChanges()
  }
}

async function updateConfigRef(kind, configId, refId) {
  const record = collection(kind).get(configId)
  if (!record) throw new Error('配置不存在')
  await request.put(`/ai/config/${kind}/${configId}`, configPayload(record, refId))
}

function configPayload(record, refId) {
  return {
    configId: record.configId,
    name: record.name || '',
    type: record.type || '',
    content: record.content || '',
    secret: record.secret || '',
    refId,
    status: record.status ?? 1,
    ownerId: record.ownerId ?? 0,
    ownerType: record.ownerType || null,
    configType: record.configType || null
  }
}

function bindingPayload(clientId, configType, refId) {
  return { configId: bindingId(clientId, configType, refId), name: '', type: null, content: clientId, secret: null, refId, status: 1, ownerId: 0, ownerType: 'client', configType }
}

function bindingId(clientId, configType, refId) {
  return `config_${clientId}_${configType}_${refId}`
}

function clientConfigs(type) {
  const clientId = editingNode.value?.data?.id
  return (configsByClient.value.get(clientId) || []).filter(item => (item.configType || item.secret) === type)
}

function invalidChatClientRuntimeBinding(client, type, refId) {
  if (client?.type !== 'chat') return ''
  if (type === 'mcp') return 'Chat Client 不允许绑定 MCP，请在 Chat 页面按请求选择 MCP 工具'
  if (type === 'advisor') {
    const advisor = collection('advisor').get(refId)
    if (String(advisor?.type || '').toLowerCase() === 'rag') {
      return 'Chat Client 不允许绑定 RAG Advisor，请在 Chat 页面按请求选择知识库'
    }
  }
  return ''
}

function fieldLabel(key) {
  return { configId: 'ID', name: '名称', type: '类型', content: '内容/角色/URL', secret: '密钥/模型名', refId: '关联 ID', status: '状态' }[key] || key
}

function clearPending() {
  pending.draftId = ''
  pending.sourceNodeId = ''
}

function cancelModal() {
  if (pending.draftId) nodes.value = nodes.value.filter(item => item.id !== pending.draftId)
  clearPending()
  modalVisible.value = false
}

function nodeClass(type, data) {
  return ['canvas-card', `canvas-${type}`, data?.draft ? 'canvas-draft' : '', data?.missing ? 'canvas-missing' : '']
}
</script>

<template>
  <div class="flex h-screen bg-surface text-text-primary">
    <Sidebar />
    <div class="flex min-w-0 flex-1 flex-col">
      <main class="flex min-h-0 flex-1 flex-col p-5">
        <section class="mb-4 flex items-center justify-between gap-3">
          <div>
            <p class="text-xs font-semibold uppercase tracking-widest text-accent">CONFIG CANVAS</p>
            <h1 class="mt-2 text-2xl font-bold">{{ agent?.agentName || agentId }}</h1>
            <p class="mt-1 text-sm text-text-secondary">{{ agent?.agentType }} 策略：Flow 槽位受策略约束，Client 依赖可在节点内配置。</p>
          </div>
          <div class="flex gap-2">
            <UiButton variant="secondary" @click="showCreatePicker()">新增节点</UiButton>
            <UiButton :loading="saving" :disabled="!hasPendingChanges" @click="saveCanvasChanges">保存 Canvas</UiButton>
            <UiButton variant="secondary" @click="router.push({ path: '/agents/flow', query: { agentId } })">返回 FLOW</UiButton>
            <UiButton variant="secondary" :loading="loading" @click="fetchData">刷新</UiButton>
          </div>
        </section>
        <p v-if="error" class="mb-4 rounded-card border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{{ error }}</p>
        <p v-if="hasPendingChanges" class="mb-4 rounded-card border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-700">有未保存的 Canvas 图修改，可点击保存或按 Ctrl + S。保存失败时数据库会回滚，当前图修改会保留。</p>
        <div class="min-h-0 flex-1 overflow-hidden rounded-card-lg border border-border-default bg-elevated shadow-card">
          <VueFlow v-model:nodes="nodes" v-model:edges="edges" class="h-full" :fit-view-on-init="true" :nodes-connectable="true" @connect="handleConnect" @node-click="handleNodeClick" @edge-click="handleEdgeClick" @pane-click="closeContextMenu" @contextmenu="openPaneContextMenu">
            <Background :gap="18" color="#e2e8f0" />
            <Controls />
            <MiniMap />
            <template v-for="type in ['agent','client','model','api','prompt','advisor','mcp','slot']" #[`node-${type}`]="{ id, data }" :key="type">
              <div :class="nodeClass(type, data)" @contextmenu="openNodeContextMenu($event, id)">
                <Handle v-if="!['agent','slot'].includes(type)" type="target" :position="Position.Top" />
                <Handle v-if="!['api','prompt','advisor','mcp','slot'].includes(type)" type="source" :position="Position.Bottom" />
                <div class="font-bold">{{ data.id }}</div>
                <div>{{ data.title }}</div>
                <p>{{ data.subtitle || data.desc }}</p>
              </div>
            </template>
          </VueFlow>
        </div>
      </main>
      <Footer />
    </div>

    <div v-if="createPickerVisible" class="fixed inset-0 z-50 grid place-items-center bg-black/30 p-4" @click="cancelCreatePicker">
      <div class="w-full max-w-lg rounded-card-lg bg-elevated p-5 shadow-card" @click.stop>
        <div class="mb-4 text-lg font-bold">新增节点</div>
        <div class="grid grid-cols-2 gap-3">
            <button v-for="type in availableNodeTypes" :key="type" class="rounded-card border border-border-default px-4 py-3 text-left font-semibold hover:border-accent" type="button" @click="createDraftNode(type)">{{ type }}</button>
            <div v-if="availableNodeTypes.length === 0" class="col-span-2 rounded-card border border-border-subtle bg-surface px-4 py-3 text-sm text-text-tertiary">当前节点没有可连接的目标类型</div>
          </div>
        </div>
      </div>

    <div v-if="contextMenu.visible" class="fixed inset-0 z-40" @click="closeContextMenu" @contextmenu.prevent="closeContextMenu">
      <div class="min-w-36 overflow-hidden rounded-card border border-border-default bg-elevated py-1 text-sm shadow-card" :style="{ position: 'fixed', left: `${contextMenu.x}px`, top: `${contextMenu.y}px` }" @click.stop @contextmenu.stop.prevent>
        <template v-if="contextNode">
          <button class="block w-full px-4 py-2 text-left hover:bg-surface" type="button" @click="deleteContextNode">删除节点</button>
          <button class="block w-full px-4 py-2 text-left hover:bg-surface disabled:cursor-not-allowed disabled:text-text-tertiary" type="button" :disabled="!canConnectFrom(contextNode)" @click="showCreatePicker(contextNode.id)">连接节点</button>
        </template>
        <button v-else class="block w-full px-4 py-2 text-left hover:bg-surface" type="button" @click="showCreatePicker()">增加节点</button>
      </div>
    </div>

    <div v-if="modalVisible" class="fixed inset-0 z-50 grid place-items-center bg-black/30 p-4" @click="cancelModal">
      <div class="max-h-[90vh] w-full max-w-3xl overflow-auto rounded-card-lg bg-elevated p-5 shadow-card" @click.stop>
        <div class="mb-4 flex items-center justify-between">
          <div class="text-lg font-bold">{{ editingNode?.data?.draft ? '新增' : '编辑' }} {{ modalType }}</div>
          <button class="text-text-tertiary" type="button" @click="cancelModal">关闭</button>
        </div>
        <div class="grid gap-3 sm:grid-cols-2">
          <label v-for="(_, key) in form" :key="key" class="text-sm">
            <span class="mb-1 block font-semibold">{{ fieldLabel(key) }}</span>
            <select v-if="modalType === 'advisor' && key === 'type'" v-model="form[key]" class="w-full rounded-card border border-border-default bg-surface px-3 py-2 outline-none focus:border-accent" @change="onAdvisorTypeChange">
              <option value="Memory">Memory</option>
              <option value="Rag">Rag</option>
            </select>
            <textarea v-else-if="key === 'content'" v-model="form[key]" rows="4" class="w-full rounded-card border border-border-default bg-surface px-3 py-2 outline-none focus:border-accent" />
            <input v-else-if="key !== 'status'" v-model="form[key]" :disabled="key === 'configId' && !editingNode?.data?.draft" class="w-full rounded-card border border-border-default bg-surface px-3 py-2 outline-none focus:border-accent" />
            <select v-else v-model.number="form[key]" class="w-full rounded-card border border-border-default bg-surface px-3 py-2 outline-none focus:border-accent">
              <option :value="1">启用</option>
              <option :value="0">禁用</option>
            </select>
          </label>
        </div>

        <div v-if="modalType === 'advisor' && form.type === 'Rag'" class="mt-5 rounded-card border border-border-subtle bg-surface p-3">
          <div class="mb-3 font-bold">RAG 知识库过滤</div>
          <div class="grid gap-3 sm:grid-cols-2">
            <label class="text-sm">
              <span class="mb-1 block font-semibold">ragTag</span>
              <select v-model="ragAdvisorForm.ragTag" class="w-full rounded-card border border-border-default bg-elevated px-3 py-2 outline-none focus:border-accent">
                <option value="">请选择知识库标签</option>
                <option v-for="tag in ragTags" :key="tag.ragTag" :value="tag.ragTag">{{ tag.ragTag }}</option>
              </select>
            </label>
            <label class="text-sm">
              <span class="mb-1 block font-semibold">topK</span>
              <input v-model.number="ragAdvisorForm.topK" type="number" class="w-full rounded-card border border-border-default bg-elevated px-3 py-2 outline-none focus:border-accent" />
            </label>
          </div>
          <p class="mt-2 text-xs text-text-tertiary">保存为：{"topK":4,"filterExpression":"knowledge == 'ragTag'"}</p>
        </div>

        <div v-if="modalType === 'client'" class="mt-5 border-t border-border-subtle pt-5">
          <div class="mb-3 font-bold">Flow 配置（当前 Agent）</div>
          <div class="grid gap-3 sm:grid-cols-2">
            <label class="text-sm"><span class="mb-1 block font-semibold">Flow Seq</span><input v-model.number="flowForm.flowSeq" type="number" class="w-full rounded-card border border-border-default bg-surface px-3 py-2" /></label>
            <label class="text-sm"><span class="mb-1 block font-semibold">Client Role</span><select v-model="flowForm.clientRole" class="w-full rounded-card border border-border-default bg-surface px-3 py-2"><option v-for="role in roleOrder" :key="role" :value="role">{{ role }}</option></select></label>
            <label class="text-sm sm:col-span-2"><span class="mb-1 block font-semibold">Flow Prompt</span><textarea v-model="flowForm.userPrompt" rows="3" class="w-full rounded-card border border-border-default bg-surface px-3 py-2" /></label>
          </div>
          <button v-if="flowForm.originAgentId && flowForm.originClientId" class="mt-3 text-sm font-semibold text-red-600" type="button" @click="deleteFlow">删除 Flow</button>
        </div>

        <div v-if="modalType === 'client' && !editingNode?.data?.draft" class="mt-5 border-t border-border-subtle pt-5">
          <div class="mb-3 font-bold">依赖配置</div>
          <div v-for="type in ['prompt','advisor','mcp']" :key="type" class="mb-4 rounded-card border border-border-subtle p-3">
            <div class="mb-2 font-semibold uppercase">{{ type }}</div>
            <div v-for="config in clientConfigs(type)" :key="config.configId" class="mb-2 flex items-center justify-between gap-2 rounded-card bg-surface px-3 py-2 text-sm">
              <span class="font-mono">{{ config.refId }}</span>
              <button class="text-red-600" type="button" @click="deleteClientConfig(config)">删除</button>
            </div>
            <div class="flex gap-2">
              <input v-model="newConfig[type]" class="min-w-0 flex-1 rounded-card border border-border-default bg-surface px-3 py-2 text-sm" :placeholder="`${type} ID`" />
              <UiButton variant="secondary" :loading="saving" @click="addClientConfig(type)">新增</UiButton>
            </div>
          </div>
        </div>

        <div class="mt-5 flex justify-end gap-2">
          <UiButton variant="secondary" @click="cancelModal">取消</UiButton>
          <UiButton :loading="saving" @click="saveModal">保存</UiButton>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.canvas-card {
  width: 220px;
  min-height: 108px;
  border: 1px solid #dbe3ee;
  border-radius: 12px;
  background: #fff;
  padding: 12px;
  color: #0f172a;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
  font-size: 12px;
}
.canvas-draft {
  border-style: dashed;
  border-color: #2563eb;
  background: #eff6ff;
}
.canvas-slot {
  border-style: dashed;
  border-color: #f97316;
  background: #fff7ed;
}
.canvas-missing {
  border-style: dashed;
  border-color: #dc2626;
  background: #fef2f2;
}
.canvas-card p {
  margin-top: 6px;
  color: #64748b;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
