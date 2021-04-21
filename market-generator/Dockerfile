FROM java:openjdk-8-jre-alpine

ENV MARKET="europe"
ENV JMX_PORT=7001
ENV SPRING_PORT=8001
ENV GRPC_PORT=9001
ENV VM_IP="0.0.0.0"

RUN mkdir App
WORKDIR App
COPY ./target/market-generator-1.0-SNAPSHOT.jar .

EXPOSE $JMX_PORT
EXPOSE $SPRING_PORT
EXPOSE $GRPC_PORT

CMD java \
-Dcom.sun.management.jmxremote.ssl=false \
-Dcom.sun.management.jmxremote.authenticate=false \
-Dcom.sun.management.jmxremote.port=${JMX_PORT} \
-Dcom.sun.management.jmxremote.rmi.port=${JMX_PORT} \
-Djava.rmi.server.hostname=${VM_IP} \
-jar market-generator-1.0-SNAPSHOT.jar --market=${MARKET} --port=${SPRING_PORT} --grpc=${GRPC_PORT}