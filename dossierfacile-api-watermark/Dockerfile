FROM amazoncorretto:15
ENV TZ=Europe/Paris
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
ADD target/dossierfacile-api-watermark.jar dossierfacile-api-watermark.jar
ENTRYPOINT ["java","-jar","-Dlog4j2.formatMsgNoLookups=true","/dossierfacile-api-watermark.jar"]
EXPOSE 3410
