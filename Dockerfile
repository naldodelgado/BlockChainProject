FROM amazoncorretto:20-alpine
MAINTAINER baeldung.com
COPY /target /

ENTRYPOINT ["java", "-jar", "./BlockChainProject-1.0-SNAPSHOT.jar"]