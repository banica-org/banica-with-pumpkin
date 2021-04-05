FROM java:openjdk-8-jre-alpine


RUN mkdir App
WORKDIR App
COPY ./target/common-1.0-SNAPSHOT.jar .

CMD java -jar common-1.0-SNAPSHOT.jar