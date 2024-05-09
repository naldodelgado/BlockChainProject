FROM amazoncorretto:20-alpine
MAINTAINER baeldung.com
COPY /target /

RUN apk update && apk add --no-cache openssl

ENV PASS_PHRASE=MySecurePassPhrase

RUN openssl genrsa -des3 -passout env:PASS_PHRASE -out server.key 2048
RUN openssl req -new -key server.key -passin env:PASS_PHRASE -out server.csr -subj "/C=US/ST=State/L=City/O=Organization/OU=Department/CN=example.com"
RUN openssl rsa -in server.key -passin env:PASS_PHRASE -out server.pem
RUN openssl x509 -req -days 365 -in server.csr -signkey server.pem -out server.crt -passin env:PASS_PHRASE
#RUN mv server.pem /etc/ssl/private && mv cacert.pem /etc/ssl/certs

ENTRYPOINT ["java", "-jar", "./BlockChainProject-1.0-SNAPSHOT.jar"]