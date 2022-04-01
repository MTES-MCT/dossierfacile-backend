FROM amazoncorretto:15
ENV TZ=Europe/Paris
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
ADD target/dossier-facile-pdf-generator.jar dossier-facile-pdf-generator.jar
ENTRYPOINT ["java","-jar","-Dlog4j2.formatMsgNoLookups=true","/dossier-facile-pdf-generator.jar"]
EXPOSE 8084
