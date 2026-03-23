FROM amazoncorretto:17-alpine AS build
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

RUN chmod +x ./gradlew
RUN ./gradlew clean bootJar -x test

# 2단계: 실행 스테이지
FROM amazoncorretto:17-alpine
WORKDIR /app

# 빌드 스테이지에서 생성된 jar 복사
COPY --from=build /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]