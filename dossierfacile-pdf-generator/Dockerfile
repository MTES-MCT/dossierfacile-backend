FROM amazoncorretto:15
ENV TZ=Europe/Paris
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
ADD target/dossierfacile-pdf-generator.jar dossierfacile-pdf-generator.jar
ENTRYPOINT ["java","-jar","-Dlog4j2.formatMsgNoLookups=true","/dossierfacile-pdf-generator.jar"]
EXPOSE 8084
