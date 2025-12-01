# ON CHANGE LA BASE : On s'aligne sur ta cible (Scalingo)
FROM --platform=linux/amd64 ubuntu:22.04 AS build

ENV DEBIAN_FRONTEND=noninteractive

# 1. Installation des outils de base
RUN apt-get update && apt-get install -y --no-install-recommends \
    software-properties-common \
    wget \
    make \
    git \
    cmake \
    ca-certificates

# 2. Installation explicite de GCC 12 (Dispo sur Ubuntu 22.04 mais pas par défaut)
RUN apt-get install -y --no-install-recommends \
    gcc-12 \
    g++-12

# 3. On définit GCC 12 comme compilateur par défaut
# (Sinon cmake va prendre gcc 11 qui est le défaut de ubuntu 22.04)
RUN update-alternatives --install /usr/bin/gcc gcc /usr/bin/gcc-12 100 \
    && update-alternatives --install /usr/bin/g++ g++ /usr/bin/g++-12 100