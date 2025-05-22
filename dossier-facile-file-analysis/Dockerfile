FROM --platform=linux/amd64 ghcr.io/graalvm/native-image-community:21.0.1 AS builder
RUN microdnf install -y tar gzip
WORKDIR /workspace/app
COPY . /workspace/app
RUN ./mvnw -Pnative -DskipTests package

FROM --platform=linux/amd64 bitnami/java:21-debian-12
RUN apt-get update && apt-get install -y \
    libopencv-dev  \
     && rm -rf /var/lib/apt/lists/*

COPY --from=builder --chmod=0755 /workspace/app/target/dossier-facile-file-analysis /usr/local/bin/app

ENTRYPOINT ["/usr/local/bin/app"]