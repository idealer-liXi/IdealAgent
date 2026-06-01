# IdealAgent 前端美化设计方案

**Date:** 2026-06-01
**Author:** AI Assistant
**Status:** Approved

---

## 1. 概述

本项目为 IdealAgent AI Agent 管理后台的前端视觉升级方案。目标是将现有蓝白配色的基础 UI 升级为**现代极简、黑白灰为主、适度微交互**的专业风格，同时通过组件拆分提升代码可维护性。

### 设计原则
- **克制**：减少装饰性元素，让内容本身成为焦点
- **一致性**：通过 Design Token 统一所有视觉参数
- **层次**：通过背景色、阴影、间距建立清晰的信息层级
- **反馈**：通过微交互让系统状态对用户可见

---

## 2. 设计系统（Design Tokens）

所有视觉常量通过 CSS 变量定义，存储于 `frontend/src/styles/tokens.css`。

### 2.1 色彩体系

| Token | 值 | 用途 |
|---|---|---|
| `--color-bg-base` | `#ffffff` | 页面底层背景 |
| `--color-bg-surface` | `#f8f9fa` | 卡片、面板、消息气泡背景 |
| `--color-bg-elevated` | `#ffffff` | 弹窗、下拉菜单、悬浮面板 |
| `--color-bg-dark` | `#1a1a2e` | 侧边栏背景 |
| `--color-text-primary` | `#1a1a2e` | 标题、正文 |
| `--color-text-secondary` | `#6b7280` | 辅助说明、标签 |
| `--color-text-tertiary` | `#9ca3af` | 占位符、禁用状态、分割线文字 |
| `--color-text-inverse` | `#ffffff` | 深色背景上的文字 |
| `--color-border-subtle` | `#f1f3f5` | 极浅分隔线 |
| `--color-border-default` | `#e5e7eb` | 标准边框、卡片边框 |
| `--color-border-focus` | `#2563eb` | 输入框聚焦边框 |
| `--color-accent` | `#2563eb` | 主按钮、链接、选中态、强调 |
| `--color-accent-hover` | `#1d4ed8` | 按钮悬停、链接悬停 |
| `--color-accent-light` | `#eff6ff` | 浅色点缀背景 |
| `--color-error` | `#ef4444` | 错误提示 |
| `--color-error-bg` | `#fef2f2` | 错误提示背景 |

### 2.2 间距体系

| Token | 值 |
|---|---|
| `--space-xs` | `4px` |
| `--space-sm` | `8px` |
| `--space-md` | `16px` |
| `--space-lg` | `24px` |
| `--space-xl` | `32px` |
| `--space-2xl` | `48px` |

### 2.3 圆角体系

| Token | 值 | 用途 |
|---|---|---|
| `--radius-sm` | `8px` | 小按钮、标签 |
| `--radius-md` | `12px` | 输入框、会话列表项 |
| `--radius-lg` | `16px` | 卡片、面板 |
| `--radius-xl` | `20px` | 大卡片、欢迎页卡片 |
| `--radius-full` | `9999px` | 圆形头像、胶囊标签 |

### 2.4 阴影体系

| Token | 值 | 用途 |
|---|---|---|
| `--shadow-sm` | `0 1px 2px rgba(0,0,0,0.04)` | 卡片默认态 |
| `--shadow-md` | `0 4px 12px rgba(0,0,0,0.05)` | 卡片悬浮态 |
| `--shadow-lg` | `0 8px 24px rgba(0,0,0,0.06)` | 下拉菜单、弹窗 |
| `--shadow-sidebar` | `4px 0 16px rgba(0,0,0,0.04)` | 侧边栏右侧阴影 |

---

## 3. 组件架构

### 3.1 目录结构

