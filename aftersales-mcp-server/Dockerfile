FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
RUN mvn -B -pl aftersales-mcp-server -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/aftersales-mcp-server/target/aftersales-mcp-server-0.1.0.jar /app/app.jar
EXPOSE 8097
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
