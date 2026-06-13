FROM eclipse-temurin:17-jre-alpine

ARG MCP_MODULE
ARG MCP_JAR
ARG MCP_PORT

WORKDIR /app
COPY ${MCP_MODULE}/target/${MCP_JAR} /app/mcp-server.jar

EXPOSE ${MCP_PORT}

ENTRYPOINT ["java", "-jar", "/app/mcp-server.jar"]
