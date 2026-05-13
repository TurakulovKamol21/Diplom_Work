# =========================
# 1-qadam: Build stage
# =========================
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# pom.xml ni alohida COPY qilamiz (dependency caching uchun)
COPY backend/pom.xml ./pom.xml

# faqat dependency’larni offline yuklaymiz
RUN mvn dependency:go-offline -B

# Source code’ni keyin COPY qilamiz
COPY backend/src ./src

# Build jar (testlarni o‘tkazmaymiz)
RUN mvn clean package -DskipTests

# =========================
# 2-qadam: Run stage
# =========================
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Build qilingan jarni COPY qilamiz
COPY --from=build /app/target/*.jar uz_macro_ai.jar

# Entry point
ENTRYPOINT ["java", "-jar", "uz_macro_ai.jar"]
