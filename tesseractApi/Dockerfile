FROM ubuntu:18.04
RUN apt-get update \
    && apt-get install tesseract-ocr -y \
    python3 \
    python3-pip \
    && apt-get clean \
    && apt-get autoremove

RUN apt-get update 
RUN apt-get install ffmpeg libsm6 libxext6  -y

RUN apt-get update && apt-get install --no-install-recommends -y \
  software-properties-common \
  build-essential \
  cmake \
  libsm6 \
  libxext6 \
  libxrender-dev \
  tesseract-ocr \
  tesseract-ocr-fra \
  poppler-utils \
  git \
  python3 \
  python3-dev \
  python3-pip && \
  if [ ! -e /usr/bin/python ]; then ln -sf python3 /usr/bin/python ; fi && \
  if [ ! -e /usr/bin/pip ]; then ln -s pip3 /usr/bin/pip ; fi && \
  pip install --no-cache --upgrade pip setuptools wheel && \
  rm -rf /var/lib/apt/lists/*

RUN pip install --no-cache --upgrade \
    opencv-contrib-python \
    pillow \
    pytesseract \
    imutils \
    pdf2image \
    Flask \
    requests \
    pypdf2

#COPY ./requirements.txt /app/requirements.txt

WORKDIR /app

#RUN pip install -r requirements.txt

COPY . /app

ENTRYPOINT [ "python" ]

EXPOSE 8080

CMD [ "app.py" ]