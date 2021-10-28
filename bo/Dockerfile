FROM amazoncorretto:15
ENV TZ=Europe/Paris
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
ADD target/dossier-facile-bo.jar dossier-facile-bo.jar
ENTRYPOINT ["java","-Xmx7168m", "-Xms1024m","-Djava.security.egd=file:/dev/./urandom","-jar","/dossier-facile-bo.jar"]
EXPOSE 8080