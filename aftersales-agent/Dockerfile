FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
RUN mvn -B -pl aftersales-agent -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/aftersales-agent/target/aftersales-agent-0.1.0-exec.jar /app/app.jar
EXPOSE 8096
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
