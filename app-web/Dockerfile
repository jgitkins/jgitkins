FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace
COPY . .
RUN ./gradlew :app-web:bootJar -x test

RUN cp app-web/build/libs/app.jar /workspace/app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app

# install SOPS
ARG SOPS_VERSION=3.9.1
ARG TARGETARCH
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl \
    && case "${TARGETARCH}" in \
      amd64) SOPS_ARCH="amd64" ;; \
      arm64) SOPS_ARCH="arm64" ;; \
      *) echo "Unsupported arch: ${TARGETARCH}" && exit 1 ;; \
    esac \
    && curl -fsSL -o /usr/local/bin/sops \
      "https://github.com/getsops/sops/releases/download/v${SOPS_VERSION}/sops-v${SOPS_VERSION}.linux.${SOPS_ARCH}" \
    && chmod +x /usr/local/bin/sops \
    && apt-get purge -y --auto-remove curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /workspace/app.jar /app/app.jar
COPY app-web/secrets /app/secrets
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
