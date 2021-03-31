FROM java:openjdk-8-jre-alpine


RUN mkdir App
WORKDIR App
COPY ./target/orderBook-1.0-SNAPSHOT.jar .

CMD java -jar orderBook-1.0-SNAPSHOT.jar