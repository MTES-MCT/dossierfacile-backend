FROM amazoncorretto:20
ENV TZ=Europe/Paris
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
ADD target/dossierfacile-bo.jar dossier-facile-bo.jar
ENTRYPOINT ["java","-Xmx7168m", "-Xms1024m","-Dlog4j2.formatMsgNoLookups=true","-Djava.security.egd=file:/dev/./urandom","-jar","/dossier-facile-bo.jar"]
EXPOSE 8080
