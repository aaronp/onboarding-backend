FROM openjdk:23-slim
WORKDIR /app
COPY target/scala-3*/*-assembly-*.jar app.jar
ENTRYPOINT ["java", "-cp", "/app/app.jar", "kind.docstore.server.Server"]
