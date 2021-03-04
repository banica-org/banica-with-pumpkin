FROM java:openjdk-8-jre-alpine

RUN mkdir App
WORKDIR App
COPY ./target/aurora-1.0-SNAPSHOT.jar .

CMD java -jar aurora-1.0-SNAPSHOT.jar