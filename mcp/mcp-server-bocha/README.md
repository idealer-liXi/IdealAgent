# mcp-server-bocha

IdealAgent SSE MCP server for Bocha web search.

## Start

```powershell
mvn -q package
java -jar target/mcp-server-bocha.jar
```

## IdealAgent Config

MCP content:

```json
{"baseUri":"http://localhost:9005","sseEndpoint":"/sse","timeoutMinutes":3}
```

MCP secret:

```json
{"apiKey":"<bocha-api-key>"}
```
