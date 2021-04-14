FROM java:openjdk-8-jre-alpine

RUN mkdir App
WORKDIR App
COPY ./target/market-generator-1.0-SNAPSHOT.jar .

CMD java -jar market-generator-1.0-SNAPSHOT.jar