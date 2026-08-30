FROM maven:3.9.9-eclipse-temurin-21 AS deps
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B


FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY --from=deps /root/.m2 /root/.m2
COPY . .
RUN mvn clean package -DskipTests


FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar order-service.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar","order-service.jar"]