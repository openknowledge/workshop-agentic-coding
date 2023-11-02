# syntax=docker/dockerfile:1.7

# ============================================================================
# Stage 1: Build Frontend (React/TypeScript/Vite)
# ============================================================================
FROM maven:3.9.9-eclipse-temurin-21 AS client-build

RUN mkdir -p /usr/customer-management-client
WORKDIR /usr

# Cache node in a separate layer
COPY customer-management-client/pom.xml customer-management-client/pom.xml
RUN --mount=type=cache,target=/root/.m2/repository,id=mvn-cache,sharing=locked \
    mvn -f customer-management-client/pom.xml initialize

# Cache npm dependencies (npm cache mount; node_modules itself remains in layer)
COPY customer-management-client/package.json customer-management-client/package-lock.json customer-management-client/
RUN --mount=type=cache,target=/root/.m2/repository,id=mvn-cache,sharing=locked \
    --mount=type=cache,target=/root/.npm,id=npm-cache,sharing=locked \
    mvn -f customer-management-client/pom.xml generate-sources

# Copy build-relevant source files
COPY customer-management-client/src                    /usr/customer-management-client/src
COPY customer-management-client/public                 /usr/customer-management-client/public
COPY customer-management-client/index.html             /usr/customer-management-client/index.html
COPY customer-management-client/vite.config.ts         /usr/customer-management-client/vite.config.ts
COPY customer-management-client/tsconfig.app.json      /usr/customer-management-client/tsconfig.app.json
COPY customer-management-client/tsconfig.node.json     /usr/customer-management-client/tsconfig.node.json
COPY customer-management-client/.prettierrc            /usr/customer-management-client/.prettierrc
COPY customer-management-client/.prettierignore        /usr/customer-management-client/.prettierignore

# Override tsconfig.json: drop test project reference (tests don't run in container)
# We don't copy `tests/` to keep the build layer cacheable when test files change
RUN echo '{"files":[],"references":[{"path":"./tsconfig.app.json"},{"path":"./tsconfig.node.json"}]}' \
    > /usr/customer-management-client/tsconfig.json

# Build client and export to /export for next stage
RUN --mount=type=cache,target=/root/.m2/repository,id=mvn-cache,sharing=locked \
    --mount=type=cache,target=/root/.npm,id=npm-cache,sharing=locked \
    mvn -f customer-management-client/pom.xml install -DskipLinting -DskipTests \
    && mkdir -p /export/.m2/repository/de/openknowledge \
    && cp -r /root/.m2/repository/de/openknowledge/. /export/.m2/repository/de/openknowledge/

# ============================================================================
# Stage 2: Build Backend (Spring Boot)
# ============================================================================
FROM maven:3.9.9-eclipse-temurin-21 AS mvn

ARG CLIENT_GROUP_PATH=de/openknowledge
ARG CLIENT_ARTIFACT=customer-management-client
ARG CLIENT_VERSION=0.1.0-SNAPSHOT

WORKDIR /usr/customer-management-server

# Copy server POM and checkstyle config for dependency resolution
COPY customer-management-client/pom.xml /tmp/client-pom.xml
COPY customer-management-server/pom.xml /usr/customer-management-server/
COPY customer-management-server/src/main/checkstyle/java.header.plain /usr/customer-management-server/src/main/checkstyle/java.header.plain

# Create stub client JAR and resolve dependencies in ONE RUN
# (cache mount overlays layer content, so stub must be created in same context)
# This allows BuildKit to parallelize client-build and server dependency resolution
RUN --mount=type=cache,target=/root/.m2/repository,id=mvn-cache,sharing=locked \
    CLIENT_REPO_PATH=/root/.m2/repository/${CLIENT_GROUP_PATH}/${CLIENT_ARTIFACT}/${CLIENT_VERSION} \
    && mkdir -p ${CLIENT_REPO_PATH} \
    && cp /tmp/client-pom.xml ${CLIENT_REPO_PATH}/${CLIENT_ARTIFACT}-${CLIENT_VERSION}.pom \
    && (cd /tmp && jar cf ${CLIENT_REPO_PATH}/${CLIENT_ARTIFACT}-${CLIENT_VERSION}.jar client-pom.xml) \
    && mvn dependency:resolve dependency:resolve-plugins dependency:go-offline spotless:check

# Stage real client JAR from client-build stage
# (cache mount would hide direct COPY into maven repo, so stage to /tmp first)
COPY --from=client-build /export/.m2/repository/${CLIENT_GROUP_PATH}/ /tmp/client-repo/${CLIENT_GROUP_PATH}/

# Build server: copy real client JAR over stub, then run offline install
COPY customer-management-server/src/main src/main
RUN --mount=type=cache,target=/root/.m2/repository,id=mvn-cache,sharing=locked \
    mkdir -p /root/.m2/repository/${CLIENT_GROUP_PATH} \
    && cp -r /tmp/client-repo/${CLIENT_GROUP_PATH}/. /root/.m2/repository/${CLIENT_GROUP_PATH}/ \
    && mvn -o -Dcheckstyle.skip -DskipTests clean install \
    && chmod -R u+x /usr/customer-management-server/target/*.jar

# ============================================================================
# Stage 3: Runtime (Minimal JRE)
# ============================================================================
FROM eclipse-temurin:21-jre-ubi10-minimal

WORKDIR /usr/app

# Copy server JAR from build stage
COPY --from=mvn /usr/customer-management-server/target/*.jar /usr/app/server.jar

ENTRYPOINT ["java", "-jar", "/usr/app/server.jar"]
