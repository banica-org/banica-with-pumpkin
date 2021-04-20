FROM java:openjdk-8-jre-alpine

ENV VM_IP="0.0.0.0"

RUN mkdir App
WORKDIR App
COPY ./target/aurora-1.0-SNAPSHOT.jar .

EXPOSE 7101
EXPOSE 8101
EXPOSE 9101

CMD java -jar aurora-1.0-SNAPSHOT.jar

CMD java \
-Dcom.sun.management.jmxremote.ssl=false \
-Dcom.sun.management.jmxremote.authenticate=false \
-Dcom.sun.management.jmxremote.port=7101 \
-Dcom.sun.management.jmxremote.rmi.port=7101 \
-Djava.rmi.server.hostname=${VM_IP} \
-jar aurora-1.0-SNAPSHOT.jar