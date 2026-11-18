# ---------- Bước 1: build file jar ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Tải dependency trước (layer này được cache nếu pom.xml không đổi)
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package && cp target/*.jar app.jar

# ---------- Bước 2: image chạy, chỉ có JRE ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 app
COPY --from=build /app/app.jar app.jar
USER app

# Gói free của Render chỉ có 512MB RAM: giới hạn heap theo % RAM của container, dùng SerialGC cho nhẹ
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k -Duser.timezone=UTC"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
