FROM amazoncorretto:15
ENV TZ=Europe/Paris
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
ADD target/dossier-facile-process-file.jar dossier-facile-process-file.jar
ENTRYPOINT ["java","-jar","-Dlog4j2.formatMsgNoLookups=true","/dossier-facile-process-file.jar"]
EXPOSE 5000
