FROM java:openjdk-8-jre-alpine

ENV VM_IP="0.0.0.0"
ENV AURORA_IP="localhost"

RUN mkdir App
WORKDIR App
COPY ./target/order-book-1.0-SNAPSHOT.jar .

EXPOSE 7201
EXPOSE 8201
EXPOSE 9201

CMD java \
-Dcom.sun.management.jmxremote.ssl=false \
-Dcom.sun.management.jmxremote.authenticate=false \
-Dcom.sun.management.jmxremote.port=7201 \
-Dcom.sun.management.jmxremote.rmi.port=7201 \
-Djava.rmi.server.hostname=${VM_IP} \
-jar order-book-1.0-SNAPSHOT.jar --hostAurora=${AURORA_IP}