```
frontend/src/
├── styles/
│   └── tokens.css              # Design Tokens CSS 变量
├── components/
│   ├── ui/                     # 原子组件（可复用）
│   │   ├── UiButton.vue
│   │   ├── UiCard.vue
│   │   ├── UiInput.vue
│   │   ├── UiBadge.vue
│   │   └── UiLoading.vue
│   ├── chat/                   # Chat 页专属组件
│   │   ├── ChatMessage.vue
│   │   ├── ChatInput.vue
│   │   └── SessionList.vue
│   ├── config/                 # Config 页专属组件
│   │   └── ConfigSection.vue
│   ├── welcome/                # Welcome 页专属组件
│   │   └── WelcomeStats.vue
│   ├── Sidebar.vue             # 全局侧边栏（重写）
│   ├── Footer.vue              # 全局页脚（重写）
│   ├── Auth.vue                # 登录页（重写）
│   ├── Welcome.vue             # 欢迎页（重写）
│   ├── Chat.vue                # 对话页（重写，引用子组件）
│   └── AiConfig.vue            # 配置页（重写，引用子组件）
```

### 3.2 原子组件规范

#### UiButton
- **变体**：`primary`（蓝色实心）、`secondary`（灰色描边）、`ghost`（透明，仅文字）
- **尺寸**：`sm`、`md`（默认）、`lg`
- **状态**：默认、hover、active（scale 0.97）、disabled（opacity 0.5）
- **圆角**：`radius-md`（12px）

#### UiCard
- **结构**：容器，接受 `title` 插槽和默认内容插槽
- **样式**：`bg-surface` 或 `bg-elevated`，`radius-lg`（16px），`shadow-sm`，hover 时 `shadow-md`
- **可选**：带 `hoverable` 属性开启悬浮效果

#### UiInput
- **样式**：`radius-md`，`border-default`，`bg-surface`，focus 时 `border-focus` + `ring-accent-light`
- **标签**：顶部对齐，`text-sm font-medium text-secondary`

#### UiBadge
- **变体**：`accent`（蓝色）、`success`（绿色）、`neutral`（灰色）
- **样式**：`radius-full`，小内边距，胶囊形状

#### UiLoading
- **样式**：16px 旋转圆环，`color-accent`
- **模式**：独立使用或内嵌于按钮中

### 3.3 页面专属组件

#### ChatMessage
- **Props**：`role`（'user' | 'assistant'）、`content`、`timestamp`
- **User 消息**：白色背景，左侧 3px `color-accent` 竖线，无 border
- **Assistant 消息**：`color-bg-surface` 背景，无 border，无竖线
- **动画**：新消息进入时淡入 + `translateY(8px → 0)`，250ms ease-out

#### ChatInput
- **Props**：`loading`（布尔值）
- **结构**：textarea + 发送按钮（`UiButton primary lg`）
- **布局**：悬浮于 Chat 页面底部，`bg-elevated`，顶部细 border

#### SessionList
- **Props**：`sessions`（数组）、`activeSessionId`
- **选中态**：左侧 3px `color-accent` 竖线 + `bg-accent-light` 背景
- **未选中态**：白色背景，hover 时 `bg-surface`
- **无数据**：居中文字提示，`text-tertiary`

#### ConfigSection
- **Props**：`title`、`description`
- **结构**：`UiCard` 容器，标题区 + 表单内容区
- **用途**：Config 页中每个配置类别（API、Model、Client 等）一个实例

#### WelcomeStats
- **结构**：横向排列的 3-4 个数据卡片
- **内容**：系统状态、后端连接状态、用户角色、项目阶段
- **样式**：`UiCard`，图标 + 数字/状态 + 标签

---

## 4. 页面级设计

### 4.1 Sidebar（全局）

#### 布局
- 宽度：`240px`（从 260px 收窄）
- 背景：`color-bg-dark`（#1a1a2e），移除渐变
- 右侧阴影：`shadow-sidebar`

#### Logo 区
- 简化为：圆形图标（IA 字母）+ 横向文字 "IdealAgent"
- 图标背景：`color-accent` 纯色
- 副标题 "MiniAgent rebuild" 移除或改为更小字号

#### 导航项
- **默认态**：`text-inverse` 透明度 70%，左侧无指示器
- **Hover**：文字透明度 100%，左侧出现 3px `color-accent` 竖线（滑入动画，200ms）
- **激活态**：`bg-white/10`，左侧 3px 竖线常驻，`text-inverse` 不透明
- **间距**：`space-sm` 间隔，`radius-md` 圆角

