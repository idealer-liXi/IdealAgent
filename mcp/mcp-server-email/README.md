# mcp-server-email

IdealAgent SSE MCP server for email sending.

## Start

```powershell
mvn -q package
java -jar target/mcp-server-email.jar
```

## IdealAgent Config

MCP content:

```json
{"baseUri":"http://localhost:9004","sseEndpoint":"/sse","timeoutMinutes":3}
```

MCP secret:

```json
{"smtpHost":"smtp.qq.com","smtpPort":"465","smtpUsername":"your-email@qq.com","smtpPassword":"<smtp-auth-code>","fromAddress":"your-email@qq.com","fromName":"IdealAgent"}
```
