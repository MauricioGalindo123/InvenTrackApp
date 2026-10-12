# Despliegue de InvenTrack (Render). El contexto de construccion es la raiz del repositorio.

# Etapa 1: compilar con Maven y Java 25
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY inventrack/.mvn .mvn
COPY inventrack/mvnw inventrack/pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY inventrack/src src
RUN ./mvnw -q -B -DskipTests package

# Etapa 2: imagen liviana solo con Java para ejecutar
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
COPY db/inventrack.sql db/inventrack.sql
COPY deploy/start-server.sh start-server.sh
RUN chmod +x start-server.sh
EXPOSE 8080
ENTRYPOINT ["/app/start-server.sh"]
