# Stufe 1: Frontend bauen (Node.js wird nur hier gebraucht, nicht im fertigen Image)
FROM node:24-alpine AS frontend
WORKDIR /frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# Stufe 2: Backend bauen; das Frontend wandert als statische Dateien in das JAR
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /backend
COPY backend/pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY backend/src ./src
COPY --from=frontend /frontend/dist ./src/main/resources/static
RUN mvn -B -q package -DskipTests

# Stufe 3: Laufzeit-Image, enthält nur Java und das JAR
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=backend /backend/target/fragetool.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
