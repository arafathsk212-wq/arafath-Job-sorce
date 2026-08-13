FROM maven:3.9.2-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY c2c-job-sourcer-backend/pom.xml ./
COPY c2c-job-sourcer-backend/src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/target/c2c-job-sourcer-backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
