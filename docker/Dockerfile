# Build Stage
FROM gradle:jdk25-corretto AS build
WORKDIR /app
COPY gradle ./gradle
COPY gradlew build.gradle.kts settings.gradle.kts lombok.config custom_google_checks.xml ./
COPY spotbugs ./spotbugs
RUN gradle dependencies
COPY src ./src
RUN gradle clean build -x test

# Production Stage
FROM amazoncorretto:25-alpine-jdk
RUN apk update && \
    apk add --no-cache curl busybox-extras openssl && \
    rm -rf /var/cache/apk/*
WORKDIR /app
COPY --from=build /app/build/libs/*.jar /app/jena.jar
EXPOSE 8080
ENTRYPOINT exec java $JAVA_OPTS -jar /app/jena.jar