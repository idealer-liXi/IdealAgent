# mcp-server-amap

IdealAgent SSE MCP server for Amap weather lookup.

## Start

```powershell
mvn -q package
java -jar target/mcp-server-amap.jar
```

## IdealAgent Config

MCP content:

```json
{"baseUri":"http://localhost:9003","sseEndpoint":"/sse","timeoutMinutes":3}
```

MCP secret:

```json
{"key":"<amap-api-key>"}
```
