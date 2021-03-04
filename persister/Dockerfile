FROM java:openjdk-8-jre-alpine

RUN mkdir App
WORKDIR App
COPY ./target/persister-1.0-SNAPSHOT.jar .

CMD java -jar persister-1.0-SNAPSHOT.jar