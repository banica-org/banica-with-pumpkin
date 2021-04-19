FROM java:openjdk-8-jre-alpine

ENV VM_IP="0.0.0.0"
ENV AURORA_IP="localhost"

RUN mkdir App
WORKDIR App
COPY ./target/calculator-1.0-SNAPSHOT.jar .

EXPOSE 7301
EXPOSE 8301

CMD java \
-Dcom.sun.management.jmxremote.ssl=false \
-Dcom.sun.management.jmxremote.authenticate=false \
-Dcom.sun.management.jmxremote.port=7301 \
-Dcom.sun.management.jmxremote.rmi.port=7301 \
-Djava.rmi.server.hostname=${VM_IP} \
-jar calculator-1.0-SNAPSHOT.jar --hostAurora=${AURORA_IP}