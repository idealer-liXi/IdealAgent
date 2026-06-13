# IdealAgent

IdealAgent 是一个面向智能体配置、执行和展示的全栈应用。项目围绕 Chat、RAG、MCP 工具、Work Agent 工作流、Agent 管理和配置中心构建，支持从模型/API 配置到多步骤任务执行的完整闭环。

## 功能特性

- **Chat 会话**：支持会话创建、消息持久化、Markdown 渲染和历史记录查看。
- **RAG 能力**：支持文件/Git 内容导入、向量检索、Advisor 配置和检索参数管理。
- **MCP 工具**：已接入 Amap、Bocha、CSDN、Email 等 SSE MCP 服务，支持通过 Work Agent 调用外部工具。
- **Work Agent**：支持 Inspector、Planner、Runner、Replier 等策略角色，按步骤执行任务并记录过程事件。
- **Agent 管理**：支持创建 Agent、自动生成策略 Prompt/Client/Flow/Binding，并维护 Canvas 配置。
- **展示页**：提供 Agent、MCP、Model 等只读展示接口，避免泄露密钥和运行时敏感配置。
- **Workshop**：支持按向导创建可执行 Agent，并绑定模型、Prompt、Advisor 和 MCP 工具。
- **Docker 部署**：提供 backend、frontend、MySQL、pgvector 和 MCP 服务的容器化部署配置。

## 技术栈

- Backend：Java 17、Spring Boot 3、MyBatis、MySQL、PostgreSQL pgvector、Spring AI
- Frontend：Vue 3、Vite、Tailwind CSS
- MCP：Spring AI MCP SSE Server、Java 17
- Infra：Docker、Docker Compose、Nginx、MySQL 8、pgvector

## 目录结构

```text
backend/                 Spring Boot 多模块后端
  ideal-agent-app/       应用启动模块
  ideal-agent-domain/    领域服务与业务编排
  ideal-agent-trigger/   HTTP Controller
  ideal-agent-infrastructure/ 持久化与外部基础设施
  docs/docker/           Docker Compose 部署配置
  docs/mysql/            MySQL schema 与脱敏 seed
frontend/                Vue 3 前端
mcp/                     独立 MCP SSE 服务
docs/                    项目计划与设计文档
```

## 本地开发

### 后端

```bash
cd backend
mvn clean package
```

开发环境配置建议放在未跟踪的本地配置文件或环境变量中，不要提交真实 API Key、SMTP 授权码、Cookie、数据库密码等敏感信息。

### 前端

```bash
cd frontend
npm install
npm run dev
```

生产构建：

```bash
cd frontend
npm run build
```

## Docker 部署

Docker 相关配置位于：

```text
backend/docs/docker/docker-compose.yml
backend/docs/docker/.env.example
```

部署前复制 `.env.example` 为私有 `.env`，并填入运行时配置。`.env` 已被 Git 忽略，不应提交。

```bash
cd backend/docs/docker
docker compose up -d
```

MCP 服务在 Docker 网络内通过容器名访问，例如：

```text
http://agent-mcp-bocha:9005
http://agent-mcp-email:9004
```

MCP 端口默认不暴露到公网，后端通过 Docker 内网调用。

## 数据与隐私

- `backend/docs/mysql/ideal_agent_seed.sql` 仅保存脱敏后的基础配置数据。
- 聊天会话和消息不进入 seed。
- 真实 `api_key`、`mcp_secret`、SMTP 授权码、Cookie、JWT Secret 等必须通过私有环境变量、私有 SQL 或后台管理界面配置。
- 提交前请执行敏感信息扫描，确认没有真实密钥进入 Git。

## 常用验证命令

```bash
cd backend
mvn clean package
```

```bash
cd frontend
npm run build
```

```bash
docker compose -f backend/docs/docker/docker-compose.yml config --quiet
```

## 状态

当前项目已完成核心 Chat、RAG、MCP、Work Agent、Agent 管理、Config 管理、Workshop 和 Docker 部署链路。后续可继续优化方向包括：MCP 服务整合、RAG 可选化、部署资源精简、监控与运维脚本完善。
