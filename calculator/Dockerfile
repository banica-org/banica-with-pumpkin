FROM java:openjdk-8-jre-alpine

RUN mkdir App

WORKDIR App

COPY ./target/calculator-1.0-SNAPSHOT.jar .

EXPOSE 1099

CMD java  -Dcom.sun.management.jmxremote \
-Dcom.sun.management.jmxremote.port=1099 \
-Dcom.sun.management.jmxremote.rmi.port=1099 \
-Dcom.sun.management.jmxremote.ssl=false \
-Dcom.sun.management.jmxremote.authenticate=false \
-Dcom.sun.management.jmxremote.local.only=false \
-Djava.rmi.server.hostname=localhost \
-jar calculator-1.0-SNAPSHOT.jar
