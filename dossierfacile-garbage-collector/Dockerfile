FROM amazoncorretto:15
ENV TZ=Europe/Paris
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
ADD target/checker-0.0.1-SNAPSHOT.jar checker-0.0.1-SNAPSHOT.jar
ENTRYPOINT ["java","-jar","-Dlog4j2.formatMsgNoLookups=true","/checker-0.0.1-SNAPSHOT.jar"]
EXPOSE 8084