#### 用户信息区
- 位置：侧边栏底部
- 样式：简化为单行（用户名 + 角色徽章），下方紧凑的退出按钮
- 退出按钮：`ghost` 变体，`text-inverse`，hover `bg-white/10`

### 4.2 Welcome（仪表盘）

#### 布局
- 从"垂直堆叠的营销页风格"改为"横向网格的仪表盘"
- 顶部：一行统计卡片（WelcomeStats）
- 中部：核心功能入口卡片（新建 Chat、配置中心）
- 底部：可选的系统公告或快捷操作

#### 统计卡片
- 使用 `UiCard`，4 列网格
- 内容：图标（使用 Heroicons 或 Lucide 图标，或保持纯 CSS）+ 数据值 + 标签
- 示例：
  - "后端状态" → 绿色圆点 + "已连接"
  - "当前阶段" → "Stage 4"
  - "用户角色" → "admin"
  - "当前用户" → 用户名

#### 功能入口卡片
- 使用 `UiCard`，hover 时 `-translateY(2px)` + `shadow-md`
- 左侧蓝色竖线装饰
- 标题 22px bold，描述 14px secondary
- "立即前往 →" 链接样式

### 4.3 Chat（对话页）

#### 整体布局
- 左侧：会话列表（宽度 280px，固定）
- 右侧：消息区 + 输入区（自适应）

#### 会话列表区
- 顶部："Chat" 标签 + "新会话"按钮（`UiButton primary`）
- Client ID 输入框：使用 `UiInput`，`font-mono`
- 会话列表：使用 `SessionList` 组件
- 底部：刷新按钮

#### 消息区
- 顶部：当前 sessionId 或 "新会话"，`font-mono text-sm text-tertiary`
- 消息列表：使用 `ChatMessage` 组件循环渲染
- 空状态：居中，"发送第一条消息开始对话"，`text-tertiary`

#### 输入区
- 位置：消息区底部，sticky
- 结构：textarea（`UiInput`，`min-h-24`）+ 发送按钮（`UiButton primary lg`）
- 错误提示：使用 `UiCard` 变体，红色背景

#### 动画
- 发送按钮点击：scale 0.97，100ms
- 新消息进入：淡入 + translateY，250ms
- 会话切换：消息列表淡入，150ms

### 4.4 AiConfig（配置页）

#### 布局
- 从"密集列表/表单"改为"分组卡片网格"
- 每类配置（API、Model、Client、Prompt、Advisor、MCP）一个 `ConfigSection`
- 2 列网格（大屏）或 1 列（小屏）

#### ConfigSection 内部
- 标题区：图标 + 标题 + 描述
- 内容区：表单或表格
- 操作区：保存/重置按钮（`UiButton`）

#### 表单样式
- 标签顶部对齐，`text-sm font-medium text-secondary`
- 输入框使用 `UiInput`
- 按钮组右对齐

### 4.5 Auth（登录页）

#### 布局
- 全屏居中，大面积留白
- 单个居中大卡片，`max-w-md`，`radius-xl`

#### 卡片内部
- 顶部：Logo（大）+ "IdealAgent" + "登录"
- 中部：用户名输入框 + 密码输入框（使用 `UiInput`）
- 底部：登录按钮（`UiButton primary`，full-width，`radius-md`）
- 错误提示：红色文字，卡片内顶部

#### 背景
- `color-bg-base`（纯白），无图案，极简

---

## 5. 微交互动画规范

### 5.1 全局过渡

| 场景 | 效果 | 时长 | 缓动函数 |
|---|---|---|---|
| 页面进入 | `opacity: 0 → 1` | 200ms | `ease-out` |
| 卡片 hover | `translateY(0 → -2px)` + `shadow-sm → shadow-md` | 200ms | `cubic-bezier(0.4, 0, 0.2, 1)` |
| 卡片 hover 离开 | 反向 | 200ms | `cubic-bezier(0.4, 0, 0.2, 1)` |
| 按钮 hover | 背景色过渡 | 150ms | `ease` |
| 按钮 active | `scale(1 → 0.97)` | 100ms | `ease` |
| 侧边栏指示器 | 竖线滑入/宽度展开 | 200ms | `ease-out` |

