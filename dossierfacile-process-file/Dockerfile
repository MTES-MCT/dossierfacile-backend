FROM ubuntu:23.10
RUN apt-get update \
    && apt-get install -y openjdk-20-jre-headless \
    && apt-get install tesseract-ocr -y \
    && apt-get install tesseract-ocr-fra -y \
    && apt-get install tesseract-ocr-eng -y
ENV TZ=Europe/Paris
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
ADD target/dossierfacile-process-file.jar dossierfacile-process-file.jar
ENTRYPOINT ["java","-jar","-Dlog4j2.formatMsgNoLookups=true","/dossierfacile-process-file.jar"]
EXPOSE 5000
