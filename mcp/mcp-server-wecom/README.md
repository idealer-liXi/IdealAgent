# mcp-server-wecom

IdealAgent SSE MCP server for WeCom application message sending.

## Start

```powershell
mvn -q package
java -jar target/mcp-server-wecom.jar
```

## IdealAgent Config

MCP content:

```json
{"baseUri":"http://localhost:9002","sseEndpoint":"/sse","timeoutMinutes":3}
```

MCP secret:

```json
{"corpId":"<corp-id>","corpSecret":"<corp-secret>","agentId":"<agent-id>"}
```
