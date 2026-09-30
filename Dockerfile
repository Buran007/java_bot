FROM gradle:8.14.3-jdk21-alpine AS build

WORKDIR /workspace
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
RUN chmod +x gradlew
COPY src ./src
RUN ./gradlew --no-daemon installDist

FROM eclipse-temurin:21-jre-alpine
WORKDIR /opt/meetbot
RUN addgroup -S meetbot && adduser -S meetbot -G meetbot
COPY --from=build --chown=meetbot:meetbot /workspace/build/install/meetbot ./
USER meetbot
ENTRYPOINT ["/opt/meetbot/bin/meetbot"]
