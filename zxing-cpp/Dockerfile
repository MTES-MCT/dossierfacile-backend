# syntax=docker/dockerfile:1
# Build dynamique sur Debian bullseye (glibc 2.31) pour bonne compatibilité
FROM debian:bullseye AS build
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential cmake git ca-certificates binutils \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /src

# ---- ZXing-CPP (shared) ----
# Pin sur un tag stable ; change si besoin.
ARG ZXING_REF=v2.3.0
RUN git clone --depth=1 --branch ${ZXING_REF} https://github.com/zxing-cpp/zxing-cpp.git

RUN cmake -S zxing-cpp -B zxing-cpp/build -DCMAKE_BUILD_TYPE=Release \
    -DBUILD_SHARED_LIBS=ON -DBUILD_EXAMPLES=OFF -DBUILD_TESTING=OFF
RUN cmake --build zxing-cpp/build --config Release -j
# Installe headers + lib partagée dans /src/local
RUN cmake --install zxing-cpp/build --prefix /src/local

# ---- Ton wrapper ----
# Assure-toi que zxing_wrapper.cpp est à côté du Dockerfile
COPY zxing_wrapper.cpp /src/

# Dossiers de sortie
RUN mkdir -p /out
RUN mkdir -p /dist/natives/linux-x64

# Compile le wrapper dynamiquement contre libZXing.so.*
# rpath => $ORIGIN pour que le loader trouve libZXing.so.3 dans le même dossier
RUN c++ -std=c++17 -O3 -shared -fPIC /src/zxing_wrapper.cpp -o /out/libzxing_jna.so \
    -I /src/local/include -L /src/local/lib -lZXing \
    -Wl,-rpath,'$ORIGIN'

# ---- Prépare artefacts à exporter ----
RUN mkdir -p /dist/natives/linux-x86_64 \
 && cp /out/libzxing_jna.so /dist/natives/linux-x86_64/ \
 # copie "réelle" (pas symlink) sous le nom attendu libZXing.so.3
 && cp -L /src/local/lib/libZXing.so.3 /dist/natives/linux-x86_64/ \
 # réduction de taille (optionnel)
 && strip --strip-unneeded /dist/natives/linux-x86_64/libzxing_jna.so || true \
 && strip --strip-unneeded /dist/natives/linux-x86_64/libZXing.so.3 || true