### 5.2 Chat 专属动画

| 场景 | 效果 | 时长 | 缓动函数 |
|---|---|---|---|
| 新消息进入 | `opacity: 0 → 1` + `translateY(8px → 0)` | 250ms | `ease-out` |
| 发送中 loading | 按钮内 `UiLoading` 替换文字 | — | — |
| 会话切换 | 消息列表 `opacity: 1 → 0 → 1` | 150ms | `ease` |

### 5.3 输入框动画

| 场景 | 效果 | 时长 | 缓动函数 |
|---|---|---|---|
| 聚焦 | `border-default → border-focus` + `ring-accent-light` 出现 | 150ms | `ease` |
| 失焦 | 反向 | 150ms | `ease` |

---

## 6. Tailwind CSS 映射

为保持 Tailwind 使用的一致性，将常用组合映射为自定义类（在 `tokens.css` 中定义 `@layer components`）：

```css
@layer components {
  .surface { @apply bg-[var(--color-bg-surface)]; }
  .elevated { @apply bg-[var(--color-bg-elevated)]; }
  .text-primary { @apply text-[var(--color-text-primary)]; }
  .text-secondary { @apply text-[var(--color-text-secondary)]; }
  .text-tertiary { @apply text-[var(--color-text-tertiary)]; }
  .border-subtle { @apply border-[var(--color-border-subtle)]; }
  .border-default { @apply border-[var(--color-border-default)]; }
  .shadow-card { @apply shadow-[var(--shadow-sm)] hover:shadow-[var(--shadow-md)]; }
  .transition-card { @apply transition-all duration-200 ease-[cubic-bezier(0.4,0,0.2,1)]; }
}
```

---

## 7. 实现策略

### 阶段 1：基础层
1. 创建 `frontend/src/styles/tokens.css`
2. 在 `main.js` 中引入 `tokens.css`
3. 确保 CSS 变量生效

### 阶段 2：原子组件
1. 创建 `frontend/src/components/ui/` 目录
2. 依次实现 `UiButton`、`UiCard`、`UiInput`、`UiBadge`、`UiLoading`
3. 每个组件附带简单用法注释

### 阶段 3：全局组件
1. 重写 `Sidebar.vue`
2. 重写 `Footer.vue`（极简版权信息 + 版本号）

### 阶段 4：页面组件拆分与重写
1. 创建 `ChatMessage.vue`、`ChatInput.vue`、`SessionList.vue`
2. 重写 `Chat.vue`（引用子组件）
3. 创建 `ConfigSection.vue`
4. 重写 `AiConfig.vue`（引用子组件）
5. 创建 `WelcomeStats.vue`
6. 重写 `Welcome.vue`
7. 重写 `Auth.vue`

### 阶段 5：动画与打磨
1. 为各组件添加过渡效果
2. 全局页面进入动画
3. 细节一致性检查（颜色、间距、圆角）
4. 响应式适配检查

---

## 8. 兼容性说明

- **浏览器**：支持 Chrome、Edge、Firefox、Safari 最新 2 个主版本
- **响应式**：
  - `lg`（1024px+）：完整双栏布局（Chat、Config）
  - `md`（768px-1023px）：单栏布局，侧边栏可折叠（后续迭代）
  - `sm`（<768px）：单栏布局，侧边栏隐藏（后续迭代）
- **当前范围**：本次美化以 `lg` 及以上屏幕为主，保持现有 `md` 降级（不恶化）

---

## 9. 风险与回退

| 风险 | 缓解措施 |
|---|---|
| Tailwind 自定义类冲突 | 使用 CSS 变量而非 `@apply` 硬编码；保持类名语义化 |
| 组件拆分引入 bug | 保持原有数据流（props/emits）不变；逐个页面替换 |
| 动画性能问题 | 仅使用 `transform` 和 `opacity` 动画，避免触发重排 |
| 用户不适应新 UI | 保留原有信息架构和交互流程，仅改视觉层 |

---

*本设计文档经用户确认后生效，后续实现将严格遵循此规范。*
