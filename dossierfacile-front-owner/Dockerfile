FROM amazoncorretto:15
ENV TZ=Europe/Paris
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
ADD target/dossier-facile-front.jar dossier-facile-front.jar
ENTRYPOINT ["java","-Dlog4j2.formatMsgNoLookups=true","-Djava.security.egd=file:/dev/./urandom","-jar","/dossier-facile-front.jar"]
EXPOSE 8080
