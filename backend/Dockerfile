FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY ideal-agent-app/target/ideal-agent-app-1.0.0.jar /app/ideal-agent-app.jar

ENV SPRING_PROFILES_ACTIVE=docker

EXPOSE 8066

ENTRYPOINT ["java", "-jar", "/app/ideal-agent-app.jar"]
