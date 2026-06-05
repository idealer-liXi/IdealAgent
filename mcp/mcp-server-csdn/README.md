# mcp-server-csdn

IdealAgent SSE MCP server for CSDN article publishing.

## Start

```powershell
mvn -q package
java -jar target/mcp-server-csdn.jar
```

## IdealAgent Config

MCP content:

```json
{"baseUri":"http://localhost:9001","sseEndpoint":"/sse","timeoutMinutes":3}
```

MCP secret:

```json
{"cookie":"<csdn-cookie>","categories":"<category>","tags":"<tag1,tag2>","coverUrl":"<cover-url>"}
```
