FROM java:openjdk-8-jre-alpine


RUN mkdir App
WORKDIR App
COPY ./target/protobuf-messages-1.0-SNAPSHOT.jar .

CMD java -jar protobuf-messages-1.0-SNAPSHOT.jar