FROM amd64/amazoncorretto:15
ENV TZ=Europe/Paris
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
COPY fonts/* usr/share/fonts/
ADD target/dossierfacile-api-tenant-0.0.1-SNAPSHOT.jar dossierfacile-api-tenant-0.0.1-SNAPSHOT.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/dossierfacile-api-tenant-0.0.1-SNAPSHOT.jar"]
EXPOSE 8080
