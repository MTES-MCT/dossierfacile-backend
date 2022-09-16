FROM amd64/amazoncorretto:15
ENV TZ=Europe/Paris
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
COPY fonts/* usr/share/fonts/
ADD target/dossierfacile-api-tenant.jar dossierfacile-api-tenant.jar
ENTRYPOINT ["java","-Xmx7168m", "-Xms1024m","-Dlog4j2.formatMsgNoLookups=true","-Djava.security.egd=file:/dev/./urandom","-jar","/dossierfacile-api-tenant.jar"]
EXPOSE 8080
