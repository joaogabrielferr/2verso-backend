
#Build Stage
FROM maven:3.9-eclipse-temurin-21-jammy AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -B -DskipTests

#Runtime Stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN groupadd -r appgroup && useradd -r -g appgroup -s /sbin/nologin appuser
COPY --from=builder /app/target/*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]