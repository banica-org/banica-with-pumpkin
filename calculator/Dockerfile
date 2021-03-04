FROM java:openjdk-8-jre-alpine


RUN mkdir App
WORKDIR App
COPY ./target/calculator-1.0-SNAPSHOT.jar .

CMD java -jar calculator-1.0-SNAPSHOT.jar