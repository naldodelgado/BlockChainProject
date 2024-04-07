FROM openjdk:17
MAINTAINER baeldung.com
COPY /target/ds-chat.jar peer.jar
RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install tcpdump -y && \
    apt-get install iputils-ping -y


ENTRYPOINT ["java", "-jar", "./peer.jar"]