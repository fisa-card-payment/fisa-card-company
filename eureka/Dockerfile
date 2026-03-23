FROM amazoncorretto:17-alpine AS build
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

RUN chmod +x ./gradlew
RUN ./gradlew clean bootJar -x test

FROM amazoncorretto:17-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

# Eureka 기본 포트는 보통 8761입니다.
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]