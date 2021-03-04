FROM java:openjdk-8-jre-alpine

RUN mkdir App
WORKDIR App
COPY ./target/order-book-1.0-SNAPSHOT.jar .

CMD java -jar order-book-1.0-SNAPSHOT.jar